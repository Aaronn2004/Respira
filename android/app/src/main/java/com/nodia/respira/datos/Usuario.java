package com.nodia.respira.datos;

// ==============================================================
// Cuenta con la que se ha iniciado sesión en la app.
//   id = "U-XXXXX" (vecinos: lo dicen en el punto de reparto) o
//        "T-XXXX" (trabajadores municipales).
// La contraseña NO se guarda en el móvil: la comprueba el servidor
// (con hash) y el móvil solo guarda el token de la sesión.
// ==============================================================
public class Usuario {

    public static final String VECINO = "vecino";
    public static final String TRABAJADOR = "trabajador";

    public final String id;
    public final String nombre;
    public final String email;
    public final String rol;

    public Usuario(String id, String nombre, String email, String rol) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.rol = rol == null ? VECINO : rol;
    }

    public boolean esTrabajador() {
        return TRABAJADOR.equals(rol);
    }
}
