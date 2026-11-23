package com.nodia.respira.ui;

import com.nodia.respira.datos.Serie;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

// Solo presentación: instantes originales, geometría y paginación de rótulos.
public final class ReferenciasGrafica {
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm", new Locale("es", "ES"));
    private ReferenciasGrafica() { }

    public static String hora(long instante) {
        ZonedDateTime t = Instant.ofEpochMilli(instante).atZone(GraficaHistorica.ZONA);
        String texto = FECHA_HORA.format(t);
        // Una hora civil repetida se distingue por su offset, nunca por un desplazamiento del punto.
        if (GraficaHistorica.ZONA.getRules().getValidOffsets(t.toLocalDateTime()).size() > 1) {
            texto += " " + t.getOffset().getId();
        }
        return texto;
    }

    public static final class Referencia {
        public final String id;
        public final long[] instantes;
        public final double[] posiciones;
        public final String[] lineas;
        public final boolean pico;

        private Referencia(Serie serie, String id, boolean pico, long... instantes) {
            this.id = id;
            this.pico = pico;
            this.instantes = instantes;
            posiciones = new double[instantes.length];
            lineas = new String[instantes.length + 1];
            lineas[0] = id;
            for (int i = 0; i < instantes.length; i++) {
                posiciones[i] = serie.posicionTemporal(instantes[i]);
                lineas[i + 1] = (instantes.length == 2 ? (i == 0 ? "↦ " : "↤ ") : "") + hora(instantes[i]);
            }
        }

        double centro() { return (posiciones[0] + posiciones[posiciones.length - 1]) / 2; }
    }

    public static List<Referencia> eventos(Serie serie, double ancho, double distanciaAgrupar) {
        List<Referencia> salida = new ArrayList<>();
        if (!serie.tieneIntervalo() || ancho <= 0) return salida;
        for (int i = 0; i < serie.alertas.size(); i++) {
            Serie.Evento a = serie.alertas.get(i);
            if (a.desde >= a.hasta || a.hasta <= serie.desde || a.desde >= serie.hasta) continue;
            String id = "A" + (i + 1);
            boolean inicioVisible = a.desde >= serie.desde;
            boolean finVisible = a.hasta <= serie.hasta;
            if (inicioVisible && finVisible
                    && (serie.posicionTemporal(a.hasta) - serie.posicionTemporal(a.desde)) * ancho <= distanciaAgrupar) {
                salida.add(new Referencia(serie, id + " inicio–fin", false, a.desde, a.hasta));
            } else {
                if (inicioVisible) salida.add(new Referencia(serie, id + " inicio", false, a.desde));
                if (finVisible) salida.add(new Referencia(serie, id + " fin", false, a.hasta));
            }
        }
        for (GraficaHistorica.Pico p : GraficaHistorica.picos(serie)) {
            salida.add(new Referencia(serie, p.etiqueta, true, p.evento.desde));
        }
        salida.sort(Comparator.comparingLong(r -> r.instantes[0]));
        return salida;
    }

    public static final class Etiqueta {
        public final Referencia referencia;
        public final double izquierda;
        public final double ancho;
        public final int fila;

        private Etiqueta(Referencia referencia, double izquierda, double ancho, int fila) {
            this.referencia = referencia;
            this.izquierda = izquierda;
            this.ancho = ancho;
            this.fila = fila;
        }
    }

    // Dos filas como máximo por página. No desplaza rótulos lejos de sus instantes para hacerlos caber.
    public static List<List<Etiqueta>> paginas(List<Referencia> referencias, double ancho,
                                               ToDoubleFunction<String> medir, double margen) {
        List<List<Etiqueta>> paginas = new ArrayList<>();
        if (ancho <= 0) return paginas;
        List<Etiqueta> pagina = new ArrayList<>();
        double[] fin = {-Double.MAX_VALUE, -Double.MAX_VALUE};
        for (Referencia referencia : referencias) {
            double caja = 0;
            for (String linea : referencia.lineas) caja = Math.max(caja, medir.applyAsDouble(linea));
            caja = Math.min(ancho, caja + 2 * margen);
            double izquierda = Math.max(0, Math.min(ancho - caja, referencia.centro() * ancho - caja / 2));
            int fila = izquierda >= fin[0] ? 0 : izquierda >= fin[1] ? 1 : -1;
            if (fila < 0) {
                paginas.add(pagina);
                pagina = new ArrayList<>();
                fin = new double[]{-Double.MAX_VALUE, -Double.MAX_VALUE};
                fila = 0;
            }
            pagina.add(new Etiqueta(referencia, izquierda, caja, fila));
            fin[fila] = izquierda + caja + margen;
        }
        if (!pagina.isEmpty()) paginas.add(pagina);
        return paginas;
    }

    public static final class RotuloPico {
        public final GraficaHistorica.Pico pico;
        public final double x, y, izquierda, arriba, ancho, alto, enlaceX, enlaceY;

        private RotuloPico(GraficaHistorica.Pico pico, double x, double y,
                           double izquierda, double arriba, double ancho, double alto) {
            this.pico = pico;
            this.x = x;
            this.y = y;
            this.izquierda = izquierda;
            this.arriba = arriba;
            this.ancho = ancho;
            this.alto = alto;
            enlaceX = Math.max(izquierda, Math.min(izquierda + ancho, x));
            enlaceY = Math.max(arriba, Math.min(arriba + alto, y));
        }
    }

    public static List<RotuloPico> rotulosPicos(Serie serie, double ancho, double alto,
                                                double min, double max, double cajaAncho, double cajaAlto, double espacio) {
        List<RotuloPico> salida = new ArrayList<>();
        if (!serie.tieneIntervalo() || ancho < cajaAncho || alto < cajaAlto || max <= min) return salida;
        List<GraficaHistorica.Pico> picos = GraficaHistorica.picosPorTiempo(serie);
        for (GraficaHistorica.Pico pico : picos) {
            double x = pico.posicion * ancho;
            double y = (max - pico.evento.valor) / (max - min) * alto;
            RotuloPico mejor = null;
            double distancia = Double.POSITIVE_INFINITY;
            for (int anillo = 1; anillo <= 5; anillo++) {
                double d = espacio * anillo;
                double[] xs = {x + d, x - cajaAncho - d, x - cajaAncho / 2};
                double[] ys = {y - cajaAlto - d, y + d, y - cajaAlto / 2};
                for (double candidatoX : xs) for (double candidatoY : ys) {
                    double a = Math.max(0, Math.min(ancho - cajaAncho, candidatoX));
                    double b = Math.max(0, Math.min(alto - cajaAlto, candidatoY));
                    boolean libre = true;
                    for (RotuloPico r : salida) {
                        if (a < r.izquierda + r.ancho + espacio / 2 && a + cajaAncho + espacio / 2 > r.izquierda
                                && b < r.arriba + r.alto + espacio / 2 && b + cajaAlto + espacio / 2 > r.arriba) libre = false;
                    }
                    for (GraficaHistorica.Pico otro : picos) {
                        double ox = otro.posicion * ancho, oy = (max - otro.evento.valor) / (max - min) * alto;
                        if (ox > a - espacio && ox < a + cajaAncho + espacio && oy > b - espacio && oy < b + cajaAlto + espacio) libre = false;
                    }
                    if (!libre) continue;
                    RotuloPico candidato = new RotuloPico(pico, x, y, a, b, cajaAncho, cajaAlto);
                    double distanciaActual = Math.hypot(candidato.enlaceX - x, candidato.enlaceY - y);
                    if (distanciaActual < distancia) { mejor = candidato; distancia = distanciaActual; }
                }
            }
            if (mejor != null) salida.add(mejor);
        }
        return salida;
    }
}
