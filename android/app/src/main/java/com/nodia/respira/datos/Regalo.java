package com.nodia.respira.datos;

// # Oferta canjeable y sus existencias; el servidor decide si puede confirmarse el canje.

public class Regalo {

    public final String id;
    public final String nombre;
    public final int coste;
    public int stock;

    // --------------------------------------------------------------
    // + id: Text, nombre: Text, coste: Z, stock: Z
    //       -->
    //     Regalo() -->
    // --------------------------------------------------------------
    public Regalo(String id, String nombre, int coste, int stock) {
        this.id = id;
        this.nombre = nombre;
        this.coste = coste;
        this.stock = stock;
    }
}
