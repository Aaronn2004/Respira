package com.nodia.respira.ble;

import java.util.Locale;

// # Medición decodificada: los métodos con... completan la misma instancia antes del envío.
// # Ausente representa la ausencia lógica (null en el código).

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
    // + nodo: Text, magnitud: Text, valor: R, contador: Z, medida_en: Z
    //       -->
    //     Medicion() -->
    // --------------------------------------------------------------
    public Medicion(String nodo, String magnitud, double valor, int contador, long medidaEn) {
        this.nodo = nodo;
        this.magnitud = magnitud;
        this.valor = valor;
        this.contador = contador;
        this.medidaEn = medidaEn;
    }

    // --------------------------------------------------------------
    // # Es el UUID que anuncia el Arduino (Publicador.h)
    // + codigo: Text
    //       -->
    //     uuidDelSensor() --x
    //       <--
    //   Text
    // --------------------------------------------------------------
    public static String uuidDelSensor(String codigo) {
        return "RESPIRA-" + codigo;
    }

    // --------------------------------------------------------------
    // + tipo: Z
    //       -->
    //     magnitudDeTipo() --x
    //       <--
    //   Text | Ausente
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
    // # Ozono usa milésimas de ppm; temperatura y ruido, décimas de su unidad.
    // + trama: TramaIBeacon, nodo: Text, ahora: Z
    //       -->
    //     desdeTrama() --x
    //       <--
    //   Medicion | Ausente
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

    // --------------------------------------------------------------
    // + lat: R | Ausente, lng: R | Ausente, precision: Z | Ausente
    //       -->
    //     conUbicacion() -->
    //       <--
    //   Medicion
    // --------------------------------------------------------------
    public Medicion conUbicacion(Double lat, Double lng, Integer precision) {
        this.lat = lat;
        this.lng = lng;
        this.precision = precision;
        return this;
    }

    // --------------------------------------------------------------
    // + rssi: Z
    //       -->
    //     conRssi() -->
    //       <--
    //   Medicion
    // --------------------------------------------------------------
    public Medicion conRssi(int rssi) {
        this.rssi = rssi;
        return this;
    }

    // --------------------------------------------------------------
    // + portador: Text
    //       -->
    //     conPortador() -->
    //       <--
    //   Medicion
    // --------------------------------------------------------------
    public Medicion conPortador(String portador) {
        this.portador = portador;
        return this;
    }

    // --------------------------------------------------------------
    // +
    //     toJSON() <--
    //       <--
    //   Text
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

    // --------------------------------------------------------------
    // +
    //     getNodo() <--
    //       <--
    //   Text
    // --------------------------------------------------------------
    public String getNodo() { return nodo; }
    // --------------------------------------------------------------
    // +
    //     getMagnitud() <--
    //       <--
    //   Text
    // --------------------------------------------------------------
    public String getMagnitud() { return magnitud; }
    // --------------------------------------------------------------
    // +
    //     getValor() <--
    //       <--
    //   R
    // --------------------------------------------------------------
    public double getValor() { return valor; }
    // --------------------------------------------------------------
    // +
    //     getContador() <--
    //       <--
    //   Z
    // --------------------------------------------------------------
    public int getContador() { return contador; }
    // --------------------------------------------------------------
    // +
    //     getMedidaEn() <--
    //       <--
    //   Z
    // --------------------------------------------------------------
    public long getMedidaEn() { return medidaEn; }

    // --------------------------------------------------------------
    // +
    //     toString() <--
    //       <--
    //   Text
    // --------------------------------------------------------------
    @Override
    public String toString() {
        return toJSON();
    }
}
