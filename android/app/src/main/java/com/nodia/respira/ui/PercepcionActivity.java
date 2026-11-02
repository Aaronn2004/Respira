package com.nodia.respira.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.radiobutton.MaterialRadioButton;
import com.nodia.respira.datos.Percepcion;
import com.nodia.respira.datos.Repositorio;
import com.nodia.respira.datos.Zona;

import java.util.Locale;

// ==============================================================
// Nueva percepción: tipo (ruido / calor / aire) y nivel 1..3, en el
// punto que el usuario ha dejado en el centro del mapa.
// ==============================================================
public class PercepcionActivity extends AppCompatActivity {

    private static final String EXTRA_LAT = "lat", EXTRA_LNG = "lng";

    private String tipo = null;
    private int nivel = 0;
    private LinearLayout niveles;

    public static void abrir(Context c, double lat, double lng) {
        c.startActivity(new Intent(c, PercepcionActivity.class).putExtra(EXTRA_LAT, lat).putExtra(EXTRA_LNG, lng));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repositorio repo = Repositorio.obtener(this);
        double lat = getIntent().getDoubleExtra(EXTRA_LAT, 0), lng = getIntent().getDoubleExtra(EXTRA_LNG, 0);
        Zona parque = repo.parqueEn(lat, lng);
        String lugar = parque != null ? parque.nombre
                : String.format(Locale.US, "Punto %.4f, %.4f", lat, lng);

        LinearLayout c = Ui.pantalla(this, "Nueva percepción");
        Ui.texto(c, "Lugar: " + lugar, 15, true, com.nodia.respira.R.color.tinta);
        Ui.nota(c, "Es el centro del mapa. Si no es correcto, vuelve y mueve el mapa.");

        Ui.etiqueta(c, "¿Qué quieres contar?");
        RadioGroup tipos = new RadioGroup(this);
        for (String t : Percepcion.TIPOS) {
            RadioButton rb = new MaterialRadioButton(this);
            rb.setId(View.generateViewId());
            rb.setText(Percepcion.nombreTipo(t));
            rb.setTag(t);
            tipos.addView(rb);
        }
        c.addView(tipos);

        niveles = new LinearLayout(this);
        niveles.setOrientation(LinearLayout.VERTICAL);
        c.addView(niveles);
        tipos.setOnCheckedChangeListener((g, id) -> {
            tipo = (String) g.findViewById(id).getTag();
            nivel = 0;
            pintarNiveles();
        });

        Ui.boton(c, "Publicar", Ui.BOTON_LLENO, v -> {
            if (tipo == null || nivel == 0) {
                Toast.makeText(this, "Elige qué notas y cuánto", Toast.LENGTH_SHORT).show();
                return;
            }
            repo.nuevaPercepcion(tipo, nivel, lat, lng, lugar);
            Toast.makeText(this, "Gracias. Se verá 24 h en el mapa, sin tu nombre.", Toast.LENGTH_LONG).show();
            finish();
        });
        Ui.nota(c, "Una percepción no es una medición: se muestra aparte, en la capa «Comunidad».");
    }

    private void pintarNiveles() {
        niveles.removeAllViews();
        Ui.etiqueta(niveles, "¿Cómo lo notas?");
        RadioGroup g = new RadioGroup(this);
        String[] nombres = Percepcion.nivelesTipo(tipo), detalles = Percepcion.detallesTipo(tipo);
        for (int i = 0; i < 3; i++) {
            RadioButton rb = new MaterialRadioButton(this);
            rb.setId(View.generateViewId());
            rb.setText(nombres[i] + " — " + detalles[i]);
            rb.setTag(i + 1);
            g.addView(rb);
        }
        g.setOnCheckedChangeListener((gr, id) -> nivel = (Integer) gr.findViewById(id).getTag());
        niveles.addView(g);
    }
}
