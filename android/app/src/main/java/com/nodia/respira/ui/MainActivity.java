package com.nodia.respira.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.nodia.respira.R;

// ==============================================================
// MainActivity: las 5 pestañas (Mapa · Parques · Sensor · Puntos · Más).
// Cada pestaña es un Fragment; se muestran/ocultan para no recargar
// el mapa al cambiar de pestaña.
// ==============================================================
public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_PESTANA = "pestana";

    private BottomNavigationView navegacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Ui.insets(findViewById(R.id.contenedor), true, false);
        Notificaciones.crearCanales(this);

        navegacion = findViewById(R.id.navegacion);
        navegacion.setOnItemSelectedListener(item -> {
            mostrar(item.getItemId());
            return true;
        });
        navegacion.setOnItemReselectedListener(item -> { });

        if (savedInstanceState == null) {
            int pestana = getIntent().getIntExtra(EXTRA_PESTANA, R.id.tab_mapa);
            if (pestana == R.id.tab_mapa) {
                mostrar(pestana); // ya está seleccionada: el listener no salta
            } else {
                navegacion.setSelectedItemId(pestana);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        int pestana = intent.getIntExtra(EXTRA_PESTANA, 0);
        if (pestana != 0) {
            irA(pestana);
        }
    }

    // Cambiar de pestaña desde un fragment (p. ej. «Ir al mapa»)
    public void irA(int idPestana) {
        navegacion.setSelectedItemId(idPestana);
    }

    private Fragment crear(int id) {
        if (id == R.id.tab_parques) return new ParquesFragment();
        if (id == R.id.tab_sensor) return new SensorFragment();
        if (id == R.id.tab_puntos) return new PuntosFragment();
        if (id == R.id.tab_mas) return new MasFragment();
        return new MapaFragment();
    }

    private void mostrar(int id) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        String etiqueta = "tab_" + id;
        for (Fragment f : fm.getFragments()) {
            if (!etiqueta.equals(f.getTag())) {
                tx.hide(f);
            }
        }
        Fragment f = fm.findFragmentByTag(etiqueta);
        if (f == null) {
            tx.add(R.id.contenedor, crear(id), etiqueta);
        } else {
            tx.show(f);
        }
        tx.commitNow();
    }
}
