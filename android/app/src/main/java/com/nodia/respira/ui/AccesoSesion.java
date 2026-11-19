package com.nodia.respira.ui;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

import com.nodia.respira.datos.Repositorio;

// Control común de acceso: protege también pantallas abiertas desde notificaciones
// y vuelve al acceso si la sesión caduca mientras se utiliza la app.
// El ciclo de vida y las notificaciones del repositorio se atienden en el hilo principal.
public final class AccesoSesion implements Application.ActivityLifecycleCallbacks {

    private final Repositorio repo;
    private Activity visible;

    // --------------------------------------------------------------
    // # effects: observa la sesión y el ciclo de vida de las pantallas.
    // + aplicacion: Application
    //       -->
    //     AccesoSesion() -->
    // --------------------------------------------------------------
    public AccesoSesion(Application aplicacion) {
        repo = Repositorio.obtener(aplicacion);
        aplicacion.registerActivityLifecycleCallbacks(this);
        repo.observar(this::comprobar);
    }

    // --------------------------------------------------------------
    // - pantalla: Activity
    //       -->
    //     requiereSesion() --x
    //       <--
    //   B
    // --------------------------------------------------------------
    static boolean requiereSesion(Activity pantalla) {
        return !(pantalla instanceof SplashActivity
                || pantalla instanceof OnboardingActivity
                || pantalla instanceof LoginActivity
                || pantalla instanceof RegistroActivity
                || pantalla instanceof RecuperarActivity
                || pantalla instanceof TextoActivity);
    }

    // --------------------------------------------------------------
    // # effects: abre el acceso y elimina las pantallas protegidas anteriores.
    // + pantalla: Activity
    //       -->
    //     abrirLogin() --x
    // --------------------------------------------------------------
    public static void abrirLogin(Activity pantalla) {
        pantalla.startActivity(new Intent(pantalla, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        pantalla.finish();
    }

    // --------------------------------------------------------------
    // # effects: abre la app y elimina los formularios de acceso de la pila.
    // + pantalla: Activity
    //       -->
    //     abrirPrincipal() --x
    // --------------------------------------------------------------
    public static void abrirPrincipal(Activity pantalla) {
        pantalla.startActivity(new Intent(pantalla, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        pantalla.finish();
    }

    // --------------------------------------------------------------
    // # effects: redirige al acceso si la pantalla visible exige una sesión.
    // -
    //     comprobar() -->
    // --------------------------------------------------------------
    private void comprobar() {
        Activity pantalla = visible;
        if (pantalla != null && !pantalla.isFinishing() && !pantalla.isDestroyed()
                && requiereSesion(pantalla) && !repo.haySesion()) {
            visible = null;
            abrirLogin(pantalla);
        }
    }

    // --------------------------------------------------------------
    // # effects: comprueba el acceso antes de mostrar la pantalla.
    // + pantalla: Activity
    //       -->
    //     onActivityStarted() -->
    // --------------------------------------------------------------
    @Override public void onActivityStarted(Activity pantalla) {
        visible = pantalla;
        comprobar();
    }

    // --------------------------------------------------------------
    // + pantalla: Activity
    //       -->
    //     onActivityStopped() -->
    // --------------------------------------------------------------
    @Override public void onActivityStopped(Activity pantalla) {
        if (visible == pantalla) visible = null;
    }

    // --------------------------------------------------------------
    // + pantalla: Activity, estado: EstadoPantalla | Ausente
    //       -->
    //     onActivityCreated() --x
    // --------------------------------------------------------------
    @Override public void onActivityCreated(Activity pantalla, Bundle estado) { }

    // --------------------------------------------------------------
    // + pantalla: Activity
    //       -->
    //     onActivityResumed() --x
    // --------------------------------------------------------------
    @Override public void onActivityResumed(Activity pantalla) { }

    // --------------------------------------------------------------
    // + pantalla: Activity
    //       -->
    //     onActivityPaused() --x
    // --------------------------------------------------------------
    @Override public void onActivityPaused(Activity pantalla) { }

    // --------------------------------------------------------------
    // + pantalla: Activity, estado: EstadoPantalla
    //       -->
    //     onActivitySaveInstanceState() --x
    // --------------------------------------------------------------
    @Override public void onActivitySaveInstanceState(Activity pantalla, Bundle estado) { }

    // --------------------------------------------------------------
    // + pantalla: Activity
    //       -->
    //     onActivityDestroyed() -->
    // --------------------------------------------------------------
    @Override public void onActivityDestroyed(Activity pantalla) {
        if (visible == pantalla) visible = null;
    }
}
