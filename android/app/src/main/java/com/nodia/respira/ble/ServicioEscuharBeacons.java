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
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.nodia.respira.R;
import com.nodia.respira.datos.Repositorio;
import com.nodia.respira.ui.MainActivity;
import com.nodia.respira.ui.Notificaciones;

// ==============================================================
// ServicioEscuharBeacons (Sprint 0, adaptado a Respira):
// escanea BLE en primer plano (con notificación fija, como exige
// Android) mientras dura el recorrido.
//
// Modos (extra MODO del Intent):
//   MODO_TODOS   -> muestra en el Log todos los dispositivos BLE
//   MODO_NUESTRO -> solo nuestro iBeacon; cada medida NUEVA se
//                   envía al servidor y se apunta en el Repositorio
// ==============================================================
public class ServicioEscuharBeacons extends Service {

    public static final String EXTRA_MODO = "MODO";
    public static final String MODO_TODOS = "TODOS";
    public static final String MODO_NUESTRO = "NUESTRO";

    // Debe coincidir EXACTAMENTE con beaconUUID de Publicador.h (Arduino)
    public static final String UUID_NUESTRO_BEACON = "AARON-GTI-PBIO-1";

    private static final String ETIQUETA_LOG = ">>>>";
    private static final int ID_NOTIFICACION = 7;

    private BluetoothLeScanner elEscaner;
    private ScanCallback elCallbackDelEscaner;
    private String modoActual = null;

    private final LogicaFake laLogica = new LogicaFake(LogicaFake.URL_SERVIDOR);

    // La placa repite el mismo anuncio muchas veces por segundo durante 3 s.
    // Solo enviamos al servidor cuando cambia el major (= cambia el contador).
    private int ultimoMajorEnviado = -1;

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

        ponerEnPrimerPlano();
        detenerEscaneo(); // si ya escaneaba en otro modo, se reinicia
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
                .setContentText("Respira está escuchando a tu sensor")
                .setOngoing(true)
                .setContentIntent(abrir)
                .build();
        int tipo = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE : 0;
        ServiceCompat.startForeground(this, ID_NOTIFICACION, n, tipo);
    }

    // --------------------------------------------------------------
    // obtenerEscaner() --> BluetoothLeScanner | null
    // --------------------------------------------------------------
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
        ultimoMajorEnviado = -1;

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
        Log.d(ETIQUETA_LOG, "Escaneo BLE arrancado en modo " + modo);
    }

    // --------------------------------------------------------------
    // resultado: ScanResult --> mostrarDispositivo()
    // --------------------------------------------------------------
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
        if (resultado.getScanRecord() == null) {
            return;
        }
        TramaIBeacon trama = new TramaIBeacon(resultado.getScanRecord().getBytes());
        if (!trama.esIBeacon() || !UUID_NUESTRO_BEACON.equals(trama.getUUIDComoTexto())) {
            return;
        }
        if (trama.getMajorEntero() == ultimoMajorEnviado) {
            return; // anuncio repetido: ya enviado
        }
        ultimoMajorEnviado = trama.getMajorEntero();

        Medicion medicion = Medicion.desdeTrama(trama);
        Log.d(ETIQUETA_LOG, "¡Nuestro beacon! " + medicion + " rssi=" + resultado.getRssi());

        final boolean enParque = estoyEnUnParque();
        final Repositorio repo = Repositorio.obtener(this);
        laLogica.guardarMedicion(medicion, (exito, detalle) -> {
            Log.d(ETIQUETA_LOG, (exito ? "POST OK: " : "POST FALLIDO: ") + detalle);
            repo.registrarMedicion(enParque, exito);
        });
    }

    // --------------------------------------------------------------
    // estoyEnUnParque() --> B   (última ubicación conocida del GPS)
    // TODO (Sprint 2): enviar lat/lng con cada medición al servidor.
    // --------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private boolean estoyEnUnParque() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            return false;
        }
        Location l = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (l == null) {
            l = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        return l != null && Repositorio.obtener(this).parqueEn(l.getLatitude(), l.getLongitude()) != null;
    }

    // --------------------------------------------------------------
    // detenerEscaneo()
    // --------------------------------------------------------------
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
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
