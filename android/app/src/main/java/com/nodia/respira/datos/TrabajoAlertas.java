package com.nodia.respira.datos;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

// # JobService <--- TrabajoAlertas; delega la actualización periódica en Repositorio.
// # EntornoAndroid = { aplicacion: Text }; identifica los recursos y servicios de la app.
// # TrabajoAndroid = { id: N }; trabajo programado por Android.

public class TrabajoAlertas extends JobService {
    private static final int ID = 203;
    private JobParameters activo;

    // --------------------------------------------------------------
    // # effects: programa o ejecuta comprobaciones de alertas con Android.
    // + contexto: EntornoAndroid, necesario: B
    //       -->
    //     programar() --x
    // --------------------------------------------------------------
    public static void programar(Context contexto, boolean necesario) {
        JobScheduler trabajos = contexto.getSystemService(JobScheduler.class);
        if (trabajos == null) return;
        boolean programado = trabajos.getPendingJob(ID) != null;
        if (necesario && !programado) {
            trabajos.schedule(new JobInfo.Builder(ID, new ComponentName(contexto, TrabajoAlertas.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(15 * 60_000L).setPersisted(true).build());
        } else if (!necesario && programado) {
            trabajos.cancel(ID);
        }
    }

    // --------------------------------------------------------------
    // # effects: programa o ejecuta comprobaciones de alertas con Android.
    // + params: TrabajoAndroid
    //       -->
    //     onStartJob() -->
    //       <--
    //   B
    // --------------------------------------------------------------
    @Override public boolean onStartJob(JobParameters params) {
        activo = params;
        Repositorio.obtener(this).comprobacionEnSegundoPlano(ok -> {
            if (activo == params) {
                activo = null;
                jobFinished(params, !ok);
            }
        });
        return true;
    }

    // --------------------------------------------------------------
    // # effects: programa o ejecuta comprobaciones de alertas con Android.
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
