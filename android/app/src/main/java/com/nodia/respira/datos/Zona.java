package com.nodia.respira.datos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// # Ausente representa la ausencia lógica (null en el código).

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

        // --------------------------------------------------------------
        // + valor: R | Ausente, estado: Text, momento: Z, origen: Text
        //       -->
        //     Dato() -->
        // --------------------------------------------------------------
        public Dato(Double valor, String estado, long momento, String origen) {
            this.valor = valor;
            this.estado = estado;
            this.momento = momento;
            this.origen = origen;
        }

        // --------------------------------------------------------------
        // + ahora: Z
        //       -->
        //     estadoEn() <--
        //       <--
        //   Text
        // --------------------------------------------------------------
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
    private String nombreCorto;
    private final Map<Magnitud, Double> media24 = new EnumMap<>(Magnitud.class);   // promedio de las últimas 24 h

    // --------------------------------------------------------------
    // + id: Text
    //   tipo: Text
    //   nombre: Text
    //   contorno: [[R]]
    //   hace_min: Z | Ausente
    //   num_mediciones: Z
    //   o3: R | Ausente
    //   temp: R | Ausente
    //   ruido: R | Ausente
    //       -->
    //     Zona() -->
    // --------------------------------------------------------------
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

    // --------------------------------------------------------------
    // + m: Magnitud, v: R | Ausente
    //       -->
    //     conMedia24h() -->
    //       <--
    //   Zona
    // --------------------------------------------------------------
    public Zona conMedia24h(Magnitud m, Double v) {
        media24.put(m, v);
        return this;
    }

    // promedio de las últimas 24 h (null si no hay o son datos de demostración)
    // --------------------------------------------------------------
    // + m: Magnitud
    //       -->
    //     media24h() <--
    //       <--
    //   R | Ausente
    // --------------------------------------------------------------
    public Double media24h(Magnitud m) {
        return media24.get(m);
    }

    // --------------------------------------------------------------
    // +
    //     esParque() <--
    //       <--
    //   B
    // --------------------------------------------------------------
    public boolean esParque() {
        return PARQUE.equals(tipo);
    }

    // --------------------------------------------------------------
    // +
    //     tipoTexto() <--
    //       <--
    //   Text
    // --------------------------------------------------------------
    public String tipoTexto() {
        return esParque() ? "Parque" : PLAYA.equals(tipo) ? "Playa" : "Barrio";
    }

    // --------------------------------------------------------------
    // + poligonos: [[[[R]]]]
    //       -->
    //     conContornos() -->
    //       <--
    //   Zona
    // --------------------------------------------------------------
    public Zona conContornos(List<List<List<double[]>>> poligonos) {
        if (!poligonos.isEmpty()) this.contornos = poligonos;
        return this;
    }

    // --------------------------------------------------------------
    // +
    //     contornos() -->
    //       <--
    //   [[[[R]]]]
    // --------------------------------------------------------------
    public List<List<List<double[]>>> contornos() {
        return contornos;
    }

    // --------------------------------------------------------------
    // + es_simulado: B
    //       -->
    //     conSimulado() -->
    //       <--
    //   Zona
    // --------------------------------------------------------------
    public Zona conSimulado(boolean esSimulado) {
        this.simulado = esSimulado;
        return this;
    }

    // --------------------------------------------------------------
    // + m: Magnitud, dato: Dato
    //       -->
    //     conDato() -->
    //       <--
    //   Zona
    // --------------------------------------------------------------
    public Zona conDato(Magnitud m, Dato dato) {
        detalles.put(m, dato);
        return this;
    }

    // --------------------------------------------------------------
    // + m: Magnitud
    //       -->
    //     dato() <--
    //       <--
    //   Dato
    // --------------------------------------------------------------
    public Dato dato(Magnitud m) {
        Dato d = detalles.get(m);
        if (d != null) return d;
        Double v = valores.get(m);
        return new Dato(v, v == null ? Reglas.ESTADO_SIN_DATOS : estado(), 0,
                v == null ? "" : simulado ? "simulado" : "");
    }

    // --------------------------------------------------------------
    // # effects: consulta el reloj.
    // + m: Magnitud
    //       -->
    //     estado() <--
    //       <--
    //   Text
    // --------------------------------------------------------------
    public String estado(Magnitud m) {
        return dato(m).estadoEn(System.currentTimeMillis());
    }

    // --------------------------------------------------------------
    // # effects: consulta el reloj.
    // + m: Magnitud
    //       -->
    //     valor() <--
    //       <--
    //   R | Ausente
    // --------------------------------------------------------------
    public Double valor(Magnitud m) {
        Dato dato = dato(m);
        return Reglas.ESTADO_SIN_DATOS.equals(dato.estadoEn(System.currentTimeMillis())) ? null : dato.valor;
    }

    // --------------------------------------------------------------
    // +
    //     estado() <--
    //       <--
    //   Text
    // --------------------------------------------------------------
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
    // --------------------------------------------------------------
    // + m: Magnitud
    //       -->
    //     nivel() <--
    //       <--
    //   Nivel
    // --------------------------------------------------------------
    public Nivel nivel(Magnitud m) {
        return m.nivel(valor(m));
    }

    // --------------------------------------------------------------
    // # Misma media de niveles que en la web; una magnitud sin datos no puntúa.
    // # effects: consulta el reloj al comprobar la antigüedad de cada lectura.
    // +
    //     nivelGeneral() <--
    //       <--
    //   Nivel
    // --------------------------------------------------------------
    public Nivel nivelGeneral() {
        int suma = 0, cantidad = 0;
        for (Magnitud m : Magnitud.values()) {
            Nivel nivel = nivel(m);
            if (nivel != Nivel.NINGUNO) {
                suma += nivel.ordinal();
                cantidad++;
            }
        }
        return cantidad == 0 ? Nivel.NINGUNO : Nivel.values()[(int) Math.round(suma / (double) cantidad)];
    }

    // --------------------------------------------------------------
    // + etiqueta: Text
    //       -->
    //     conNombreCorto() -->
    //       <--
    //   Zona
    // --------------------------------------------------------------
    public Zona conNombreCorto(String etiqueta) {
        nombreCorto = etiqueta;
        return this;
    }

    // --------------------------------------------------------------
    // +
    //     nombreCorto() <--
    //       <--
    //   Text
    // --------------------------------------------------------------
    public String nombreCorto() {
        return nombreCorto == null ? nombre : nombreCorto;
    }

    // --------------------------------------------------------------
    // # Cada polígono incluye su contorno exterior y, después, sus huecos.
    // + lat: R, lng: R
    //       -->
    //     contiene() <--
    //       <--
    //   B
    // --------------------------------------------------------------
    public boolean contiene(double lat, double lng) {
        for (List<List<double[]>> poligono : contornos) {
            if (poligono.isEmpty() || !Reglas.dentro(lat, lng, poligono.get(0))) continue;
            boolean enHueco = false;
            for (int i = 1; i < poligono.size(); i++) {
                if (Reglas.dentro(lat, lng, poligono.get(i))) { enHueco = true; break; }
            }
            if (!enHueco) return true;
        }
        return false;
    }

    // --------------------------------------------------------------
    // + pos: [R]
    //       -->
    //     conEtiquetaEn() -->
    //       <--
    //   Zona
    // --------------------------------------------------------------
    public Zona conEtiquetaEn(double[] pos) {
        this.posEtiqueta = pos;
        return this;
    }

    // --------------------------------------------------------------
    // +
    //     posicionEtiqueta() <--
    //       <--
    //   [R]
    // --------------------------------------------------------------
    public double[] posicionEtiqueta() {
        return posEtiqueta != null ? posEtiqueta : centro();
    }

    // --------------------------------------------------------------
    // +
    //     centro() <--
    //       <--
    //   [R]
    // --------------------------------------------------------------
    public double[] centro() {
        double lat = 0, lng = 0;
        for (double[] p : contorno) {
            lat += p[0];
            lng += p[1];
        }
        return new double[]{lat / contorno.size(), lng / contorno.size()};
    }

    // --------------------------------------------------------------
    // +
    //     resumen() <--
    //       <--
    //   Text
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
    // --------------------------------------------------------------
    // +
    //     magnitudesAltas() <--
    //       <--
    //   Text
    // --------------------------------------------------------------
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
