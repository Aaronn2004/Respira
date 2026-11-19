package com.nodia.respira.ble;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.nodia.respira.datos.ApiRespira;
import com.nodia.respira.datos.Repositorio;
import com.nodia.respira.datos.Usuario;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


// # SQLiteOpenHelper <--- EnvioMediciones; EnvioMediciones <>--- Lectura.
// # Ausente representa la ausencia lógica (null en el código).
// # ValorJson = Ausente | B | R | Text | [ValorJson] | Map[Text, ValorJson].
// # ObjetoJson = Map[Text, ValorJson].
// # ErrorApi = { codigo: Z, mensaje: Text, campos: Map[Text, Text] }.
// # EntornoAndroid = { aplicacion: Text }; identifica los recursos y servicios de la app.
// # TrabajoAndroid = { id: N }; trabajo programado por Android.
// # BaseLocal = Map[Text, [ObjetoJson]]; tablas de la cola persistente.
// # ErrorDatos = { detalle: Text }; fallo al leer o interpretar datos.
/** Cola local por cuenta. Un resultado parcial confirma solo los índices aceptados por la API. */
public final class EnvioMediciones extends SQLiteOpenHelper {
    private static final int TRABAJO = 204;
    private static final int PENDIENTE = 0, CONFIRMADA = 1, RECHAZADA = 2;
    private static final long SEMANA_MS = 7 * 24 * 3_600_000L;
    private static final ZoneId ZONA = ZoneId.of("Europe/Madrid");
    private static EnvioMediciones instancia;
    private final Context contexto;
    private final Handler principal = new Handler(Looper.getMainLooper());
    private final ApiRespira api = new ApiRespira();
    private boolean enviando;

    private static final class Lectura {
        final long id;
        final String cuerpo;

        // --------------------------------------------------------------
        // - id: Z, cuerpo: Text
        //       -->
        //     Lectura() -->
        // --------------------------------------------------------------
        Lectura(long id, String cuerpo) {
            this.id = id;
            this.cuerpo = cuerpo;
        }
    }

    // --------------------------------------------------------------
    // # effects: lee o escribe la cola SQLite.
    // + contexto: EntornoAndroid
    //       -->
    //     obtener() --x
    //       <--
    //   EnvioMediciones
    // --------------------------------------------------------------
    public static synchronized EnvioMediciones obtener(Context contexto) {
        if (instancia == null) instancia = new EnvioMediciones(contexto.getApplicationContext());
        return instancia;
    }

    // --------------------------------------------------------------
    // # effects: lee o escribe la cola SQLite.
    // - contexto: EntornoAndroid
    //       -->
    //     EnvioMediciones() -->
    // --------------------------------------------------------------
    private EnvioMediciones(Context contexto) {
        super(contexto, "mediciones.db", null, 1);
        this.contexto = contexto;
    }

    // --------------------------------------------------------------
    // # effects: lee o escribe la cola SQLite.
    // + bd: BaseLocal
    //       -->
    //     onCreate() -->
    // --------------------------------------------------------------
    @Override public void onCreate(SQLiteDatabase bd) {
        bd.execSQL("CREATE TABLE medicion (id INTEGER PRIMARY KEY, cuenta TEXT NOT NULL, "
                + "momento INTEGER NOT NULL, estado INTEGER NOT NULL DEFAULT 0, cuerpo TEXT)");
        bd.execSQL("CREATE INDEX medicion_cuenta_estado ON medicion(cuenta,estado,momento)");
    }

    // --------------------------------------------------------------
    // + bd: BaseLocal, vieja: Z, nueva: Z
    //       -->
    //     onUpgrade() -->
    // --------------------------------------------------------------
    @Override public void onUpgrade(SQLiteDatabase bd, int vieja, int nueva) { }

    // --------------------------------------------------------------
    // # effects: lee o escribe la cola SQLite; envía mediciones cuando hay conexión.
    // + cuenta: Text, medicion: Medicion
    //       -->
    //     guardar() -->
    // --------------------------------------------------------------
    public void guardar(String cuenta, Medicion medicion) {
        ContentValues fila = new ContentValues();
        fila.put("cuenta", cuenta);
        fila.put("momento", medicion.getMedidaEn());
        fila.put("cuerpo", medicion.toJSON());
        getWritableDatabase().insertOrThrow("medicion", null, fila);
        publicarEstado(cuenta);
        programar(cuenta);
        ConnectivityManager red = contexto.getSystemService(ConnectivityManager.class);
        if (red != null && red.getActiveNetwork() != null) enviar(ok -> { });
    }

    // --------------------------------------------------------------
    // # effects: coordina el trabajo de envío en segundo plano.
    // +
    //     reanudar() -->
    // --------------------------------------------------------------
    public void reanudar() {
        Usuario usuario = Repositorio.obtener(contexto).usuario();
        if (usuario != null) {
            publicarEstado(usuario.id);
            programar(usuario.id);
        }
    }

    // --------------------------------------------------------------
    // # effects: lee o escribe la cola SQLite.
    // + cuenta: Text
    //       -->
    //     borrarCuenta() -->
    // --------------------------------------------------------------
    public void borrarCuenta(String cuenta) {
        getWritableDatabase().delete("medicion", "cuenta = ?", new String[]{cuenta});
    }

    // --------------------------------------------------------------
    // # effects: coordina el trabajo de envío en segundo plano.
    // - cuenta: Text
    //       -->
    //     programar() -->
    // --------------------------------------------------------------
    private void programar(String cuenta) {
        JobScheduler trabajos = contexto.getSystemService(JobScheduler.class);
        if (trabajos == null || trabajos.getPendingJob(TRABAJO) != null || pendientes(cuenta) == 0) return;
        trabajos.schedule(new JobInfo.Builder(TRABAJO, new ComponentName(contexto, Trabajo.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true)
                .setBackoffCriteria(30_000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL).build());
    }

    // --------------------------------------------------------------
    // # effects: lee o escribe la cola SQLite.
    // - cuenta: Text
    //       -->
    //     pendientes() <--
    //       <--
    //   Z
    // --------------------------------------------------------------
    private int pendientes(String cuenta) {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM medicion WHERE cuenta = ? AND estado = 0",
                new String[]{cuenta})) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
    }

    // --------------------------------------------------------------
    // # effects: lee o escribe la cola SQLite; consulta el reloj.
    // - cuenta: Text
    //       -->
    //     publicarEstado() -->
    // --------------------------------------------------------------
    private void publicarEstado(String cuenta) {
        long inicio = Instant.now().atZone(ZONA).toLocalDate().atStartOfDay(ZONA).toInstant().toEpochMilli();
        int enviadas, rechazadas;
        long ultima;
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT SUM(estado = 1), SUM(estado = 2), MAX(CASE WHEN estado = 1 THEN momento END) "
                        + "FROM medicion WHERE cuenta = ? AND momento >= ?", new String[]{cuenta, Long.toString(inicio)})) {
            cursor.moveToFirst();
            enviadas = cursor.getInt(0);
            rechazadas = cursor.getInt(1);
            ultima = cursor.getLong(2);
        }
        int enCola = pendientes(cuenta);
        principal.post(() -> Repositorio.obtener(contexto).actualizarEnvios(cuenta, enviadas, enCola, rechazadas, ultima));
    }

    // --------------------------------------------------------------
    // # El lote queda ligado a la cuenta y al token capturados al iniciar el envío.
    // # effects: lee o escribe la cola SQLite; envía mediciones cuando hay conexión; consulta el reloj.
    // -
    //     enviar() -->
    //       <--
    //   B
    // --------------------------------------------------------------
    private void enviar(ApiRespira.Listo<Boolean> resultado) {
        Repositorio repo = Repositorio.obtener(contexto);
        final String cuenta, token;
        synchronized (repo) {
            Usuario usuario = repo.usuario();
            cuenta = usuario == null ? null : usuario.id;
            token = repo.token();
        }
        if (cuenta == null || token == null) {
            resultado.ok(true);
            return;
        }
        synchronized (this) {
            if (enviando) {
                resultado.ok(false);
                return;
            }
            enviando = true;
        }

        List<Lectura> lote = new ArrayList<>();
        long limite = System.currentTimeMillis() - SEMANA_MS;
        SQLiteDatabase bd = getWritableDatabase();
        bd.delete("medicion", "momento < ?", new String[]{Long.toString(limite)});
        try (Cursor cursor = bd.rawQuery(
                "SELECT id,cuerpo FROM medicion WHERE cuenta = ? AND estado = 0 ORDER BY id LIMIT 100",
                new String[]{cuenta})) {
            while (cursor.moveToNext()) lote.add(new Lectura(cursor.getLong(0), cursor.getString(1)));
        }
        if (lote.isEmpty()) {
            terminado(true, resultado);
            return;
        }
        JSONObject cuerpo = new JSONObject();
        try {
            JSONArray mediciones = new JSONArray();
            for (Lectura lectura : lote) mediciones.put(new JSONObject(lectura.cuerpo));
            cuerpo.put("mediciones", mediciones);
        } catch (JSONException e) {
            Log.w("Respira", "No se pudo preparar el lote");
            terminado(false, resultado);
            return;
        }

        api.pedir("POST", "/mediciones", cuerpo, token, new ApiRespira.Respuesta() {
            // --------------------------------------------------------------
            // + respuesta: Text
            //       -->
            //     ok() -->
            // --------------------------------------------------------------
            @Override public void ok(String respuesta) {
                try {
                    Set<Integer> errores = indicesRechazados(new JSONObject(respuesta), lote.size());
                    conciliar(bd, lote, errores);
                    publicarEstado(cuenta);
                    repo.refrescar(false);
                    terminado(true, aceptado -> {
                        if (pendientes(cuenta) > 0) enviar(resultado);
                        else resultado.ok(aceptado);
                    });
                } catch (JSONException e) {
                    terminado(false, resultado);
                }
            }

            // --------------------------------------------------------------
            // + e: ErrorApi
            //       -->
            //     error() -->
            // --------------------------------------------------------------
            @Override public void error(ApiRespira.Error e) {
                terminado(false, resultado);
            }
        });
    }

    // --------------------------------------------------------------
    // # Valida la respuesta completa antes de decidir qué filas del lote se confirman.
    // - respuesta: ObjetoJson, cantidad: Z
    //       -->
    //     indicesRechazados() --x
    //       <--
    //   Set[N] | ErrorDatos
    // --------------------------------------------------------------
    static Set<Integer> indicesRechazados(JSONObject respuesta, int cantidad) throws JSONException {
        JSONArray lista = respuesta.getJSONArray("errores");
        Set<Integer> indices = new HashSet<>();
        for (int i = 0; i < lista.length(); i++) {
            int rechazado = lista.getJSONObject(i).getInt("i");
            if (rechazado < 0 || rechazado >= cantidad || !indices.add(rechazado)) {
                throw new JSONException("Índice de lectura no válido");
            }
        }
        int guardadas = respuesta.getInt("guardadas"), repetidas = respuesta.getInt("repetidas");
        if (guardadas < 0 || repetidas < 0 || (long) guardadas + repetidas + indices.size() != cantidad) {
            throw new JSONException("Resultado incompleto");
        }
        return indices;
    }

    // --------------------------------------------------------------
    // # Solo confirma los índices aceptados; las filas rechazadas conservan su estado diferenciado.
    // # effects: lee o escribe la cola SQLite.
    // - bd: BaseLocal, lote: [Lectura], rechazadas: Set[Z | Ausente]
    //       -->
    //     conciliar() --x
    // --------------------------------------------------------------
    private static void conciliar(SQLiteDatabase bd, List<Lectura> lote, Set<Integer> rechazadas) {
        bd.beginTransaction();
        try {
            for (int i = 0; i < lote.size(); i++) {
                ContentValues cambios = new ContentValues();
                cambios.put("estado", rechazadas.contains(i) ? RECHAZADA : CONFIRMADA);
                cambios.putNull("cuerpo");
                bd.update("medicion", cambios, "id = ?", new String[]{Long.toString(lote.get(i).id)});
            }
            bd.setTransactionSuccessful();
        } finally {
            bd.endTransaction();
        }
    }

    // --------------------------------------------------------------
    // # effects: coordina el trabajo de envío en segundo plano.
    // - exito: B
    //       -->
    //     terminado() -->
    //       <--
    //   B
    // --------------------------------------------------------------
    private void terminado(boolean exito, ApiRespira.Listo<Boolean> resultado) {
        synchronized (this) { enviando = false; }
        principal.post(() -> resultado.ok(exito));
    }

    public static class Trabajo extends JobService {
        private JobParameters activo;

        // --------------------------------------------------------------
        // # effects: coordina el trabajo de envío en segundo plano.
        // + params: TrabajoAndroid
        //       -->
        //     onStartJob() -->
        //       <--
        //   B
        // --------------------------------------------------------------
        @Override public boolean onStartJob(JobParameters params) {
            activo = params;
            EnvioMediciones.obtener(this).enviar(ok -> {
                if (activo == params) {
                    activo = null;
                    jobFinished(params, !ok);
                }
            });
            return true;
        }

        // --------------------------------------------------------------
        // # effects: coordina el trabajo de envío en segundo plano.
        // + params: TrabajoAndroid
        //       -->
        //     onStopJob() -->
        //       <--
        //   B
        // --------------------------------------------------------------
        @Override public boolean onStopJob(JobParameters params) {
            if (activo == params) activo = null;
            return true;
        }
    }
}
