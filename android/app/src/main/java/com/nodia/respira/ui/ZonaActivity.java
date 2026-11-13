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
import com.nodia.respira.datos.Salud;
import com.nodia.respira.datos.Serie;
import com.nodia.respira.datos.Zona;

// ==============================================================
// Zona: valores actuales, media de 24 h, efectos si hay nivel Medio/Alto
// y evolución en 24 h / 7 días (con huecos cortos aproximados,
// alertas y picos).
// ==============================================================
public class ZonaActivity extends AppCompatActivity {

    public static final String EXTRA_ZONA = "zona";

    private Repositorio repo;
    private Zona zona;
    private Magnitud magnitud = Magnitud.O3;
    private boolean dia = true;
    private LinearLayout c;
    private String zonaId;
    private Serie serieServidor;      // la última serie pedida a la API
    private String claveSerie;        // de qué zona, magnitud y periodo es

    public static void abrir(Context ctx, String zonaId) {
        ctx.startActivity(new Intent(ctx, ZonaActivity.class).putExtra(EXTRA_ZONA, zonaId));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repo = Repositorio.obtener(this);
        zonaId = getIntent().getStringExtra(EXTRA_ZONA);
        zona = repo.zona(zonaId);
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
        Zona actual = repo.zona(zonaId);   // las zonas se renuevan al leer la API
        if (actual != null) {
            zona = actual;
        }
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

        // media de las últimas 24 h (la da el servidor)
        if (!Reglas.ESTADO_SIN_DATOS.equals(estado)) {
            StringBuilder m24 = new StringBuilder();
            for (Magnitud m : Magnitud.values()) {
                Double v = zona.media24h(m);
                if (v != null) {
                    m24.append(m24.length() == 0 ? "" : " · ").append(m.abreviatura).append(" ")
                            .append(m.formatear(v)).append(" ").append(m.unidad);
                }
            }
            if (m24.length() > 0) {
                Ui.valor(valores, "Media de las últimas 24 h", m24.toString());
            }
        }

        // efectos en la salud de lo que esté en Medio o Alto
        if (!Reglas.ESTADO_SIN_DATOS.equals(estado)) {
            for (Salud.Efecto ef : Salud.EFECTOS) {
                Double v = zona.valor(ef.magnitud);
                if (v == null || ef.magnitud.nivel(v) == Nivel.BAJO || ef.magnitud.nivel(v) == Nivel.NINGUNO) {
                    continue;
                }
                Nivel n = ef.magnitud.nivel(v);
                int grav = n == Nivel.ALTO ? Salud.DANA : Salud.MOLESTA;
                String efecto = "";
                for (Salud.Paso p : ef.pasos) {
                    if (p.gravedad == grav) {
                        efecto = p.efecto;
                    }
                }
                Ui.aviso(c, ef.nombre + " en " + n.nombre + ": " + efecto + ". Qué hacer: " + ef.hacer
                        + ". Más sensibles: " + String.join(", ", ef.peorPara) + ".", n);
            }
            Ui.boton(c, "Efectos en la salud", Ui.BOTON_TEXTO, v -> startActivity(new Intent(this, EfectosActivity.class)));
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
            Chip chip = new Chip(this, null, com.google.android.material.R.attr.chipStyle);
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

        String periodoElegido = dia ? "24h" : "7d";
        int tramos = dia ? 48 : 56;
        Serie serie;
        if (repo.datosReales()) {
            final String clave = zona.id + magnitud.id + periodoElegido;
            if (!clave.equals(claveSerie)) {
                claveSerie = clave;
                serieServidor = null;
                repo.serie(zona.id, magnitud, periodoElegido, datos -> {
                    if (clave.equals(claveSerie) && !isFinishing()) {
                        serieServidor = datos != null ? datos : new Serie(new Double[tramos], null);
                        pintar();
                    }
                });
            }
            serie = serieServidor != null ? serieServidor : new Serie(new Double[tramos], null);
            if (serieServidor == null) {
                Ui.nota(c, "Cargando la evolución desde el servidor…");
            }
        } else {
            serie = new Serie(DatosDemo.serie(zona, magnitud, periodoElegido), null);
            Ui.nota(c, "Datos de demostración: no hay conexión con el servidor.");
        }
        LinearLayout card = Ui.tarjeta(c);
        GraficaView g = new GraficaView(this);
        g.mostrar(serie.valores, serie.aprox, magnitud, dia);
        card.addView(g);

        // mínimo, media y máximo: solo con lo medido (lo aproximado no cuenta)
        double mn = Double.MAX_VALUE, mx = -Double.MAX_VALUE, suma = 0;
        int n = 0;
        for (Double v : serie.valores) {
            if (v != null) {
                mn = Math.min(mn, v);
                mx = Math.max(mx, v);
                suma += v;
                n++;
            }
        }
        LinearLayout stats = Ui.tarjeta(c);
        Ui.valor(stats, "Mínimo", magnitud.formatear(n == 0 ? null : mn) + (n == 0 ? "" : " " + magnitud.unidad));
        Ui.valor(stats, "Media", magnitud.formatear(n == 0 ? null : suma / n) + (n == 0 ? "" : " " + magnitud.unidad));
        Ui.valor(stats, "Máximo", magnitud.formatear(n == 0 ? null : mx) + (n == 0 ? "" : " " + magnitud.unidad));

        // alertas y picos del periodo
        if (serieServidor != null || !repo.datosReales()) {
            Ui.etiqueta(c, "Alertas y picos");
            LinearLayout ev = Ui.tarjeta(c);
            if (serie.alertas.isEmpty() && serie.picos.isEmpty()) {
                Ui.nota(ev, repo.datosReales() ? "Sin ratos en Medio o Alto en este periodo."
                        : "Las alertas y picos se calculan en el servidor.");
            }
            for (Serie.Evento a : serie.alertas) {
                Nivel nv = "alto".equals(a.nivel) ? Nivel.ALTO : Nivel.MEDIO;
                LinearLayout f = Ui.fila(ev, "En " + nv.nombre + " " + franja(a.desde, a.hasta),
                        "Máximo " + magnitud.formatear(a.valor) + " " + magnitud.unidad, null);
                Ui.insignia(f, nv.nombre, nv);
            }
            for (Serie.Evento p : serie.picos) {
                Nivel nv = magnitud.nivel(p.valor);
                LinearLayout f = Ui.fila(ev, "Pico de " + magnitud.formatear(p.valor) + " " + magnitud.unidad,
                        hora(p.desde, true), null);
                Ui.insignia(f, nv.nombre, nv);
            }
        }

        Ui.nota(c, "Media cada " + (dia ? "30 minutos" : "3 horas") + ". Línea de puntos: hueco corto (hasta 2 h) aproximado; "
                + "no cuenta en mínimo, media y máximo. Tramos grises: ratos más largos sin ningún sensor, que no se rellenan.");
        Ui.nota(c, getString(com.nodia.respira.R.string.nota_privacidad_mapa));
    }

    // ms --> hora() --> "lun 9 14:30" | "14:30"
    private static final java.time.format.DateTimeFormatter FMT_HORA = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
    private static final java.time.format.DateTimeFormatter FMT_DIA = java.time.format.DateTimeFormatter.ofPattern("EEE d HH:mm", new java.util.Locale("es", "ES"));

    public static String hora(long ms, boolean conDia) {
        java.time.ZonedDateTime t = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault());
        return (conDia ? FMT_DIA : FMT_HORA).format(t);
    }

    static String franja(long desde, long hasta) {
        java.time.ZoneId z = java.time.ZoneId.systemDefault();
        boolean mismoDia = java.time.Instant.ofEpochMilli(desde).atZone(z).toLocalDate()
                .equals(java.time.Instant.ofEpochMilli(hasta).atZone(z).toLocalDate());
        return hora(desde, true) + " – " + (mismoDia ? hora(hasta, false) : hora(hasta, true));
    }
}
