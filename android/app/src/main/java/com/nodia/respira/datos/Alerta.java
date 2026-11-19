package com.nodia.respira.datos;

// # Umbral de una magnitud en una zona; la clave compuesta permite una alerta por pareja.

// ==============================================================
// Alerta de un parque (se guarda solo en el móvil).
// condicion: "bajo" | "medio" | "alto" | "custom"
// ==============================================================
public class Alerta {

    public final String zonaId;
    public final Magnitud magnitud;
    public final String condicion;
    public final String valor; // solo si condicion = custom

    // --------------------------------------------------------------
    // + zona_id: Text, magnitud: Magnitud, condicion: Text, valor: Text
    //       -->
    //     Alerta() -->
    // --------------------------------------------------------------
    public Alerta(String zonaId, Magnitud magnitud, String condicion, String valor) {
        this.zonaId = zonaId;
        this.magnitud = magnitud;
        this.condicion = condicion;
        this.valor = valor;
    }

    // --------------------------------------------------------------
    // +
    //     clave() <--
    //       <--
    //   Text
    // --------------------------------------------------------------
    public String clave() {
        return clave(zonaId, magnitud);
    }

    // --------------------------------------------------------------
    // - zona_id: Text, m: Magnitud
    //       -->
    //     clave() --x
    //       <--
    //   Text
    // --------------------------------------------------------------
    static String clave(String zonaId, Magnitud m) {
        return zonaId + ":" + m.id;
    }

    // --------------------------------------------------------------
    // +
    //     texto() <--
    //       <--
    //   Text
    // --------------------------------------------------------------
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
