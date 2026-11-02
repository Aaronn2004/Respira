package com.nodia.respira.datos;

import com.nodia.respira.R;

// ==============================================================
// Nivel de una medida respecto a los umbrales de su magnitud.
// ==============================================================
public enum Nivel {

    BAJO("bajo", "Bajo", R.color.nivel_bajo, R.color.nivel_bajo_suave),
    MEDIO("medio", "Medio", R.color.nivel_medio, R.color.nivel_medio_suave),
    ALTO("alto", "Alto", R.color.nivel_alto, R.color.nivel_alto_suave),
    NINGUNO("none", "Sin datos", R.color.nivel_ninguno, R.color.superficie_2);

    public final String id;
    public final String nombre;
    public final int color;
    public final int colorSuave;

    Nivel(String id, String nombre, int color, int colorSuave) {
        this.id = id;
        this.nombre = nombre;
        this.color = color;
        this.colorSuave = colorSuave;
    }

    public static Nivel desdeId(String id) {
        for (Nivel n : values()) {
            if (n.id.equals(id)) {
                return n;
            }
        }
        return NINGUNO;
    }
}
