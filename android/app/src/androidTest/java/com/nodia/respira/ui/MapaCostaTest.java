package com.nodia.respira.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.nodia.respira.R;
import com.nodia.respira.datos.Repositorio;
import com.nodia.respira.datos.Zona;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class MapaCostaTest {

    // --------------------------------------------------------------
    // # effects: comprueba en el dispositivo el espacio visible del mapa vertical.
    // +
    //     encuadraCostaEnVertical() --x
    // --------------------------------------------------------------
    @Test public void encuadraCostaEnVertical() throws Exception {
        comprobarEncuadre(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, Configuration.ORIENTATION_PORTRAIT);
    }

    // --------------------------------------------------------------
    // # effects: comprueba el encuadre tras recrear la pantalla en horizontal.
    // +
    //     encuadraCostaEnHorizontal() --x
    // --------------------------------------------------------------
    @Test public void encuadraCostaEnHorizontal() throws Exception {
        comprobarEncuadre(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, Configuration.ORIENTATION_LANDSCAPE);
    }

    // --------------------------------------------------------------
    // # effects: gira la actividad de prueba y comprueba la proyección real de Maps.
    // - orientacion_solicitada: Z, orientacion_esperada: Z
    //       -->
    //     comprobarEncuadre() --x
    // --------------------------------------------------------------
    private static void comprobarEncuadre(int solicitada, int esperada) throws Exception {
        assumeTrue("La prueba del mapa requiere una sesión de prueba abierta",
                Repositorio.obtener(InstrumentationRegistry.getInstrumentation().getTargetContext()).haySesion());
        try (ActivityScenario<MainActivity> escenario = ActivityScenario.launch(MainActivity.class)) {
            escenario.onActivity(a -> a.setRequestedOrientation(solicitada));
            // La rotación se aplica mediante una recreación asíncrona de Android.
            Thread.sleep(1200);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            CountDownLatch listo = new CountDownLatch(1);
            AtomicReference<GoogleMap> mapa = new AtomicReference<>();
            escenario.onActivity(a -> {
                assertTrue(a.getResources().getConfiguration().orientation == esperada);
                Fragment f = a.getSupportFragmentManager().findFragmentByTag("tab_" + R.id.tab_mapa);
                assertNotNull(f);
                SupportMapFragment vista = (SupportMapFragment) f.getChildFragmentManager().findFragmentById(R.id.mapa);
                assertNotNull(vista);
                vista.getMapAsync(m -> { mapa.set(m); listo.countDown(); });
            });
            assertTrue("Maps no ha inicializado", listo.await(15, TimeUnit.SECONDS));
            onView(withId(R.id.encuadrar)).perform(click());
            escenario.onActivity(a -> {
                View cabecera = a.findViewById(R.id.cabecera);
                View pie = a.findViewById(R.id.pie);
                List<Zona> zonas = Repositorio.obtener(a).zonas();
                assertTrue("El catálogo debe contener las ocho zonas de la web", zonas.size() == 8);
                for (Zona z : zonas) {
                    for (List<List<double[]>> poligono : z.contornos()) {
                        for (List<double[]> anillo : poligono) {
                            for (double[] p : anillo) {
                                Point punto = mapa.get().getProjection().toScreenLocation(new LatLng(p[0], p[1]));
                                assertTrue(z.nombre + " queda bajo la cabecera", punto.y >= cabecera.getBottom());
                                assertTrue(z.nombre + " queda bajo el pie", punto.y <= pie.getTop());
                            }
                        }
                    }
                }
                a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            });
        }
    }
}
