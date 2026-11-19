package com.nodia.respira.datos;

import java.util.List;

// # Ausente representa la ausencia lógica (null en el código).

// ==============================================================
// DatosDemo: series de demostración y acceso al catálogo costero real.
// Las zonas sin lecturas de la API conservan sus contornos y no inventan valores.
// ==============================================================
public final class DatosDemo {

    // Ámbito del Grau i Platja (el mismo que usa la web).
    private static final double LAT_NORTE = 39.026, LAT_SUR = 38.981;
    private static final double LNG_OESTE = -0.187, LNG_ESTE = -0.139;

    public static final double LAT_CENTRO = 39.002;
    public static final double LNG_CENTRO = -0.165;

    // --------------------------------------------------------------
    // -
    //     DatosDemo() -->
    // --------------------------------------------------------------
    private DatosDemo() { }

    // x, y del prototipo --> [lat, lng]
    // --------------------------------------------------------------
    // + x: R, y: R
    //       -->
    //     geo() --x
    //       <--
    //   [R]
    // --------------------------------------------------------------
    public static double[] geo(double x, double y) {
        return new double[]{
                LAT_NORTE - y / 560.0 * (LAT_NORTE - LAT_SUR),
                LNG_OESTE + x / 390.0 * (LNG_ESTE - LNG_OESTE)};
    }

    // --------------------------------------------------------------
    // +
    //     zonas() --x
    //       <--
    //   [Zona]
    // --------------------------------------------------------------
    public static List<Zona> zonas() {
        return ZonasCosta.zonas();
    }

    // --------------------------------------------------------------
    // # Serie para la gráfica de evolución (misma fórmula que el prototipo).
    // # per: "24h" (48 puntos, cada 30 min) | "7d" (56 puntos, cada 3 h)
    // # null = no pasó ningún sensor (no se rellena).
    // + z: Zona, m: Magnitud, per: Text
    //       -->
    //     serie() --x
    //       <--
    //   [R | Ausente]
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

    // --------------------------------------------------------------
    // # La semilla se actualiza para mantener una secuencia reproducible.
    // # effects: actualiza la semilla recibida.
    // - semilla: [Z]
    //       -->
    //     rnd() --x
    //       <--
    //   R, semilla: [Z]
    // --------------------------------------------------------------
    private static double rnd(long[] semilla) {
        semilla[0] = (semilla[0] * 9301 + 49297) % 233280;
        return semilla[0] / 233280.0;
    }
}
