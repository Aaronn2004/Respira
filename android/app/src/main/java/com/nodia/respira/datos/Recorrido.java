package com.nodia.respira.datos;

// # Estado del recorrido y del envío de mediciones del sensor que lleva esta cuenta.

// Recorrido con sensor de hoy (uno a la vez).
public class Recorrido {

    public static final String BUSCANDO = "buscando";
    public static final String ACTIVO = "activo";
    public static final String BLUETOOTH = "bluetooth";
    public static final String UBICACION = "ubicacion";
    public static final String LOCALIZANDO = "localizando";
    public static final String SIN_ALCANCE = "alcance";
    public static final String PAUSA = "pausa";
    public static final String TERMINADO = "fin";

    public final String codigo;
    public final String inicio;      // "10:42"
    public String estado;
    public int enviadas;
    public int pendientes;
    public int rechazadas;
    public String ultima;            // hora de la última medición
    public long ultimaMs;

    // --------------------------------------------------------------
    // + codigo: Text, inicio: Text
    //       -->
    //     Recorrido() -->
    // --------------------------------------------------------------
    public Recorrido(String codigo, String inicio) {
        this.codigo = codigo;
        this.inicio = inicio;
        this.estado = BUSCANDO;
    }

    // --------------------------------------------------------------
    // +
    //     enCurso() <--
    //       <--
    //   B
    // --------------------------------------------------------------
    public boolean enCurso() {
        return !TERMINADO.equals(estado);
    }
}
