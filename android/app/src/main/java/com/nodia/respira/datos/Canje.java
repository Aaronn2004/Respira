package com.nodia.respira.datos;

// Código de recogida de un regalo (CJ-XXXX), válido 7 días.
public class Canje {

    public final String codigo;
    public final String regalo;
    public final String fecha;
    public final String caduca;
    public String estado; // pendiente | entregado | caducado

    public Canje(String codigo, String regalo, String fecha, String caduca, String estado) {
        this.codigo = codigo;
        this.regalo = regalo;
        this.fecha = fecha;
        this.caduca = caduca;
        this.estado = estado;
    }
}
