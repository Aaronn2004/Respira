package com.nodia.respira.datos;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// ==============================================================
// Zona del mapa: un parque o un barrio.
// Valor de cada magnitud = media de lo medido por los portadores
// que han pasado por la zona en la última hora.
// ==============================================================
public class Zona {

    public static final String PARQUE = "parque";
    public static final String BARRIO = "barrio";

    public final String id;
    public final String tipo;
    public final String nombre;
    public final List<double[]> contorno;   // [lat, lng]
    public final Integer haceMin;           // null = nunca ha pasado nadie
    public final int numMediciones;
    private final Map<Magnitud, Double> valores = new EnumMap<>(Magnitud.class);
    private double[] posEtiqueta;           // dónde va el nombre en el mapa (null = centro)
    private final Map<Magnitud, Double> media24 = new EnumMap<>(Magnitud.class);   // promedio de las últimas 24 h

    public Zona(String id, String tipo, String nombre, List<double[]> contorno,
                Integer haceMin, int numMediciones, Double o3, Double temp, Double ruido) {
        this.id = id;
        this.tipo = tipo;
        this.nombre = nombre;
        this.contorno = contorno;
        this.haceMin = haceMin;
        this.numMediciones = numMediciones;
        valores.put(Magnitud.O3, o3);
        valores.put(Magnitud.TEMP, temp);
        valores.put(Magnitud.RUIDO, ruido);
    }

    public Zona conMedia24h(Magnitud m, Double v) {
        media24.put(m, v);
        return this;
    }

    // promedio de las últimas 24 h (null si no hay o son datos de demostración)
    public Double media24h(Magnitud m) {
        return media24.get(m);
    }

    public boolean esParque() {
        return PARQUE.equals(tipo);
    }

    public Double valor(Magnitud m) {
        return valores.get(m);
    }

    public String estado() {
        return Reglas.estadoZona(haceMin);
    }

    // Nivel que se pinta en el mapa para la magnitud elegida
    public Nivel nivel(Magnitud m) {
        return Reglas.ESTADO_SIN_DATOS.equals(estado()) ? Nivel.NINGUNO : m.nivel(valor(m));
    }

    public Zona conEtiquetaEn(double[] pos) {
        this.posEtiqueta = pos;
        return this;
    }

    public double[] posicionEtiqueta() {
        return posEtiqueta != null ? posEtiqueta : centro();
    }

    public double[] centro() {
        double lat = 0, lng = 0;
        for (double[] p : contorno) {
            lat += p[0];
            lng += p[1];
        }
        return new double[]{lat / contorno.size(), lng / contorno.size()};
    }

    // --------------------------------------------------------------
    // resumen() --> Text   ("Estado general: Moderado · hace 14 min")
    // --------------------------------------------------------------
    public String resumen() {
        if (Reglas.ESTADO_SIN_DATOS.equals(estado())) {
            return "Sin datos en la última hora";
        }
        int suma = 0, n = 0;
        for (Magnitud m : Magnitud.values()) {
            Nivel nv = m.nivel(valor(m));
            if (nv != Nivel.NINGUNO) {
                suma += nv.ordinal() + 1;
                n++;
            }
        }
        if (n == 0) {
            return "Sin datos en la última hora";
        }
        double media = suma / (double) n;
        String g = media < 1.5 ? "Bueno" : media < 2.5 ? "Moderado" : "Malo";
        return "Estado general: " + g + " · " + Reglas.haceTexto(haceMin);
    }

    // Magnitudes en nivel alto ("Ozono · Ruido") o "" si ninguna
    public String magnitudesAltas() {
        List<String> altas = new ArrayList<>();
        if (!Reglas.ESTADO_SIN_DATOS.equals(estado())) {
            for (Magnitud m : Magnitud.values()) {
                if (m.nivel(valor(m)) == Nivel.ALTO) {
                    altas.add(m.nombre);
                }
            }
        }
        return String.join(" · ", altas);
    }
}
