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
import android.os.Handler;
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
import com.nodia.respira.datos.Usuario;
import com.nodia.respira.ui.MainActivity;
import com.nodia.respira.ui.Notificaciones;

import java.util.HashMap;
import java.util.Map;

// # Service <--- ServicioEscuharBeacons; recibe, filtra y geolocaliza anuncios del sensor activo.
// # Ausente representa la ausencia lógica (null en el código).
// # ValorJson = Ausente | B | R | Text | [ValorJson] | Map[Text, ValorJson].
// # ObjetoJson = Map[Text, ValorJson].
// # EntornoAndroid = { aplicacion: Text }; identifica los recursos y servicios de la app.
// # Ubicacion = { latitud: R, longitud: R, precision: R, momento: DateTime }.
// # SolicitudServicio = { accion: Text, extras: Map[Text, ValorJson] }.
// # AnuncioBle = { bytes: [Z], rssi: Z }.
// # EscanerBle = { activo: B }; recurso de escucha Bluetooth.

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

    private static final long ESPERA_SENSOR_MS = 15_000L;
    private static boolean ejecutando;

    private static final String ETIQUETA_LOG = ">>>>";
    private static final int ID_NOTIFICACION = 7;

    private BluetoothLeScanner elEscaner;
    private ScanCallback elCallbackDelEscaner;
    private String modoActual = null;
    private String uuidBuscado = null;
    private String codigoSensor = null;

    private final Handler principal = new Handler(Looper.getMainLooper());
    private String propietario;
    private String modoSolicitado;
    private long inicioEscucha, ultimaRecepcion;

    // La placa repite el mismo anuncio unas 10 veces por segundo durante 2 s.
    // Por cada tipo de medida solo se envía cuando cambia el contador.
    private final Map<Integer, Integer> ultimoContadorPorTipo = new HashMap<>();
    private final Runnable vigilar = new Runnable() {
        // --------------------------------------------------------------
        // # effects: coordina el servicio y su notificación en Android.
        // +
        //     run() -->
        // --------------------------------------------------------------
        @Override public void run() {
            actualizarEstado();
            if (ejecutando) principal.postDelayed(this, 3_000L);
        }
    };

    private LocationManager gestorUbicacion;
    private Location ultimaUbicacion;
    private final LocationListener oyenteUbicacion = new LocationListener() {
        // --------------------------------------------------------------
        // # Acepta la posición si mejora la precisión o la anterior ya tiene más de diez segundos.
        // # effects: recibe o consulta la ubicación del dispositivo.
        // + l: Ubicacion
        //       -->
        //     onLocationChanged() -->
        // --------------------------------------------------------------
        @Override
        public void onLocationChanged(@NonNull Location l) {
            if (ultimaUbicacion == null || l.getAccuracy() <= ultimaUbicacion.getAccuracy()
                    || l.getTime() - ultimaUbicacion.getTime() > 10_000) {
                ultimaUbicacion = l;
            }
        }

        // En Android 9 y 10 el sistema llama también a estos (sin ellos, se cierra la app)
        // --------------------------------------------------------------
        // + proveedor: Text, estado: Z, extras: ObjetoJson
        //       -->
        //     onStatusChanged() -->
        // --------------------------------------------------------------
        @Override
        public void onStatusChanged(String proveedor, int estado, android.os.Bundle extras) { }

        // --------------------------------------------------------------
        // + proveedor: Text
        //       -->
        //     onProviderEnabled() -->
        // --------------------------------------------------------------
        @Override
        public void onProviderEnabled(@NonNull String proveedor) {
            actualizarEstado();
        }

        // --------------------------------------------------------------
        // + proveedor: Text
        //       -->
        //     onProviderDisabled() -->
        // --------------------------------------------------------------
        @Override
        public void onProviderDisabled(@NonNull String proveedor) {
            actualizarEstado();
        }
    };

    // --------------------------------------------------------------
    // # effects: coordina el servicio y su notificación en Android.
    // + contexto: EntornoAndroid
    //       -->
    //     arrancar() --x
    // --------------------------------------------------------------
    public static void arrancar(Context contexto) {
        Intent i = new Intent(contexto, ServicioEscuharBeacons.class);
        i.putExtra(EXTRA_MODO, MODO_NUESTRO);
        ContextCompat.startForegroundService(contexto, i);
    }

    // --------------------------------------------------------------
    // # effects: coordina el servicio y su notificación en Android.
    // + contexto: EntornoAndroid
    //       -->
    //     detener() --x
    // --------------------------------------------------------------
    public static void detener(Context contexto) {
        contexto.stopService(new Intent(contexto, ServicioEscuharBeacons.class));
    }

    // --------------------------------------------------------------
    // +
    //     ejecutando() --x
    //       <--
    //   B
    // --------------------------------------------------------------
    public static boolean ejecutando() {
        return ejecutando;
    }

    // --------------------------------------------------------------
    // # effects: coordina el servicio y su notificación en Android; consulta el reloj.
    // + intent: SolicitudServicio, flags: Z, start_id: Z
    //       -->
    //     onStartCommand() -->
    //       <--
    //   Z
    // --------------------------------------------------------------
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String modo = (intent != null && intent.getStringExtra(EXTRA_MODO) != null)
                ? intent.getStringExtra(EXTRA_MODO) : MODO_TODOS;
        Log.d(ETIQUETA_LOG, "ServicioEscuharBeacons.onStartCommand(): modo = " + modo);

        Repositorio repo = Repositorio.obtener(this);
        Recorrido r = repo.recorrido();
        Usuario usuario = repo.usuario();
        propietario = usuario == null ? null : usuario.id;
        codigoSensor = r != null ? r.codigo : null;
        uuidBuscado = codigoSensor != null ? Medicion.uuidDelSensor(codigoSensor) : null;
        modoSolicitado = modo;

        ponerEnPrimerPlano();
        detenerEscaneo();
        inicioEscucha = System.currentTimeMillis();
        ultimaRecepcion = 0;
        ultimoContadorPorTipo.clear();
        ejecutando = true;
        principal.removeCallbacks(vigilar);
        vigilar.run();
        return START_NOT_STICKY;
    }

    // --------------------------------------------------------------
    // # effects: coordina el servicio y su notificación en Android.
    // -
    //     ponerEnPrimerPlano() -->
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
    // # effects: recibe o consulta la ubicación del dispositivo.
    // -
    //     arrancarUbicacion() -->
    // --------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private void arrancarUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(ETIQUETA_LOG, "Sin permiso de ubicación: los datos irán sin zona");
            return;
        }
        gestorUbicacion = getSystemService(LocationManager.class);
        if (gestorUbicacion == null || !gestorUbicacion.isLocationEnabled()) return;
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

    // --------------------------------------------------------------
    // # effects: consulta o recibe datos del sistema Bluetooth.
    // + contexto: EntornoAndroid
    //       -->
    //     bluetoothActivado() --x
    //       <--
    //   B
    // --------------------------------------------------------------
    @SuppressLint("MissingPermission")
    public static boolean bluetoothActivado(Context contexto) {
        if (!tienePermisoEscanear(contexto) || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && ContextCompat.checkSelfPermission(contexto, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED)) return false;
        BluetoothManager gestor = contexto.getSystemService(BluetoothManager.class);
        BluetoothAdapter adaptador = gestor == null ? null : gestor.getAdapter();
        return adaptador != null && adaptador.isEnabled();
    }

    // --------------------------------------------------------------
    // # effects: recibe o consulta la ubicación del dispositivo.
    // + contexto: EntornoAndroid
    //       -->
    //     ubicacionActivada() --x
    //       <--
    //   B
    // --------------------------------------------------------------
    public static boolean ubicacionActivada(Context contexto) {
        LocationManager gestor = contexto.getSystemService(LocationManager.class);
        return ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && gestor != null && gestor.isLocationEnabled();
    }

    private boolean preparandoUbicacion;

    // --------------------------------------------------------------
    // # effects: coordina el servicio y su notificación en Android; consulta el reloj.
    // -
    //     actualizarEstado() -->
    // --------------------------------------------------------------
    private void actualizarEstado() {
        Repositorio repo = Repositorio.obtener(this);
        Usuario usuario = repo.usuario();
        if (MODO_NUESTRO.equals(modoSolicitado) && (usuario == null || !usuario.id.equals(propietario))) {
            stopSelf();
            return;
        }
        if (!bluetoothActivado(this)) {
            detenerEscaneo();
            repo.estadoSensor(Recorrido.BLUETOOTH);
            return;
        }
        if (!ubicacionActivada(this)) {
            prepararUbicacionDeNuevo();
            repo.estadoSensor(Recorrido.UBICACION);
            return;
        }
        if (!preparandoUbicacion) {
            arrancarUbicacion();
            preparandoUbicacion = true;
        }
        if (elEscaner == null) {
            inicioEscucha = System.currentTimeMillis();
            arrancarEscaneo(modoSolicitado);
        }
        long ahora = System.currentTimeMillis();
        if (ultimaRecepcion == 0) {
            repo.estadoSensor(ahora - inicioEscucha >= ESPERA_SENSOR_MS ? Recorrido.SIN_ALCANCE : Recorrido.BUSCANDO);
        } else if (ahora - ultimaRecepcion >= ESPERA_SENSOR_MS) {
            repo.estadoSensor(Recorrido.SIN_ALCANCE);
        } else {
            repo.estadoSensor(ubicacionReciente(ahora) ? Recorrido.ACTIVO : Recorrido.LOCALIZANDO);
        }
    }

    // --------------------------------------------------------------
    // # effects: recibe o consulta la ubicación del dispositivo.
    // -
    //     prepararUbicacionDeNuevo() -->
    // --------------------------------------------------------------
    private void prepararUbicacionDeNuevo() {
        detenerEscaneo();
        detenerUbicacion();
        preparandoUbicacion = false;
        ultimaRecepcion = 0;
        ultimaUbicacion = null;
    }

    // --------------------------------------------------------------
    // # effects: recibe o consulta la ubicación del dispositivo.
    // -
    //     detenerUbicacion() -->
    // --------------------------------------------------------------
    private void detenerUbicacion() {
        if (gestorUbicacion == null) return;
        try {
            gestorUbicacion.removeUpdates(oyenteUbicacion);
        } catch (SecurityException ignored) {
            Log.d(ETIQUETA_LOG, "Permiso de ubicación retirado");
        }
    }

    // --------------------------------------------------------------
    // - ahora: Z
    //       -->
    //     ubicacionReciente() <--
    //       <--
    //   B
    // --------------------------------------------------------------
    private boolean ubicacionReciente(long ahora) {
        return ultimaUbicacion != null && ahora - ultimaUbicacion.getTime() < 120_000L;
    }

    // --------------------------------------------------------------
    // # effects: consulta o recibe datos del sistema Bluetooth.
    // -
    //     obtenerEscaner() <--
    //       <--
    //   EscanerBle | Ausente
    // --------------------------------------------------------------
    @SuppressLint("MissingPermission")
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
    // # effects: consulta o recibe datos del sistema Bluetooth.
    // + c: EntornoAndroid
    //       -->
    //     tienePermisoEscanear() --x
    //       <--
    //   B
    // --------------------------------------------------------------
    public static boolean tienePermisoEscanear(Context c) {
        String permiso = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? Manifest.permission.BLUETOOTH_SCAN
                : Manifest.permission.ACCESS_FINE_LOCATION;
        return ContextCompat.checkSelfPermission(c, permiso) == PackageManager.PERMISSION_GRANTED;
    }

    // --------------------------------------------------------------
    // # effects: consulta o recibe datos del sistema Bluetooth.
    // - modo: Text
    //       -->
    //     arrancarEscaneo() -->
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

        elCallbackDelEscaner = new ScanCallback() {
            // --------------------------------------------------------------
            // # effects: consulta o recibe datos del sistema Bluetooth.
            // + callback_type: Z, resultado: AnuncioBle
            //       -->
            //     onScanResult() -->
            // --------------------------------------------------------------
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                if (MODO_NUESTRO.equals(modoActual)) {
                    procesarSiEsNuestroBeacon(resultado);
                } else {
                    mostrarDispositivo(resultado);
                }
            }

            // --------------------------------------------------------------
            // # effects: consulta o recibe datos del sistema Bluetooth.
            // + codigo_error: Z
            //       -->
            //     onScanFailed() -->
            // --------------------------------------------------------------
            @Override
            public void onScanFailed(int codigoError) {
                Log.e(ETIQUETA_LOG, "Error al escanear BLE. Código: " + codigoError);
                detenerEscaneo();
            }
        };

        ScanSettings ajustes = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        try {
            elEscaner.startScan(null, ajustes, elCallbackDelEscaner);
        } catch (IllegalStateException | SecurityException e) {
            detenerEscaneo();
            return;
        }
        Log.d(ETIQUETA_LOG, "Escaneo BLE arrancado en modo " + modo + (uuidBuscado == null ? "" : ", buscando " + uuidBuscado));
    }

    // --------------------------------------------------------------
    // # effects: consulta o recibe datos del sistema Bluetooth.
    // - resultado: AnuncioBle
    //       -->
    //     mostrarDispositivo() -->
    // --------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private void mostrarDispositivo(ScanResult resultado) {
        String nombre = resultado.getScanRecord() != null ? resultado.getScanRecord().getDeviceName() : null;
        Log.d(ETIQUETA_LOG, "Dispositivo: " + resultado.getDevice().getAddress()
                + " | nombre: " + nombre + " | rssi: " + resultado.getRssi());
    }

    // --------------------------------------------------------------
    // # Filtra el UUID del recorrido y descarta repeticiones del contador por tipo de magnitud.
    // # effects: consulta o recibe datos del sistema Bluetooth; guarda las mediciones nuevas con su ubicación; consulta el reloj.
    // - resultado: AnuncioBle
    //       -->
    //     procesarSiEsNuestroBeacon() -->
    // --------------------------------------------------------------
    private void procesarSiEsNuestroBeacon(ScanResult resultado) {
        if (resultado.getScanRecord() == null || uuidBuscado == null) {
            return;
        }
        TramaIBeacon trama = new TramaIBeacon(resultado.getScanRecord().getBytes());
        if (!trama.esIBeacon() || !uuidBuscado.equals(trama.getUUIDComoTexto())) {
            return;
        }
        long ahora = System.currentTimeMillis();
        ultimaRecepcion = ahora;
        if (!ubicacionActivada(this) || !ubicacionReciente(ahora)) return;
        Integer anterior = ultimoContadorPorTipo.get(trama.getTipoMedicion());
        if (anterior != null && anterior == trama.getContador()) return;
        Medicion medicion = Medicion.desdeTrama(trama, codigoSensor, ahora);
        if (medicion == null) return;
        Repositorio repo = Repositorio.obtener(this);
        Usuario usuario = repo.usuario();
        if (usuario == null || !usuario.id.equals(propietario)) return;
        Location l = ultimaUbicacion;
        medicion.conUbicacion(l.getLatitude(), l.getLongitude(), Math.round(l.getAccuracy()));
        medicion.conRssi(resultado.getRssi());
        try {
            EnvioMediciones.obtener(this).guardar(propietario, medicion);
        } catch (android.database.sqlite.SQLiteException e) {
            Log.e(ETIQUETA_LOG, "No se pudo guardar la lectura localmente", e);
            repo.estadoSensor(Recorrido.PAUSA);
            return;
        }
        ultimoContadorPorTipo.put(trama.getTipoMedicion(), trama.getContador());
        repo.anotarUltimoValor(medicion.getMagnitud(), medicion.getValor());
        repo.estadoSensor(Recorrido.ACTIVO);
    }

    // --------------------------------------------------------------
    // # effects: consulta o recibe datos del sistema Bluetooth.
    // -
    //     detenerEscaneo() -->
    // --------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private void detenerEscaneo() {
        if (elEscaner != null && elCallbackDelEscaner != null && tienePermisoEscanear(this)) {
            try {
                elEscaner.stopScan(elCallbackDelEscaner);
                Log.d(ETIQUETA_LOG, "Escaneo BLE detenido");
            } catch (IllegalStateException | SecurityException e) {
                Log.d(ETIQUETA_LOG, "Escáner apagado");
            }
        }
        elEscaner = null;
        elCallbackDelEscaner = null;
        modoActual = null;
    }

    // --------------------------------------------------------------
    // # effects: coordina el servicio y su notificación en Android.
    // +
    //     onDestroy() -->
    // --------------------------------------------------------------
    @Override
    public void onDestroy() {
        ejecutando = false;
        principal.removeCallbacks(vigilar);
        detenerEscaneo();
        Repositorio.obtener(this).estadoSensor(Recorrido.PAUSA);
        detenerUbicacion();
        super.onDestroy();
    }

    // --------------------------------------------------------------
    // + intent: SolicitudServicio
    //       -->
    //     onBind() --x
    //       <--
    //   Ausente
    // --------------------------------------------------------------
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
