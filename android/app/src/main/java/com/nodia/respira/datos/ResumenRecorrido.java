package com.nodia.respira.datos;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

// ==============================================================
// Estadísticas del recorrido de un día con sensor. Las calcula el
// servidor (Puntos.php) con las mediciones de ese día:
//   inicio / fin   primera y última medición (hora local, "10:42")
//   duracionMin    de la primera a la última
//   distanciaM     lo andado en los tramos válidos (sin huecos ni vehículo)
//   lecturas       anuncios recibidos (las 3 magnitudes de un instante = 1)
//   parques        parques por los que pasó
// ==============================================================
public class ResumenRecorrido {

    public final String inicio;       // null si no hubo mediciones
    public final String fin;
    public final int duracionMin;
    public final int distanciaM;
    public final int lecturas;
    public final List<String> parques;

    public ResumenRecorrido(String inicio, String fin, int duracionMin, int distanciaM, int lecturas, List<String> parques) {
        this.inicio = inicio;
        this.fin = fin;
        this.duracionMin = duracionMin;
        this.distanciaM = distanciaM;
        this.lecturas = lecturas;
        this.parques = Collections.unmodifiableList(new ArrayList<>(parques));
    }

    public boolean vacio() {
        return lecturas == 0;
    }

    // --------------------------------------------------------------
    // {inicio: ISO | null, fin, duracion_min, distancia_m, lecturas, parques: [..]}
    //   --> deJSON() --> ResumenRecorrido | null
    // También lee la copia guardada en el móvil (inicio y fin ya en "HH:mm").
    // --------------------------------------------------------------
    public static ResumenRecorrido deJSON(JSONObject o) throws JSONException {
        if (o == null) {
            return null;
        }
        List<String> p = new ArrayList<>();
        JSONArray a = o.optJSONArray("parques");
        for (int i = 0; a != null && i < a.length(); i++) {
            p.add(a.getString(i));
        }
        return new ResumenRecorrido(hora(o.isNull("inicio") ? null : o.getString("inicio")),
                hora(o.isNull("fin") ? null : o.getString("fin")),
                o.optInt("duracion_min", 0), o.optInt("distancia_m", 0), o.optInt("lecturas", 0), p);
    }

    public JSONObject json() throws JSONException {
        return new JSONObject().put("inicio", inicio == null ? JSONObject.NULL : inicio)
                .put("fin", fin == null ? JSONObject.NULL : fin).put("duracion_min", duracionMin)
                .put("distancia_m", distanciaM).put("lecturas", lecturas).put("parques", new JSONArray(parques));
    }

    // '2026-11-08T09:00:00.000Z' --> '10:00' (hora de España); 'HH:mm' se deja igual
    static String hora(String iso) {
        if (iso == null || iso.length() <= 5) {
            return iso;
        }
        try {
            java.time.LocalTime t = Instant.parse(iso).atZone(ZoneId.of("Europe/Madrid")).toLocalTime();
            return String.format(Locale.ROOT, "%02d:%02d", t.getHour(), t.getMinute());
        } catch (Exception e) {
            return iso;
        }
    }

    // 850 --> "850 m"; 2430 --> "2,4 km"
    public static String distanciaTexto(int metros) {
        if (metros < 1000) {
            return metros + " m";
        }
        return String.format(new Locale("es", "ES"), "%.1f km", metros / 1000.0);
    }

    // 45 --> "45 min"; 85 --> "1 h 25 min"; 120 --> "2 h"
    public static String duracionTexto(int min) {
        if (min < 60) {
            return min + " min";
        }
        return (min / 60) + " h" + (min % 60 == 0 ? "" : " " + (min % 60) + " min");
    }

    // "1 h 25 min · 2,4 km · 34 lecturas"
    public String linea() {
        return duracionTexto(duracionMin) + " · " + distanciaTexto(distanciaM) + " · " + lecturas
                + (lecturas == 1 ? " lectura" : " lecturas");
    }
}
