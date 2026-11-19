package com.nodia.respira.datos;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// # Ausente representa la ausencia lógica (null en el código).

// ==============================================================
// Reglas: lógica pura (sin Android) para poder probarla con JUnit.
// ==============================================================
public final class Reglas {

    public static final String ESTADO_OK = "ok";
    public static final String ESTADO_ANTIGUO = "antiguo";
    public static final String ESTADO_SIN_DATOS = "sin_datos";

    public static final int MAX_PUNTOS_DIA = 100;
    public static final int MIN_POR_PUNTO = 5;
    public static final int MAX_INTENTOS_LOGIN = 5;
    public static final long BLOQUEO_LOGIN_MS = 15 * 60 * 1000L;

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    private static final Pattern USUARIO = Pattern.compile("^[A-Za-z0-9._]{3,20}$");
    private static final Pattern CODIGO_SENSOR = Pattern.compile("^RSP-[A-Z0-9]{4}$");

    // --------------------------------------------------------------
    // -
    //     Reglas() -->
    // --------------------------------------------------------------
    private Reglas() { }

    // --------------------------------------------------------------
    // # null o más de 24 h -> sin datos; más de 1 h -> antiguo
    // + minutos_desde_ultima: Z | Ausente
    //       -->
    //     estadoZona() --x
    //       <--
    //   Text
    // --------------------------------------------------------------
    public static String estadoZona(Integer minutosDesdeUltima) {
        if (minutosDesdeUltima == null || minutosDesdeUltima > 24 * 60) {
            return ESTADO_SIN_DATOS;
        }
        return minutosDesdeUltima > 60 ? ESTADO_ANTIGUO : ESTADO_OK;
    }

    // --------------------------------------------------------------
    // # Los minutos en parques aportan un segundo bloque de puntos; se aplica el máximo diario.
    // + minutos: Z, minutos_en_parques: Z
    //       -->
    //     puntosDelDia() --x
    //       <--
    //   Z
    // --------------------------------------------------------------
    public static int puntosDelDia(int minutos, int minutosEnParques) {
        int puntos = minutos / MIN_POR_PUNTO + minutosEnParques / MIN_POR_PUNTO;
        return Math.min(MAX_PUNTOS_DIA, Math.max(0, puntos));
    }

    // --------------------------------------------------------------
    // + texto: Text
    //       -->
    //     esEmail() --x
    //       <--
    //   B
    // --------------------------------------------------------------
    public static boolean esEmail(String texto) {
        return texto != null && texto.trim().length() <= 120 && EMAIL.matcher(texto.trim()).matches();
    }

    // --------------------------------------------------------------
    // + texto: Text
    //       -->
    //     esNombreUsuario() --x
    //       <--
    //   B
    // --------------------------------------------------------------
    public static boolean esNombreUsuario(String texto) {
        return texto != null && USUARIO.matcher(texto.trim()).matches();
    }

    // --------------------------------------------------------------
    // + texto: Text
    //       -->
    //     esCodigoSensor() --x
    //       <--
    //   B
    // --------------------------------------------------------------
    public static boolean esCodigoSensor(String texto) {
        return texto != null && CODIGO_SENSOR.matcher(texto.trim().toUpperCase(Locale.ROOT)).matches();
    }

    // --------------------------------------------------------------
    // + texto: Text, m: Magnitud
    //       -->
    //     errorValorAlerta() --x
    //       <--
    //   Text | Ausente
    // --------------------------------------------------------------
    public static String errorValorAlerta(String texto, Magnitud m) {
        String limpio = texto == null ? "" : texto.trim().replace(',', '.');
        if (limpio.isEmpty()) {
            return "Escribe el valor límite.";
        }
        double v;
        try {
            v = Double.parseDouble(limpio);
        } catch (NumberFormatException e) {
            return "Escribe solo un número, por ejemplo " + (m == Magnitud.O3 ? "0.080" : "30") + ".";
        }
        if (!Double.isFinite(v) || v < m.rangoMin || v > m.rangoMax) {
            return "Fuera de rango: debe estar entre " + m.formatear(m.rangoMin) + " y "
                    + m.formatear(m.rangoMax) + " " + m.unidad + ".";
        }
        return null;
    }

    // --------------------------------------------------------------
    // + minutos: Z | Ausente
    //       -->
    //     haceTexto() --x
    //       <--
    //   Text
    // --------------------------------------------------------------
    public static String haceTexto(Integer minutos) {
        if (minutos == null) {
            return "sin mediciones";
        }
        if (minutos < 60) {
            return "hace " + minutos + " min";
        }
        return "hace " + Math.round(minutos / 60.0) + " h";
    }

    // --------------------------------------------------------------
    // # Cruce de rayos sobre el polígono en el orden [latitud, longitud].
    // + lat: R, lng: R, contorno: [[R]]
    //       -->
    //     dentro() --x
    //       <--
    //   B
    // --------------------------------------------------------------
    public static boolean dentro(double lat, double lng, List<double[]> contorno) {
        boolean dentro = false;
        for (int i = 0, j = contorno.size() - 1; i < contorno.size(); j = i++) {
            double[] a = contorno.get(i);
            double[] b = contorno.get(j);
            boolean cruza = (a[1] > lng) != (b[1] > lng)
                    && lat < (b[0] - a[0]) * (lng - a[1]) / (b[1] - a[1]) + a[0];
            if (cruza) {
                dentro = !dentro;
            }
        }
        return dentro;
    }
}
