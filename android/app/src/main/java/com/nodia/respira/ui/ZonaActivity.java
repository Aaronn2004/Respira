package com.nodia.respira.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;
import com.nodia.respira.datos.DatosDemo;
import com.nodia.respira.datos.Magnitud;
import com.nodia.respira.datos.Nivel;
import com.nodia.respira.datos.Reglas;
import com.nodia.respira.datos.Repositorio;
import com.nodia.respira.datos.Zona;

// ==============================================================
// Zona: valores actuales + evolución en 24 h / 7 días.
// ==============================================================
public class ZonaActivity extends AppCompatActivity {

    public static final String EXTRA_ZONA = "zona";

    private Repositorio repo;
    private Zona zona;
    private Magnitud magnitud = Magnitud.O3;
    private boolean dia = true;
    private LinearLayout c;

    public static void abrir(Context ctx, String zonaId) {
        ctx.startActivity(new Intent(ctx, ZonaActivity.class).putExtra(EXTRA_ZONA, zonaId));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repo = Repositorio.obtener(this);
        zona = repo.zona(getIntent().getStringExtra(EXTRA_ZONA));
        if (zona == null) {
            finish();
            return;
        }
        c = Ui.pantalla(this, zona.nombre);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (zona != null) {
            pintar();
        }
    }

    private void pintar() {
        c.removeAllViews();
        String estado = zona.estado();
        Ui.texto(c, (zona.esParque() ? "Parque" : "Barrio") + " · " + zona.numMediciones + " mediciones en la última hora",
                13, false, com.nodia.respira.R.color.tinta_3);
        if (Reglas.ESTADO_ANTIGUO.equals(estado)) {
            Ui.aviso(c, "La última medición es " + Reglas.haceTexto(zona.haceMin) + ". Puede no reflejar cómo está ahora.", Nivel.MEDIO);
        }

        LinearLayout valores = Ui.tarjeta(c);
        for (Magnitud m : Magnitud.values()) {
            Double v = zona.valor(m);
            boolean sin = v == null || Reglas.ESTADO_SIN_DATOS.equals(estado);
            LinearLayout f = Ui.fila(valores, m.nombre + (sin ? "" : " · " + m.formatear(v) + " " + m.unidad),
                    sin ? "Nadie con sensor ha pasado por aquí en las últimas 24 h" : "Última medición " + Reglas.haceTexto(zona.haceMin), null);
            Ui.insignia(f, sin ? "Sin datos" : Reglas.ESTADO_ANTIGUO.equals(estado) ? "Antiguo" : m.nivel(v).nombre,
                    sin || Reglas.ESTADO_ANTIGUO.equals(estado) ? Nivel.NINGUNO : m.nivel(v));
        }

        if (zona.esParque()) {
            boolean fav = repo.esFavorito(zona.id);
            Ui.boton(c, fav ? "★ Guardado en Mis parques" : "☆ Guardar parque", fav ? Ui.BOTON_LLENO : Ui.BOTON_BORDE, v -> {
                repo.alternarFavorito(zona.id);
                pintar();
            });
            Ui.boton(c, "🔔 " + (repo.alerta(zona.id) == null ? "Crear alerta" : "Editar alerta"), Ui.BOTON_BORDE,
                    v -> AlertaActivity.abrir(this, zona.id));
        }

        Ui.etiqueta(c, "Evolución");
        if (Reglas.ESTADO_SIN_DATOS.equals(estado)) {
            Ui.vacio(c, "Sin datos en este periodo", "No ha pasado nadie con sensor por esta zona.", null, null);
            return;
        }
        ChipGroup chips = new ChipGroup(this);
        chips.setSingleSelection(true);
        chips.setSelectionRequired(true);
        for (Magnitud m : Magnitud.values()) {
            Chip chip = new Chip(this, null, com.google.android.material.R.attr.chipChoiceStyle);
            chip.setText(m.nombre);
            chip.setCheckable(true);
            chip.setChecked(m == magnitud);
            chip.setOnClickListener(v -> {
                magnitud = m;
                pintar();
            });
            chips.addView(chip);
        }
        c.addView(chips);

        MaterialButtonToggleGroup periodo = new MaterialButtonToggleGroup(this);
        periodo.setSingleSelection(true);
        periodo.setSelectionRequired(true);
        MaterialButton b24 = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b24.setText("24 horas");
        b24.setId(android.view.View.generateViewId());
        MaterialButton b7 = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b7.setText("7 días");
        b7.setId(android.view.View.generateViewId());
        periodo.addView(b24, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        periodo.addView(b7, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        periodo.check(dia ? b24.getId() : b7.getId());
        periodo.addOnButtonCheckedListener((g, id, marcado) -> {
            if (marcado && (id == b24.getId()) != dia) {
                dia = id == b24.getId();
                pintar();
            }
        });
        c.addView(periodo);

        Double[] serie = DatosDemo.serie(zona, magnitud, dia ? "24h" : "7d");
        LinearLayout card = Ui.tarjeta(c);
        GraficaView g = new GraficaView(this);
        g.mostrar(serie, magnitud, dia);
        card.addView(g);

        double mn = Double.MAX_VALUE, mx = -Double.MAX_VALUE, suma = 0;
        int n = 0;
        for (Double v : serie) {
            if (v != null) {
                mn = Math.min(mn, v);
                mx = Math.max(mx, v);
                suma += v;
                n++;
            }
        }
        LinearLayout stats = Ui.tarjeta(c);
        Ui.valor(stats, "Mínimo", magnitud.formatear(mn) + " " + magnitud.unidad);
        Ui.valor(stats, "Media", magnitud.formatear(n == 0 ? null : suma / n) + " " + magnitud.unidad);
        Ui.valor(stats, "Máximo", magnitud.formatear(mx) + " " + magnitud.unidad);
        Ui.nota(c, "Media cada " + (dia ? "30 minutos" : "3 horas") + ". Los tramos grises son horas en las que no pasó ningún sensor: no se rellenan.");
        Ui.nota(c, getString(com.nodia.respira.R.string.nota_privacidad_mapa));
    }
}
