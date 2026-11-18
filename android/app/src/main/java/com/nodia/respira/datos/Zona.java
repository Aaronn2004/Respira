package com.nodia.respira.datos;

import java.util.ArrayList;
import java.util.Collections;
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
    public static final String PLAYA = "playa";

    public static final class Dato {
        public final Double valor;
        public final String estado;
        public final long momento;
        public final String origen;

        public Dato(Double valor, String estado, long momento, String origen) {
            this.valor = valor;
            this.estado = estado;
            this.momento = momento;
            this.origen = origen;
        }

        public String estadoEn(long ahora) {
            if (valor == null) return Reglas.ESTADO_SIN_DATOS;
            if (momento <= 0 || "estimado".equals(estado)) return estado;
            long edad = ahora - momento;
            if (edad > 24 * 3_600_000L) return Reglas.ESTADO_SIN_DATOS;
            return edad > 3_600_000L ? Reglas.ESTADO_ANTIGUO : estado;
        }
    }

    public final String id;
    public final String tipo;
    public final String nombre;
    public final List<double[]> contorno;   // [lat, lng]
    public final Integer haceMin;           // null = nunca ha pasado nadie
    public final int numMediciones;
    private final Map<Magnitud, Dato> detalles = new EnumMap<>(Magnitud.class);
    private List<List<List<double[]>>> contornos;
    private boolean simulado;
    private final Map<Magnitud, Double> valores = new EnumMap<>(Magnitud.class);
    private double[] posEtiqueta;           // dónde va el nombre en el mapa (null = centro)
    private final Map<Magnitud, Double> media24 = new EnumMap<>(Magnitud.class);   // promedio de las últimas 24 h

    public Zona(String id, String tipo, String nombre, List<double[]> contorno,
                Integer haceMin, int numMediciones, Double o3, Double temp, Double ruido) {
        this.id = id;
        this.tipo = tipo;
        this.nombre = nombre;
        this.contorno = contorno;
        this.contornos = Collections.singletonList(Collections.singletonList(contorno));
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

    public String tipoTexto() {
        return esParque() ? "Parque" : PLAYA.equals(tipo) ? "Playa" : "Barrio";
    }

    public Zona conContornos(List<List<List<double[]>>> poligonos) {
        if (!poligonos.isEmpty()) this.contornos = poligonos;
        return this;
    }

    public List<List<List<double[]>>> contornos() {
        return contornos;
    }

    public Zona conSimulado(boolean esSimulado) {
        this.simulado = esSimulado;
        return this;
    }

    public Zona conDato(Magnitud m, Dato dato) {
        detalles.put(m, dato);
        return this;
    }

    public Dato dato(Magnitud m) {
        Dato d = detalles.get(m);
        if (d != null) return d;
        Double v = valores.get(m);
        return new Dato(v, v == null ? Reglas.ESTADO_SIN_DATOS : estado(), 0,
                v == null ? "" : simulado ? "simulado" : "");
    }

    public String estado(Magnitud m) {
        return dato(m).estadoEn(System.currentTimeMillis());
    }

    public Double valor(Magnitud m) {
        Dato dato = dato(m);
        return Reglas.ESTADO_SIN_DATOS.equals(dato.estadoEn(System.currentTimeMillis())) ? null : dato.valor;
    }

    public String estado() {
        if (detalles.isEmpty()) return Reglas.estadoZona(haceMin);
        String estado = Reglas.ESTADO_SIN_DATOS;
        for (Magnitud m : detalles.keySet()) {
            String actual = estado(m);
            if (Reglas.ESTADO_OK.equals(actual)) return actual;
            if (Reglas.ESTADO_ANTIGUO.equals(actual) || (Reglas.ESTADO_SIN_DATOS.equals(estado)
                    && "estimado".equals(actual))) estado = actual;
        }
        return estado;
    }

    // Nivel que se pinta en el mapa para la magnitud elegida
    public Nivel nivel(Magnitud m) {
        return m.nivel(valor(m));
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
        List<String> estados = new ArrayList<>();
        for (Magnitud m : Magnitud.values()) {
            String estado = estado(m);
            String desc = Reglas.ESTADO_ANTIGUO.equals(estado) ? "antiguo"
                    : Reglas.ESTADO_SIN_DATOS.equals(estado) ? "sin datos"
                    : "estimado".equals(estado) ? "estimado"
                    : "simulado".equals(dato(m).origen) ? "simulado" : "";
            if (!desc.isEmpty()) estados.add(m.nombre + " " + desc);
        }
        return "Estado general: " + g + (estados.isEmpty() ? "" : " · " + String.join(", ", estados));
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
