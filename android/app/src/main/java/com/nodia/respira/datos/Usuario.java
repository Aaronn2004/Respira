package com.nodia.respira.datos;

// ==============================================================
// Cuenta de usuario. id = "U-XXXXX": lo da en el punto de reparto.
// OJO: en la versión real la contraseña NO se guarda en el móvil;
// la comprueba el servidor (con hash). Aquí es una cuenta de demo.
// ==============================================================
public class Usuario {

    public final String id;
    public final String nombre;
    public final String email;
    final String clave;

    public Usuario(String id, String nombre, String email, String clave) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.clave = clave;
    }
}
