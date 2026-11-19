package com.nodia.respira.datos;

// # Vale de regalo devuelto por la API, con código, caducidad y estado.

// Código de recogida de un regalo (CJ-XXXX), válido 7 días.
public class Canje {

    public final String codigo;
    public final String regalo;
    public final String fecha;
    public final String caduca;
    public String estado; // pendiente | entregado | caducado

    // --------------------------------------------------------------
    // + codigo: Text, regalo: Text, fecha: Text, caduca: Text, estado: Text
    //       -->
    //     Canje() -->
    // --------------------------------------------------------------
    public Canje(String codigo, String regalo, String fecha, String caduca, String estado) {
        this.codigo = codigo;
        this.regalo = regalo;
        this.fecha = fecha;
        this.caduca = caduca;
        this.estado = estado;
    }
}
