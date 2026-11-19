package com.nodia.respira.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertNotNull;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.nodia.respira.datos.Repositorio;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AccesoSesionTest {

    // --------------------------------------------------------------
    // # effects: cierra la sesión de pruebas antes de comprobar el acceso.
    // +
    //     sinSesion() --x
    // --------------------------------------------------------------
    @Before public void sinSesion() {
        Instrumentation i = InstrumentationRegistry.getInstrumentation();
        i.runOnMainSync(() -> Repositorio.obtener(i.getTargetContext()).cerrarSesion());
        i.waitForIdleSync();
    }

    // --------------------------------------------------------------
    // # effects: intenta abrir la aplicación sin una sesión válida.
    // +
    //     noSePuedeAbrirElMapaSinSesion() --x
    // --------------------------------------------------------------
    @Test public void noSePuedeAbrirElMapaSinSesion() {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        comprobarRedireccion(new Intent(c, MainActivity.class));
    }

    // --------------------------------------------------------------
    // # effects: intenta abrir una zona como lo haría una notificación.
    // +
    //     unaNotificacionNoEvitaElInicioDeSesion() --x
    // --------------------------------------------------------------
    @Test public void unaNotificacionNoEvitaElInicioDeSesion() {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String zona = Repositorio.obtener(c).zonas().get(0).id;
        comprobarRedireccion(new Intent(c, ZonaActivity.class).putExtra(ZonaActivity.EXTRA_ZONA, zona));
    }

    // --------------------------------------------------------------
    // # effects: salta la introducción y comprueba la siguiente pantalla.
    // +
    //     saltarIntroduccionAbreElAcceso() --x
    // --------------------------------------------------------------
    @Test public void saltarIntroduccionAbreElAcceso() {
        Instrumentation i = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = i.addMonitor(LoginActivity.class.getName(), null, false);
        try (ActivityScenario<OnboardingActivity> escenario = ActivityScenario.launch(OnboardingActivity.class)) {
            onView(withText("Saltar")).perform(click());
            Activity acceso = i.waitForMonitorWithTimeout(monitor, 5000);
            assertNotNull("La introducción debe dar paso al acceso, no al mapa", acceso);
            i.runOnMainSync(acceso::finish);
        } finally {
            i.removeMonitor(monitor);
        }
    }

    // --------------------------------------------------------------
    // # effects: abre la pantalla y comprueba que se sustituye por el acceso.
    // - destino: Intent
    //       -->
    //     comprobarRedireccion() --x
    // --------------------------------------------------------------
    private static void comprobarRedireccion(Intent destino) {
        Instrumentation i = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = i.addMonitor(LoginActivity.class.getName(), null, false);
        try {
            i.getTargetContext().startActivity(destino
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            Activity acceso = i.waitForMonitorWithTimeout(monitor, 5000);
            assertNotNull("Una pantalla protegida debe abrir el inicio de sesión", acceso);
            i.runOnMainSync(acceso::finish);
        } finally {
            i.removeMonitor(monitor);
        }
    }
}
