package com.nodia.respira.datos;

public class Regalo {

    public final String id;
    public final String nombre;
    public final int coste;
    public int stock;

    public Regalo(String id, String nombre, int coste, int stock) {
        this.id = id;
        this.nombre = nombre;
        this.coste = coste;
        this.stock = stock;
    }
}
