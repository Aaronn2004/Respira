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
// También sirve para EDITAR una percepción propia (tipo y nivel;
// conserva el lugar y la hora en que se publicó).
// ==============================================================
public class PercepcionActivity extends AppCompatActivity {

    private static final String EXTRA_LAT = "lat", EXTRA_LNG = "lng", EXTRA_ID = "id";

    private String tipo = null;
    private int nivel = 0;
    private LinearLayout niveles;

    public static void abrir(Context c, double lat, double lng) {
        c.startActivity(new Intent(c, PercepcionActivity.class).putExtra(EXTRA_LAT, lat).putExtra(EXTRA_LNG, lng));
    }

    public static void editar(Context c, long id) {
        c.startActivity(new Intent(c, PercepcionActivity.class).putExtra(EXTRA_ID, id));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repositorio repo = Repositorio.obtener(this);
        long idEditar = getIntent().getLongExtra(EXTRA_ID, -1);
        Percepcion original = idEditar >= 0 ? repo.percepcion(idEditar) : null;
        if (idEditar >= 0 && (original == null || !repo.esMia(original))) {
            Toast.makeText(this, "Solo puedes editar tus propias percepciones", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        boolean editando = original != null;

        final double lat, lng;
        final String lugar;
        if (editando) {
            lat = original.lat;
            lng = original.lng;
            lugar = original.direccion;
            tipo = original.tipo;
            nivel = original.nivel;
        } else {
            lat = getIntent().getDoubleExtra(EXTRA_LAT, 0);
            lng = getIntent().getDoubleExtra(EXTRA_LNG, 0);
            Zona parque = repo.parqueEn(lat, lng);
            lugar = parque != null ? parque.nombre : String.format(Locale.US, "Punto %.4f, %.4f", lat, lng);
        }

        LinearLayout c = Ui.pantalla(this, editando ? "Editar percepción" : "Nueva percepción");
        Ui.texto(c, "Lugar: " + lugar, 15, true, com.nodia.respira.R.color.tinta);
        Ui.nota(c, editando ? "El lugar no se cambia. Si te equivocaste de sitio, bórrala y publícala de nuevo."
                : "Es el centro del mapa. Si no es correcto, vuelve y mueve el mapa.");

        Ui.etiqueta(c, "¿Qué quieres contar?");
        RadioGroup tipos = new RadioGroup(this);
        for (String t : Percepcion.TIPOS) {
            RadioButton rb = new MaterialRadioButton(this);
            rb.setId(View.generateViewId());
            rb.setText(Percepcion.nombreTipo(t));
            rb.setTag(t);
            tipos.addView(rb);
            if (t.equals(tipo)) {
                rb.setChecked(true);
            }
        }
        c.addView(tipos);

        niveles = new LinearLayout(this);
        niveles.setOrientation(LinearLayout.VERTICAL);
        c.addView(niveles);
        if (tipo != null) {
            pintarNiveles();
        }
        tipos.setOnCheckedChangeListener((g, id) -> {
            tipo = (String) g.findViewById(id).getTag();
            nivel = 0;
            pintarNiveles();
        });

        Ui.boton(c, editando ? "Guardar cambios" : "Publicar", Ui.BOTON_LLENO, v -> {
            if (tipo == null || nivel == 0) {
                Toast.makeText(this, "Elige qué notas y cuánto", Toast.LENGTH_SHORT).show();
                return;
            }
            if (editando) {
                if (repo.editarPercepcion(original.id, tipo, nivel)) {
                    Toast.makeText(this, "Percepción actualizada", Toast.LENGTH_SHORT).show();
                }
                finish();
                return;
            }
            if (!repo.nuevaPercepcion(tipo, nivel, lat, lng, lugar)) {
                Toast.makeText(this, "Has publicado 3 percepciones en la última hora. Espera un poco.", Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(this, "Gracias. Se verá 24 h en el mapa, sin tu nombre.", Toast.LENGTH_LONG).show();
            finish();
        });
        Ui.nota(c, "Una percepción no es una medición: se muestra aparte, en la capa «Comunidad». "
                + "Puedes editarla o borrarla en Más › Mis percepciones.");
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
            if (nivel == i + 1) {
                rb.setChecked(true);
            }
        }
        g.setOnCheckedChangeListener((gr, id) -> nivel = (Integer) gr.findViewById(id).getTag());
        niveles.addView(g);
    }
}
