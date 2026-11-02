package com.nodia.respira.datos;

// Línea del historial de puntos: un día con sensor o un canje.
public class Movimiento {

    public final String fecha;
    public final int minutos;
    public final int minutosParque;
    public final int descartados;
    public final int puntos;       // negativo si es un canje
    public final String canje;     // null si es un día con sensor

    public Movimiento(String fecha, int minutos, int minutosParque, int descartados, int puntos, String canje) {
        this.fecha = fecha;
        this.minutos = minutos;
        this.minutosParque = minutosParque;
        this.descartados = descartados;
        this.puntos = puntos;
        this.canje = canje;
    }
}
