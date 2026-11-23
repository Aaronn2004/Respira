package com.nodia.respira.ui;

import com.nodia.respira.datos.Magnitud;
import com.nodia.respira.datos.Serie;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

// Geometría de presentación, sin Android ni acceso a red. No recalcula eventos ni medias.
public final class GraficaHistorica {
    public static final ZoneId ZONA = ZoneId.of("Europe/Madrid");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM", new Locale("es", "ES"));
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", new Locale("es", "ES"));

    private GraficaHistorica() { }

    public static final class Alto {
        public final Serie.Evento evento;
        public final long desde;
        public final long hasta;
        public final double inicio;
        public final double fin;
        public final double centroEtiqueta;
        public final int fila;

        private Alto(Serie serie, Serie.Evento evento, long desde, long hasta,
                     double centroEtiqueta, int fila) {
            this.evento = evento;
            this.desde = desde;
            this.hasta = hasta;
            inicio = serie.posicionTemporal(desde);
            fin = serie.posicionTemporal(hasta);
            this.centroEtiqueta = centroEtiqueta;
            this.fila = fila;
        }
    }

    // Recorta solo la representación: no cambia los eventos ni detecta cruces de la curva.
    // Cada etiqueta ocupa una fila libre si coincide horizontalmente con otra.
    public static List<Alto> altos(Serie serie, double ancho, double anchoEtiqueta) {
        List<Alto> salida = new ArrayList<>();
        if (!serie.tieneIntervalo() || anchoEtiqueta <= 0 || ancho < anchoEtiqueta) return salida;
        List<Serie.Evento> eventos = new ArrayList<>(serie.alertas);
        eventos.sort(Comparator.comparingLong(e -> e.desde));
        List<Double> finales = new ArrayList<>();
        for (Serie.Evento evento : eventos) {
            if (!"alto".equals(evento.nivel)) continue;
            long desde = Math.max(serie.desde, evento.desde);
            long hasta = Math.min(serie.hasta, evento.hasta);
            if (desde >= hasta) continue;
            double centro = (serie.posicionTemporal(desde) + serie.posicionTemporal(hasta)) * ancho / 2;
            double izquierda = Math.max(0, Math.min(ancho - anchoEtiqueta, centro - anchoEtiqueta / 2));
            int fila = 0;
            while (fila < finales.size() && izquierda < finales.get(fila)) fila++;
            if (fila == finales.size()) finales.add(0.0);
            finales.set(fila, izquierda + anchoEtiqueta);
            salida.add(new Alto(serie, evento, desde, hasta, (izquierda + anchoEtiqueta / 2) / ancho, fila));
        }
        return salida;
    }

    public static int filasAltos(List<Alto> altos) {
        int filas = 0;
        for (Alto alto : altos) filas = Math.max(filas, alto.fila + 1);
        return filas;
    }

    public static final class Pico {
        public final String etiqueta;
        public final Serie.Evento evento;
        public final double posicion;

        private Pico(int numero, Serie.Evento evento, double posicion) {
            this.etiqueta = "P" + numero;
            this.evento = evento;
            this.posicion = posicion;
        }
    }

    public static List<Pico> picos(Serie serie) {
        List<Pico> salida = new ArrayList<>();
        for (int i = 0; i < Math.min(3, serie.picos.size()); i++) {
            Serie.Evento evento = serie.picos.get(i);
            if (!Double.isFinite(evento.valor)) continue;
            if (serie.tieneIntervalo() && (evento.desde < serie.desde || evento.desde > serie.hasta)) continue;
            salida.add(new Pico(i + 1, evento,
                    serie.tieneIntervalo() ? serie.posicionTemporal(evento.desde) : Double.NaN));
        }
        return salida;
    }

    // Rótulos separados en columnas, ordenados por tiempo para reducir cruces de las guías.
    // Los puntos conservan sus coordenadas incluso cuando coinciden exactamente.
    public static List<Pico> picosPorTiempo(Serie serie) {
        List<Pico> salida = picos(serie);
        salida.sort(Comparator.comparingDouble(p -> p.posicion));
        return salida;
    }

    public static double[] limites(Serie serie, Magnitud magnitud) {
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (Double valor : serie.valores) {
            if (valor == null || !Double.isFinite(valor)) continue;
            min = Math.min(min, valor);
            max = Math.max(max, valor);
        }
        if (!Double.isFinite(min)) return null;
        List<Pico> picos = serie.tieneIntervalo() ? picos(serie) : new ArrayList<>();
        for (Pico pico : picos) {
            min = Math.min(min, pico.evento.valor);
            max = Math.max(max, pico.evento.valor);
        }
        if (magnitud == Magnitud.TEMP) {
            min -= 2;
            max = Math.max(max, magnitud.limiteMedio) + 2;
        } else {
            min = Math.min(min, magnitud.limiteMedio * 0.6);
            max = Math.max(max, magnitud.limiteAlto * 1.08);
        }
        if (!picos.isEmpty()) {
            double margen = Math.max((max - min) * 0.06, 0.001);
            min -= margen;
            max += margen;
        }
        return new double[]{min, max};
    }

    public static final class Marca {
        public final long instante;
        public final double posicion;
        public final String fecha;
        public final String hora;

        private Marca(Serie serie, ZonedDateTime t, boolean repetirHora) {
            instante = t.toInstant().toEpochMilli();
            posicion = serie.posicionTemporal(instante);
            fecha = FECHA.format(t);
            hora = HORA.format(t) + (repetirHora ? " " + t.getOffset().getId().substring(0, 3) : "");
        }
    }

    // El ancho reservado incluye el espacio entre etiquetas. Nunca se redondean los datos.
    public static List<Marca> marcas(Serie serie, double ancho, double anchoEtiqueta) {
        List<Marca> vacio = new ArrayList<>();
        if (!serie.tieneIntervalo() || anchoEtiqueta <= 0 || ancho < anchoEtiqueta) return vacio;
        boolean diario = serie.hasta - serie.desde <= 2 * 86400000L;
        int[] pasos = diario ? new int[]{1, 2, 3, 6, 12, 24} : new int[]{1, 2, 3, 7, 14, 30};
        for (int paso : pasos) {
            List<ZonedDateTime> tiempos = new ArrayList<>();
            ZonedDateTime inicio = Instant.ofEpochMilli(serie.desde).atZone(ZONA);
            if (diario) {
                // Avanzar por instantes conserva las dos horas repetidas al terminar el horario de verano.
                for (ZonedDateTime t = inicio.truncatedTo(ChronoUnit.HOURS);
                        t.toInstant().toEpochMilli() <= serie.hasta; t = t.plusHours(1)) {
                    if (t.toInstant().toEpochMilli() >= serie.desde && t.getHour() % paso == 0) tiempos.add(t);
                }
            } else {
                LocalDate fin = Instant.ofEpochMilli(serie.hasta).atZone(ZONA).toLocalDate();
                for (LocalDate d = inicio.toLocalDate(); !d.isAfter(fin); d = d.plusDays(1)) {
                    ZonedDateTime t = d.atStartOfDay(ZONA);
                    long ms = t.toInstant().toEpochMilli();
                    if (ms >= serie.desde && ms <= serie.hasta && Math.floorMod(d.toEpochDay(), (long) paso) == 0) tiempos.add(t);
                }
            }
            List<Marca> resultado = new ArrayList<>();
            double derechaAnterior = -Double.MAX_VALUE;
            boolean caben = true;
            for (ZonedDateTime t : tiempos) {
                boolean repetida = tiempos.stream().anyMatch(otra -> !otra.equals(t)
                        && otra.toLocalDateTime().equals(t.toLocalDateTime()));
                Marca marca = new Marca(serie, t, repetida);
                double izquierda = Math.max(0, Math.min(ancho - anchoEtiqueta,
                        marca.posicion * ancho - anchoEtiqueta / 2));
                if (izquierda < derechaAnterior) caben = false;
                derechaAnterior = izquierda + anchoEtiqueta;
                resultado.add(marca);
            }
            if (caben && !resultado.isEmpty()) return resultado;
        }
        return vacio;
    }
}
