package com.nodia.respira.datos;

// ==============================================================
// Alerta de un parque (se guarda solo en el móvil).
// condicion: "bajo" | "medio" | "alto" | "custom"
// ==============================================================
public class Alerta {

    public final String zonaId;
    public final Magnitud magnitud;
    public final String condicion;
    public final String valor; // solo si condicion = custom

    public Alerta(String zonaId, Magnitud magnitud, String condicion, String valor) {
        this.zonaId = zonaId;
        this.magnitud = magnitud;
        this.condicion = condicion;
        this.valor = valor;
    }

    public String texto() {
        String m = magnitud.nombre.toLowerCase();
        switch (condicion) {
            case "custom":
                return m + " por encima de " + valor + " " + magnitud.unidad;
            case "bajo":
                return m + " en nivel bajo (buen momento)";
            default:
                return m + " en nivel " + Nivel.desdeId(condicion).nombre.toLowerCase();
        }
    }
}
