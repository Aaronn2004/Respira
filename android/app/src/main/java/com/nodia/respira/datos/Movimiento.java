package com.nodia.respira.datos;

// Línea del historial de puntos: un día con sensor o un canje.
// Los calcula el servidor (Puntos.php) con las mediciones del día.
public class Movimiento {

    public final String fecha;
    public final int minutos;
    public final int minutosParque;
    public final int descartados;
    public final int puntos;       // negativo si es un canje
    public final String canje;     // null si es un día con sensor
    public final String nota;      // «En revisión», «Anulado: …», «Caducado…» | null
    public final ResumenRecorrido recorrido;  // estadísticas del día con sensor | null (canjes)

    public Movimiento(String fecha, int minutos, int minutosParque, int descartados, int puntos, String canje) {
        this(fecha, minutos, minutosParque, descartados, puntos, canje, null);
    }

    public Movimiento(String fecha, int minutos, int minutosParque, int descartados, int puntos, String canje, String nota) {
        this(fecha, minutos, minutosParque, descartados, puntos, canje, nota, null);
    }

    public Movimiento(String fecha, int minutos, int minutosParque, int descartados, int puntos, String canje, String nota,
                      ResumenRecorrido recorrido) {
        this.fecha = fecha;
        this.minutos = minutos;
        this.minutosParque = minutosParque;
        this.descartados = descartados;
        this.puntos = puntos;
        this.canje = canje;
        this.nota = nota;
        this.recorrido = recorrido;
    }
}
