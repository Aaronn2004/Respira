package com.nodia.respira.ble;

import java.util.Locale;

// ==============================================================
// Medicion = (tipo: N, valor: R, contador: N)
// ==============================================================
public class Medicion {

    public static final int TIPO_O3 = 11;
    public static final int TIPO_TEMPERATURA = 12;
    public static final int TIPO_RUIDO = 13;

    // El O3 viaja en el minor como ppm * 1000
    private static final double FACTOR_O3 = 1000.0;

    private final int tipo;
    private final double valor;
    private final int contador;

    // --------------------------------------------------------------
    // tipo: N, valor: R, contador: N --> Medicion()
    // --------------------------------------------------------------
    public Medicion(int tipo, double valor, int contador) {
        this.tipo = tipo;
        this.valor = valor;
        this.contador = contador;
    }

    // --------------------------------------------------------------
    // trama: TramaIBeacon --> desdeTrama() --> Medicion
    // --------------------------------------------------------------
    public static Medicion desdeTrama(TramaIBeacon trama) {
        int tipo = trama.getTipoMedicion();
        double valor = trama.getValorEntero();
        if (tipo == TIPO_O3) {
            valor = valor / FACTOR_O3;
        }
        return new Medicion(tipo, valor, trama.getContador());
    }

    // --------------------------------------------------------------
    // toJSON() --> Text
    // (a mano para que se pueda probar con JUnit sin Android)
    // --------------------------------------------------------------
    public String toJSON() {
        return String.format(Locale.US,
                "{\"tipo\":%d,\"valor\":%s,\"contador\":%d}",
                tipo, Double.toString(valor), contador);
    }

    public int getTipo() { return tipo; }
    public double getValor() { return valor; }
    public int getContador() { return contador; }

    @Override
    public String toString() {
        return toJSON();
    }
}
