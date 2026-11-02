package com.nodia.respira.datos;

// ==============================================================
// Percepción de un vecino: cómo nota el ruido, el calor o el aire.
// tipo: "ruido" | "temp" | "aire"; nivel: 1..3
// ==============================================================
public class Percepcion {

    public static final String[] TIPOS = {"ruido", "temp", "aire"};

    public final long id;
    public final String tipo;
    public final int nivel;
    public final double lat;
    public final double lng;
    public final long cuando;      // ms
    public final String direccion;
    public final String autorId;   // null si es de otro vecino (demo)

    public Percepcion(long id, String tipo, int nivel, double lat, double lng,
                      long cuando, String direccion, String autorId) {
        this.id = id;
        this.tipo = tipo;
        this.nivel = nivel;
        this.lat = lat;
        this.lng = lng;
        this.cuando = cuando;
        this.direccion = direccion;
        this.autorId = autorId;
    }

    public static String nombreTipo(String tipo) {
        switch (tipo) {
            case "temp": return "Sensación térmica";
            case "aire": return "Aire percibido";
            default: return "Ruido percibido";
        }
    }

    public static String[] nivelesTipo(String tipo) {
        switch (tipo) {
            case "temp": return new String[]{"Agradable", "Calor", "Mucho calor"};
            case "aire": return new String[]{"Limpio", "Regular", "Malo"};
            default: return new String[]{"Bajo", "Medio", "Alto"};
        }
    }

    public static String[] detallesTipo(String tipo) {
        switch (tipo) {
            case "temp": return new String[]{"Se está bien", "Apetece buscar sombra", "Agobiante"};
            case "aire": return new String[]{"Se respira bien", "Algo cargado", "Pican ojos o garganta"};
            default: return new String[]{"Se puede conversar", "Molesta un poco", "Muy molesto"};
        }
    }

    public String titulo() {
        return nombreTipo(tipo) + ": " + nivelesTipo(tipo)[nivel - 1];
    }

    public Nivel comoNivel() {
        return nivel == 1 ? Nivel.BAJO : nivel == 2 ? Nivel.MEDIO : Nivel.ALTO;
    }
}
