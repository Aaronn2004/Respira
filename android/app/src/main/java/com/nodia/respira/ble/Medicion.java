package com.nodia.respira.ble;

import java.util.Locale;

// ==============================================================
// Medicion: un dato de un sensor, tal como se envía al servidor
// (POST /mediciones de la API de Respira).
//
//   nodo, magnitud ("o3" | "temp" | "ruido"), valor, contador,
//   medidaEn (ms), lat, lng, precision (m), rssi, portador (ID)
//
// Del iBeacon (ver Arduino/Conversiones.h):
//   tipo 11 = O3     minor = ppm x 1000
//   tipo 12 = temp   minor = décimas de °C (CON signo)
//   tipo 13 = ruido  minor = décimas de dB
// ==============================================================
public class Medicion {

    public static final int TIPO_O3 = 11;
    public static final int TIPO_TEMPERATURA = 12;
    public static final int TIPO_RUIDO = 13;

    private final String nodo;
    private final String magnitud;
    private final double valor;
    private final int contador;
    private final long medidaEn;
    private Double lat, lng;
    private Integer precision;
    private Integer rssi;
    private String portador;

    // --------------------------------------------------------------
    // nodo: Text, magnitud: Text, valor: R, contador: N, medidaEn: N --> Medicion()
    // --------------------------------------------------------------
    public Medicion(String nodo, String magnitud, double valor, int contador, long medidaEn) {
        this.nodo = nodo;
        this.magnitud = magnitud;
        this.valor = valor;
        this.contador = contador;
        this.medidaEn = medidaEn;
    }

    // --------------------------------------------------------------
    // codigo: Text --> uuidDelSensor() --> Text   ("RSP-4F7K" -> "RESPIRA-RSP-4F7K")
    // Es el UUID que anuncia el Arduino (Publicador.h)
    // --------------------------------------------------------------
    public static String uuidDelSensor(String codigo) {
        return "RESPIRA-" + codigo;
    }

    // --------------------------------------------------------------
    // tipo: N --> magnitudDeTipo() --> Text | null
    // --------------------------------------------------------------
    public static String magnitudDeTipo(int tipo) {
        switch (tipo) {
            case TIPO_O3: return "o3";
            case TIPO_TEMPERATURA: return "temp";
            case TIPO_RUIDO: return "ruido";
            default: return null;
        }
    }

    // --------------------------------------------------------------
    // trama: TramaIBeacon, nodo: Text, ahora: N --> desdeTrama() --> Medicion | null
    // null si el tipo no es de Respira
    // --------------------------------------------------------------
    public static Medicion desdeTrama(TramaIBeacon trama, String nodo, long ahora) {
        int tipo = trama.getTipoMedicion();
        String magnitud = magnitudDeTipo(tipo);
        if (magnitud == null) {
            return null;
        }
        double valor;
        if (tipo == TIPO_O3) {
            valor = trama.getValorEntero() / 1000.0;
        } else {
            valor = trama.getValorConSigno() / 10.0;
        }
        return new Medicion(nodo, magnitud, valor, trama.getContador(), ahora);
    }

    public Medicion conUbicacion(Double lat, Double lng, Integer precision) {
        this.lat = lat;
        this.lng = lng;
        this.precision = precision;
        return this;
    }

    public Medicion conRssi(int rssi) {
        this.rssi = rssi;
        return this;
    }

    public Medicion conPortador(String portador) {
        this.portador = portador;
        return this;
    }

    // --------------------------------------------------------------
    // toJSON() --> Text   (a mano: así se prueba con JUnit sin Android)
    // --------------------------------------------------------------
    public String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US, "{\"nodo\":\"%s\",\"magnitud\":\"%s\",\"valor\":%s,\"contador\":%d,\"medida_en\":%d",
                nodo, magnitud, Double.toString(valor), contador, medidaEn));
        if (lat != null && lng != null) {
            sb.append(String.format(Locale.US, ",\"lat\":%.6f,\"lng\":%.6f", lat, lng));
            if (precision != null) {
                sb.append(",\"precision\":").append(precision);
            }
        }
        if (rssi != null) {
            sb.append(",\"rssi\":").append(rssi);
        }
        if (portador != null) {
            sb.append(",\"portador\":\"").append(portador.replace("\"", "")).append('"');
        }
        return sb.append('}').toString();
    }

    public String getNodo() { return nodo; }
    public String getMagnitud() { return magnitud; }
    public double getValor() { return valor; }
    public int getContador() { return contador; }
    public long getMedidaEn() { return medidaEn; }

    @Override
    public String toString() {
        return toJSON();
    }
}
