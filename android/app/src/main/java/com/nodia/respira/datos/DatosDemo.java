package com.nodia.respira.datos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// ==============================================================
// DatosDemo: los mismos datos de ejemplo que el prototipo (v17),
// colocados sobre Gandía. Las zonas son APROXIMADAS y ficticias:
// cuando exista la API de zonas, esto se sustituye por la respuesta
// del servidor (GET /zonas).
// ==============================================================
public final class DatosDemo {

    // Caja de Gandía que corresponde al lienzo 390 x 560 del prototipo
    private static final double LAT_NORTE = 38.9815, LAT_SUR = 38.9540;
    private static final double LNG_OESTE = -0.1980, LNG_ESTE = -0.1700;

    public static final double LAT_CENTRO = 38.9677;
    public static final double LNG_CENTRO = -0.1840;

    private DatosDemo() { }

    // x, y del prototipo --> [lat, lng]
    public static double[] geo(double x, double y) {
        return new double[]{
                LAT_NORTE - y / 560.0 * (LAT_NORTE - LAT_SUR),
                LNG_OESTE + x / 390.0 * (LNG_ESTE - LNG_OESTE)};
    }

    // Contorno orgánico (parque) alrededor de un punto del prototipo
    private static List<double[]> mancha(double x, double y, double rx, double ry, int semilla) {
        List<double[]> p = new ArrayList<>();
        int n = 14;
        for (int i = 0; i < n; i++) {
            double t = i * 2 * Math.PI / n;
            double r = 1 + 0.14 * Math.sin(3 * t + semilla) + 0.07 * Math.cos(5 * t + semilla * 2);
            p.add(geo(x + Math.cos(t) * rx * r, y + Math.sin(t) * ry * r));
        }
        return p;
    }

    private static List<double[]> rect(double x, double y, double w, double h) {
        return new ArrayList<>(Arrays.asList(geo(x, y), geo(x + w, y), geo(x + w, y + h), geo(x, y + h)));
    }

    public static List<Zona> zonas() {
        List<Zona> z = new ArrayList<>();
        // Barrios primero: se dibujan debajo de los parques
        z.add(new Zona("norte", Zona.BARRIO, "Barrio Norte", rect(0, 0, 390, 196), 4, 210, 0.058, 25.2, 59.0)
                .conEtiquetaEn(geo(195, 90)));
        z.add(new Zona("centro", Zona.BARRIO, "Centro", rect(0, 262, 390, 110), 2, 388, 0.066, 26.1, 63.0)
                .conEtiquetaEn(geo(300, 350)));
        z.add(new Zona("sur", Zona.BARRIO, "Barrio Sur", rect(0, 384, 390, 176), 11, 96, 0.049, 24.0, 55.0)
                .conEtiquetaEn(geo(195, 470)));
        z.add(new Zona("ribera", Zona.PARQUE, "Parque de la Ribera", mancha(92, 128, 52, 40, 1), 6, 84, 0.041, 26.8, 49.0));
        z.add(new Zona("molino", Zona.PARQUE, "Jardín del Molino", mancha(298, 108, 46, 38, 2), 14, 31, 0.072, 24.1, 57.0));
        z.add(new Zona("central", Zona.PARQUE, "Parque Central", mancha(208, 306, 58, 34, 3), 3, 152, 0.097, 27.9, 67.0));
        z.add(new Zona("estacion", Zona.PARQUE, "Jardines de la Estación", mancha(88, 430, 40, 28, 4), 190, 0, 0.055, 22.0, 61.0));
        z.add(new Zona("oeste", Zona.PARQUE, "Parque del Oeste", mancha(302, 445, 40, 30, 5), null, 0, null, null, null));
        return z;
    }

    public static List<Percepcion> percepciones(long ahora) {
        Object[][] a = {
                {"ruido", 70, 252, 3, 20, "Av. del Río, 40"},
                {"ruido", 262, 206, 2, 120, "C/ del Molino, 18"},
                {"ruido", 150, 356, 1, 300, "C/ Mayor, 61"},
                {"ruido", 322, 330, 3, 12, "Pl. Mayor, 4"},
                {"temp", 120, 110, 3, 35, "Av. del Río, 12"},
                {"temp", 250, 300, 2, 80, "Pl. Mayor, 1"},
                {"aire", 300, 150, 2, 50, "C/ del Molino, 5"},
                {"aire", 60, 420, 1, 200, "Pl. de la Estación, 3"},
                {"temp", 300, 450, 1, 420, "C/ Poniente, 38"}};
        List<Percepcion> lista = new ArrayList<>();
        for (int i = 0; i < a.length; i++) {
            double[] g = geo((Integer) a[i][1], (Integer) a[i][2]);
            lista.add(new Percepcion(i + 1, (String) a[i][0], (Integer) a[i][3], g[0], g[1],
                    ahora - (Integer) a[i][4] * 60_000L, (String) a[i][5], null));
        }
        // Publicada por Lucía (U-48213): con su sesión se puede editar y borrar
        double[] g = geo(205, 170);
        lista.add(new Percepcion(a.length + 1, "ruido", 2, g[0], g[1], ahora - 40 * 60_000L, "C/ Mayor, 43", "U-48213"));
        return lista;
    }

    // --------------------------------------------------------------
    // Serie para la gráfica de evolución (misma fórmula que el prototipo).
    // per: "24h" (48 puntos, cada 30 min) | "7d" (56 puntos, cada 3 h)
    // null = no pasó ningún sensor (no se rellena).
    // --------------------------------------------------------------
    public static Double[] serie(Zona z, Magnitud m, String per) {
        boolean dia = "24h".equals(per);
        int n = dia ? 48 : 56;
        final long[] semilla = {7};
        for (char c : (z.id + m.id + per).toCharArray()) {
            semilla[0] += c;
        }
        Double actual = z.valor(m);
        double base = actual != null ? actual : (m == Magnitud.O3 ? 0.05 : m == Magnitud.TEMP ? 22 : 58);
        Double[] out = new Double[n];
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double ciclo = dia ? Math.sin((t - 0.3) * Math.PI * 2) : Math.sin(t * Math.PI * 2 * 7);
            double r = rnd(semilla);
            Double v;
            if (m == Magnitud.O3) {
                v = base * (0.75 + 0.35 * ciclo + 0.12 * (r - 0.5));
            } else if (m == Magnitud.TEMP) {
                v = base + 3 * ciclo + (r - 0.5);
            } else {
                v = base + 6 * ciclo + 3 * (r - 0.5);
            }
            if (dia && i >= 4 && i <= 13) {
                v = null;
            }
            if (!dia && rnd(semilla) < 0.08) {
                v = null;
            }
            if (z.haceMin == null && i > n - 4) {
                v = null;
            }
            out[i] = v;
        }
        if (actual != null) {
            out[n - 1] = actual;
        }
        return out;
    }

    private static double rnd(long[] semilla) {
        semilla[0] = (semilla[0] * 9301 + 49297) % 233280;
        return semilla[0] / 233280.0;
    }
}
