package com.nodia.respira.ble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.nodia.respira.R;
import com.nodia.respira.datos.Recorrido;
import com.nodia.respira.datos.Repositorio;
import com.nodia.respira.ui.MainActivity;
import com.nodia.respira.ui.Notificaciones;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ==============================================================
// ServicioEscuharBeacons: escanea BLE en primer plano (con
// notificación fija, como exige Android) mientras dura el recorrido.
//
// Solo atiende al iBeacon del sensor del recorrido:
//   UUID = "RESPIRA-" + código del sensor (16 caracteres)
// Cada medida NUEVA se envía a la API con la ubicación del móvil
// (POST /mediciones). Sin conexión se guardan y se reenvían luego.
//
// Modos (extra MODO del Intent):
//   MODO_TODOS   -> muestra en el Log todos los dispositivos BLE
//   MODO_NUESTRO -> el sensor del recorrido
// ==============================================================
public class ServicioEscuharBeacons extends Service {

    public static final String EXTRA_MODO = "MODO";
    public static final String MODO_TODOS = "TODOS";
    public static final String MODO_NUESTRO = "NUESTRO";

    private static final int MAX_PENDIENTES = 500;

    private static final String ETIQUETA_LOG = ">>>>";
    private static final int ID_NOTIFICACION = 7;

    private BluetoothLeScanner elEscaner;
    private ScanCallback elCallbackDelEscaner;
    private String modoActual = null;
    private String uuidBuscado = null;
    private String codigoSensor = null;

    private final LogicaFake laLogica = new LogicaFake(LogicaFake.URL_SERVIDOR);

    // La placa repite el mismo anuncio unas 10 veces por segundo durante 2 s.
    // Por cada tipo de medida solo se envía cuando cambia el contador.
    private final Map<Integer, Integer> ultimoContadorPorTipo = new HashMap<>();
    private final List<Medicion> pendientes = new ArrayList<>();
    private boolean enviandoPendientes = false;

    private LocationManager gestorUbicacion;
    private Location ultimaUbicacion;
    private final LocationListener oyenteUbicacion = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location l) {
            if (ultimaUbicacion == null || l.getAccuracy() <= ultimaUbicacion.getAccuracy()
                    || l.getTime() - ultimaUbicacion.getTime() > 10_000) {
                ultimaUbicacion = l;
            }
        }

        // En Android 9 y 10 el sistema llama también a estos (sin ellos, se cierra la app)
        @Override
        public void onStatusChanged(String proveedor, int estado, android.os.Bundle extras) { }

        @Override
        public void onProviderEnabled(@NonNull String proveedor) { }

        @Override
        public void onProviderDisabled(@NonNull String proveedor) { }
    };

    // --------------------------------------------------------------
    // contexto: Context --> arrancar()   (desde la pantalla Sensor)
    // --------------------------------------------------------------
    public static void arrancar(Context contexto) {
        Intent i = new Intent(contexto, ServicioEscuharBeacons.class);
        i.putExtra(EXTRA_MODO, MODO_NUESTRO);
        ContextCompat.startForegroundService(contexto, i);
    }

    public static void detener(Context contexto) {
        contexto.stopService(new Intent(contexto, ServicioEscuharBeacons.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String modo = (intent != null && intent.getStringExtra(EXTRA_MODO) != null)
                ? intent.getStringExtra(EXTRA_MODO) : MODO_TODOS;
        Log.d(ETIQUETA_LOG, "ServicioEscuharBeacons.onStartCommand(): modo = " + modo);

        Recorrido r = Repositorio.obtener(this).recorrido();
        codigoSensor = r != null ? r.codigo : null;
        uuidBuscado = codigoSensor != null ? Medicion.uuidDelSensor(codigoSensor) : null;

        ponerEnPrimerPlano();
        detenerEscaneo(); // si ya escaneaba en otro modo, se reinicia
        arrancarUbicacion();
        arrancarEscaneo(modo);
        return START_NOT_STICKY;
    }

    // --------------------------------------------------------------
    // ponerEnPrimerPlano(): notificación fija "Recorrido activo"
    // --------------------------------------------------------------
    private void ponerEnPrimerPlano() {
        Notificaciones.crearCanales(this);
        PendingIntent abrir = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class).putExtra(MainActivity.EXTRA_PESTANA, R.id.tab_sensor),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new NotificationCompat.Builder(this, Notificaciones.CANAL_RECORRIDO)
                .setSmallIcon(R.drawable.ic_sensor)
                .setContentTitle("Recorrido activo")
                .setContentText("Respira está escuchando a tu sensor" + (codigoSensor == null ? "" : " " + codigoSensor))
                .setOngoing(true)
                .setContentIntent(abrir)
                .build();
        // connectedDevice (BLE) + location (para situar cada dato con la pantalla apagada).
        // El tipo location solo se puede pedir si ya hay permiso de ubicación.
        int tipo = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tipo = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE;
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                tipo |= ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
            }
        }
        ServiceCompat.startForeground(this, ID_NOTIFICACION, n, tipo);
    }

    // --------------------------------------------------------------
    // arrancarUbicacion(): GPS y red cada 5 s (para situar cada dato)
    // --------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private void arrancarUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(ETIQUETA_LOG, "Sin permiso de ubicación: los datos irán sin zona");
            return;
        }
        gestorUbicacion = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (gestorUbicacion == null) {
            return;
        }
        for (String proveedor : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
            try {
                if (gestorUbicacion.isProviderEnabled(proveedor)) {
                    gestorUbicacion.requestLocationUpdates(proveedor, 5000, 0, oyenteUbicacion, Looper.getMainLooper());
                    Location l = gestorUbicacion.getLastKnownLocation(proveedor);
                    if (l != null) {
                        oyenteUbicacion.onLocationChanged(l);
                    }
                }
            } catch (Exception e) {
                Log.e(ETIQUETA_LOG, "No se pudo pedir la ubicación a " + proveedor + ": " + e);
            }
        }
    }

    private BluetoothLeScanner obtenerEscaner() {
        BluetoothManager gestor = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adaptador = gestor == null ? null : gestor.getAdapter();
        if (adaptador == null) {
            Log.e(ETIQUETA_LOG, "Este dispositivo no tiene Bluetooth (¿emulador?)");
            return null;
        }
        if (!adaptador.isEnabled()) {
            Log.e(ETIQUETA_LOG, "El Bluetooth está apagado");
            return null;
        }
        return adaptador.getBluetoothLeScanner();
    }

    // --------------------------------------------------------------
    // tienePermisoEscanear() --> B
    // --------------------------------------------------------------
    public static boolean tienePermisoEscanear(Context c) {
        String permiso = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? Manifest.permission.BLUETOOTH_SCAN
                : Manifest.permission.ACCESS_FINE_LOCATION;
        return ContextCompat.checkSelfPermission(c, permiso) == PackageManager.PERMISSION_GRANTED;
    }

    // --------------------------------------------------------------
    // modo: Text --> arrancarEscaneo()
    // --------------------------------------------------------------
    @SuppressLint("MissingPermission") // se comprueba en tienePermisoEscanear()
    private void arrancarEscaneo(String modo) {
        if (!tienePermisoEscanear(this)) {
            Log.e(ETIQUETA_LOG, "Faltan permisos para escanear BLE");
            return;
        }
        elEscaner = obtenerEscaner();
        if (elEscaner == null) {
            Log.e(ETIQUETA_LOG, "No se pudo obtener el escáner BLE");
            return;
        }

        modoActual = modo;
        ultimoContadorPorTipo.clear();

        elCallbackDelEscaner = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                if (MODO_NUESTRO.equals(modoActual)) {
                    procesarSiEsNuestroBeacon(resultado);
                } else {
                    mostrarDispositivo(resultado);
                }
            }

            @Override
            public void onScanFailed(int codigoError) {
                Log.e(ETIQUETA_LOG, "Error al escanear BLE. Código: " + codigoError);
            }
        };

        ScanSettings ajustes = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        elEscaner.startScan(null, ajustes, elCallbackDelEscaner);
        Log.d(ETIQUETA_LOG, "Escaneo BLE arrancado en modo " + modo + (uuidBuscado == null ? "" : ", buscando " + uuidBuscado));
    }

    @SuppressLint("MissingPermission")
    private void mostrarDispositivo(ScanResult resultado) {
        String nombre = resultado.getScanRecord() != null ? resultado.getScanRecord().getDeviceName() : null;
        Log.d(ETIQUETA_LOG, "Dispositivo: " + resultado.getDevice().getAddress()
                + " | nombre: " + nombre + " | rssi: " + resultado.getRssi());
    }

    // --------------------------------------------------------------
    // resultado: ScanResult --> procesarSiEsNuestroBeacon()
    // --------------------------------------------------------------
    private void procesarSiEsNuestroBeacon(ScanResult resultado) {
        if (resultado.getScanRecord() == null || uuidBuscado == null) {
            return;
        }
        TramaIBeacon trama = new TramaIBeacon(resultado.getScanRecord().getBytes());
        if (!trama.esIBeacon() || !uuidBuscado.equals(trama.getUUIDComoTexto())) {
            return;
        }
        Integer anterior = ultimoContadorPorTipo.get(trama.getTipoMedicion());
        if (anterior != null && anterior == trama.getContador()) {
            return; // el mismo anuncio repetido: ya enviado
        }
        Medicion medicion = Medicion.desdeTrama(trama, codigoSensor, System.currentTimeMillis());
        if (medicion == null) {
            return; // tipo desconocido
        }
        ultimoContadorPorTipo.put(trama.getTipoMedicion(), trama.getContador());

        Location l = ultimaUbicacion;
        if (l != null && System.currentTimeMillis() - l.getTime() < 120_000) {
            medicion.conUbicacion(l.getLatitude(), l.getLongitude(), Math.round(l.getAccuracy()));
        }
        medicion.conRssi(resultado.getRssi());
        final Repositorio repo = Repositorio.obtener(this);
        // Con la sesión: el servidor comprueba que este sensor está prestado
        // a esta cuenta y le cuenta los minutos (puntos)
        laLogica.conToken(repo.token());
        Log.d(ETIQUETA_LOG, "Medida del sensor: " + medicion);

        final boolean enParque = l != null && repo.parqueEn(l.getLatitude(), l.getLongitude()) != null;
        repo.anotarUltimoValor(medicion.getMagnitud(), medicion.getValor());
        laLogica.guardarMedicion(medicion, (exito, codigo, detalle) -> {
            Log.d(ETIQUETA_LOG, (exito ? "POST OK " : "POST FALLIDO ") + codigo + ": " + detalle);
            if (exito) {
                enviarPendientes();
            } else if (codigo == -1 || codigo >= 500) {
                guardarPendiente(medicion); // sin red o servidor caído: se reintenta luego
            }
            repo.registrarMedicion(enParque, exito);
        });
    }

    private synchronized void guardarPendiente(Medicion m) {
        if (pendientes.size() >= MAX_PENDIENTES) {
            pendientes.remove(0);
        }
        pendientes.add(m);
    }

    // --------------------------------------------------------------
    // enviarPendientes(): al volver la conexión manda el lote guardado
    // --------------------------------------------------------------
    private synchronized void enviarPendientes() {
        if (pendientes.isEmpty() || enviandoPendientes) {
            return;
        }
        enviandoPendientes = true;
        final List<Medicion> lote = new ArrayList<>(pendientes);
        laLogica.guardarMediciones(lote, (exito, codigo, detalle) -> {
            synchronized (ServicioEscuharBeacons.this) {
                if (exito) {
                    pendientes.removeAll(lote);
                    Repositorio.obtener(ServicioEscuharBeacons.this).marcarPendientesEnviadas(lote.size());
                }
                enviandoPendientes = false;
            }
            Log.d(ETIQUETA_LOG, "Pendientes: " + codigo + " " + detalle);
        });
    }

    @SuppressLint("MissingPermission")
    private void detenerEscaneo() {
        if (elEscaner != null && elCallbackDelEscaner != null && tienePermisoEscanear(this)) {
            elEscaner.stopScan(elCallbackDelEscaner);
            Log.d(ETIQUETA_LOG, "Escaneo BLE detenido");
        }
        elCallbackDelEscaner = null;
        modoActual = null;
    }

    @Override
    public void onDestroy() {
        detenerEscaneo();
        if (gestorUbicacion != null) {
            gestorUbicacion.removeUpdates(oyenteUbicacion);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
