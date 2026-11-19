package com.nodia.respira.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import java.util.DoubleSummaryStatistics;

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
// --------------------------------------------------------------
// class ZonaActivity
// # Detalle de zona, series, estadísticas y alertas.
// # AppCompatActivity <--- ZonaActivity
// # Proyecciones lógicas de interfaz; se omiten mecanismos de Android.
// Entorno = { densidad: R, tema: Text }
// Vista = { contenido: Text, visible: B, habilitada: B }
// EstadoPantalla = Map[Text, Text | Z | R | B | Ausente]
// --------------------------------------------------------------
public class ZonaActivity extends AppCompatActivity {

    public static final String EXTRA_ZONA = "zona";

    private Repositorio repo;
    private Zona zona;
    private Magnitud magnitud = Magnitud.O3;
    private boolean dia = true;
    private LinearLayout c;
    private String zonaId;
    private Serie serieServidor;      // la última serie pedida a la API
    private boolean falloSerie;
    private int generacionSerie;
    private String claveSerie;        // de qué zona, magnitud y periodo es

    // --------------------------------------------------------------
    // # effects: abre la pantalla de destino
    // + ctx: Entorno, zonaId: Text
    //       -->
    //           abrir() --x
    // --------------------------------------------------------------
    public static void abrir(Context ctx, String zonaId) {
        ctx.startActivity(new Intent(ctx, ZonaActivity.class).putExtra(EXTRA_ZONA, zonaId));
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // + savedInstanceState: EstadoPantalla | Ausente
    //       -->
    //           onCreate() -->
    // --------------------------------------------------------------
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

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz
    // +
    //   onResume() -->
    // --------------------------------------------------------------
    @Override
    protected void onResume() {
        super.onResume();
        if (zona != null) {
            pintar();
        }
    }

    // --------------------------------------------------------------
    // # La generación descarta respuestas de una selección anterior; el resumen excluye aproximaciones.
    // # effects: lee el repositorio, solicita la serie y actualiza la interfaz
    // -
    //   pintar() -->
    // --------------------------------------------------------------
    private void pintar() {
        Zona actual = repo.zona(zonaId);   // las zonas se renuevan al leer la API
        if (actual != null) {
            zona = actual;
        }
        c.removeAllViews();
        String estado = zona.estado();
        Ui.texto(c, zona.tipoTexto() + " · " + zona.numMediciones + " mediciones en la última hora",
                13, false, com.nodia.respira.R.color.tinta_3);
        LinearLayout valores = Ui.tarjeta(c);
        for (Magnitud m : Magnitud.values()) Ui.datoZona(valores, zona, m);

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
                if (!Reglas.ESTADO_OK.equals(zona.estado(ef.magnitud))
                        || !"real".equals(zona.dato(ef.magnitud).origen)) continue;
                Ui.aviso(c, ef.nombre + " en nivel " + n.nombre + " en la escala de Respira. "
                        + ef.hacer + " Más sensibles: " + String.join(", ", ef.peorPara) + ".", n);
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
        Serie serie;
        if (repo.datosReales()) {
            final String clave = zona.id + ":" + magnitud.id + ":" + periodoElegido;
            if (!clave.equals(claveSerie)) {
                claveSerie = clave;
                serieServidor = null;
                falloSerie = false;
                int solicitud = ++generacionSerie;
                repo.serie(zona.id, magnitud, periodoElegido, datos -> {
                    if (solicitud == generacionSerie && !isFinishing() && !isDestroyed()) {
                        serieServidor = datos;
                        falloSerie = datos == null;
                        pintar();
                    }
                });
            }
            if (falloSerie) {
                Ui.vacio(c, "No se pudo cargar la evolución", "Comprueba la conexión e inténtalo de nuevo.",
                        "Reintentar", v -> { claveSerie = null; pintar(); });
                return;
            }
            if (serieServidor == null) {
                Ui.nota(c, "Cargando la evolución desde el servidor…");
                return;
            }
            serie = serieServidor;
        } else {
            serie = new Serie(DatosDemo.serie(zona, magnitud, periodoElegido), null);
            Ui.nota(c, "Datos de demostración: no hay conexión con el servidor.");
        }
        if (java.util.Arrays.stream(serie.valores).allMatch(java.util.Objects::isNull)) {
            Ui.vacio(c, "Sin datos en este periodo", "Elige otra magnitud u otro periodo para consultar la evolución.", null, null);
            return;
        }
        LinearLayout card = Ui.tarjeta(c);
        GraficaView g = new GraficaView(this);
        g.mostrar(serie.valores, serie.aprox, serie.origen, magnitud, dia);
        if (serie.tieneSimulacion()) Ui.nota(card, "Trazo discontinuo: datos simulados.");
        card.addView(g);

        // mínimo, media y máximo: solo con lo medido (lo aproximado no cuenta)
        DoubleSummaryStatistics resumen = new DoubleSummaryStatistics();
        for (Double v : serie.valores) if (v != null) resumen.accept(v);
        boolean vacio = resumen.getCount() == 0;
        LinearLayout stats = Ui.tarjeta(c);
        mostrarEstadistica(stats, "Mínimo", serie.conResumen ? serie.minimo : vacio ? null : resumen.getMin());
        mostrarEstadistica(stats, "Media", serie.conResumen ? serie.media : vacio ? null : resumen.getAverage());
        mostrarEstadistica(stats, "Máximo", serie.conResumen ? serie.maximo : vacio ? null : resumen.getMax());

        if (serieServidor != null) {
            Ui.etiqueta(c, "Alertas y picos");
            LinearLayout ev = Ui.tarjeta(c);
            if (serie.alertas.isEmpty() && serie.picos.isEmpty() && serie.avisos.isEmpty()) {
                Ui.nota(ev, "No hay alertas ni picos en este periodo.");
            }
            for (Serie.Evento a : serie.alertas) {
                Nivel nv = Nivel.desdeId(a.nivel);
                evento(ev, a, "En " + nv.nombre + " " + franja(a.desde, a.hasta), "Máximo ", nv);
            }
            for (Serie.Evento p : serie.picos) {
                evento(ev, p, "Pico · " + hora(p.desde, true), "", magnitud.nivel(p.valor));
            }
            for (Serie.Evento a : serie.avisos) {
                evento(ev, a, "Aviso emitido · " + hora(a.desde, true), "", magnitud.nivel(a.valor));
            }
        }

        Ui.nota(c, "Media cada " + (dia ? "30 minutos" : "3 horas") + ". Línea de puntos: hueco corto (hasta 2 h) aproximado; "
                + "no cuenta en mínimo, media y máximo. Tramos grises: ratos más largos sin ningún sensor, que no se rellenan.");
        Ui.nota(c, getString(com.nodia.respira.R.string.nota_privacidad_mapa));
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // - tarjeta: Vista, titulo: Text, valor: R | Ausente
    //       -->
    //           mostrarEstadistica() <--
    // --------------------------------------------------------------
    private void mostrarEstadistica(LinearLayout tarjeta, String titulo, Double valor) {
        Ui.valor(tarjeta, titulo, valor == null ? "—" : magnitud.formatear(valor) + " " + magnitud.unidad);
    }

    // --------------------------------------------------------------
    // # effects: actualiza la interfaz Android
    // - lista: Vista, evento: Serie.Evento, titulo: Text, prefijo: Text, nivel: Nivel
    //       -->
    //           evento() <--
    // --------------------------------------------------------------
    private void evento(LinearLayout lista, Serie.Evento evento, String titulo, String prefijo, Nivel nivel) {
        String origen = "simulado".equals(evento.origen) ? " · Simulado" : "";
        LinearLayout fila = Ui.fila(lista, titulo, prefijo + magnitud.formatear(evento.valor)
                + " " + magnitud.unidad + origen, null);
        Ui.insignia(fila, nivel.nombre, nivel);
    }

    // ms --> hora() --> "lun 9 14:30" | "14:30"
    private static final java.time.format.DateTimeFormatter FMT_HORA = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
    private static final java.time.format.DateTimeFormatter FMT_DIA = java.time.format.DateTimeFormatter.ofPattern("EEE d HH:mm", new java.util.Locale("es", "ES"));

    // --------------------------------------------------------------
    // # effects: lee la zona horaria del dispositivo
    // + ms: DateTime, conDia: B
    //       -->
    //           hora() --x
    //       <--
    //   resultado: Text
    // --------------------------------------------------------------
    public static String hora(long ms, boolean conDia) {
        java.time.ZonedDateTime t = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault());
        return (conDia ? FMT_DIA : FMT_HORA).format(t);
    }

    // --------------------------------------------------------------
    // # effects: lee la zona horaria del dispositivo
    // + desde: DateTime, hasta: DateTime
    //       -->
    //           franja() --x
    //       <--
    //   resultado: Text
    // --------------------------------------------------------------
    static String franja(long desde, long hasta) {
        java.time.ZoneId z = java.time.ZoneId.systemDefault();
        boolean mismoDia = java.time.Instant.ofEpochMilli(desde).atZone(z).toLocalDate()
                .equals(java.time.Instant.ofEpochMilli(hasta).atZone(z).toLocalDate());
        return hora(desde, true) + " – " + (mismoDia ? hora(hasta, false) : hora(hasta, true));
    }
}
