package com.nodia.respira.datos;

import java.util.Locale;

// ==============================================================
// Magnitud: lo que mide el sensor (ozono, temperatura y ruido).
// Los umbrales son orientativos (mismos que el prototipo).
// ==============================================================
public enum Magnitud {

    O3("o3", "Ozono", "O₃", "ppm", 0.060, 0.090, 0.001, 0.5),
    TEMP("temp", "Temperatura", "T", "°C", 25, 32, -20, 50),
    RUIDO("ruido", "Ruido", "dB", "dB", 55, 65, 30, 120);

    public final String id;
    public final String nombre;
    public final String abreviatura;
    public final String unidad;
    public final double limiteMedio;   // a partir de aquí: nivel medio
    public final double limiteAlto;    // a partir de aquí: nivel alto
    public final double rangoMin;      // rango aceptado en alertas personalizadas
    public final double rangoMax;

    Magnitud(String id, String nombre, String abreviatura, String unidad,
             double limiteMedio, double limiteAlto, double rangoMin, double rangoMax) {
        this.id = id;
        this.nombre = nombre;
        this.abreviatura = abreviatura;
        this.unidad = unidad;
        this.limiteMedio = limiteMedio;
        this.limiteAlto = limiteAlto;
        this.rangoMin = rangoMin;
        this.rangoMax = rangoMax;
    }

    // --------------------------------------------------------------
    // valor: R | null --> nivel() --> Nivel
    // --------------------------------------------------------------
    public Nivel nivel(Double valor) {
        if (valor == null) {
            return Nivel.NINGUNO;
        }
        if (valor < limiteMedio) {
            return Nivel.BAJO;
        }
        return valor < limiteAlto ? Nivel.MEDIO : Nivel.ALTO;
    }

    // --------------------------------------------------------------
    // valor: R | null --> formatear() --> Text
    // --------------------------------------------------------------
    public String formatear(Double valor) {
        if (valor == null) {
            return "—";
        }
        switch (this) {
            case O3:
                return String.format(Locale.US, "%.3f", valor);
            case TEMP:
                return String.format(Locale.US, "%.1f", valor);
            default:
                return String.valueOf(Math.round(valor));
        }
    }

    public static Magnitud desdeId(String id) {
        for (Magnitud m : values()) {
            if (m.id.equals(id)) {
                return m;
            }
        }
        return O3;
    }
}
