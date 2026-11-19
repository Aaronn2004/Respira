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

// # Resumen del recorrido que acompaña al historial de puntos; las horas se muestran en Europe/Madrid.
// # Ausente representa la ausencia lógica (null en el código).
// # ValorJson = Ausente | B | R | Text | [ValorJson] | Map[Text, ValorJson].
// # ObjetoJson = Map[Text, ValorJson].
// # ErrorDatos = { detalle: Text }; fallo al leer o interpretar datos.

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

    // --------------------------------------------------------------
    // + inicio: Text | Ausente
    //   fin: Text | Ausente
    //   duracion_min: Z
    //   distancia_m: Z
    //   lecturas: Z
    //   parques: [Text]
    //       -->
    //     ResumenRecorrido() -->
    // --------------------------------------------------------------
    public ResumenRecorrido(String inicio, String fin, int duracionMin, int distanciaM, int lecturas, List<String> parques) {
        this.inicio = inicio;
        this.fin = fin;
        this.duracionMin = duracionMin;
        this.distanciaM = distanciaM;
        this.lecturas = lecturas;
        this.parques = Collections.unmodifiableList(new ArrayList<>(parques));
    }

    // --------------------------------------------------------------
    // +
    //     vacio() <--
    //       <--
    //   B
    // --------------------------------------------------------------
    public boolean vacio() {
        return lecturas == 0;
    }

    // --------------------------------------------------------------
    // # También lee la copia guardada en el móvil (inicio y fin ya en "HH:mm").
    // + o: ObjetoJson | Ausente
    //       -->
    //     deJSON() --x
    //       <--
    //   ResumenRecorrido | Ausente | ErrorDatos
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

    // --------------------------------------------------------------
    // +
    //     json() <--
    //       <--
    //   ObjetoJson | ErrorDatos
    // --------------------------------------------------------------
    public JSONObject json() throws JSONException {
        return new JSONObject().put("inicio", inicio == null ? JSONObject.NULL : inicio)
                .put("fin", fin == null ? JSONObject.NULL : fin).put("duracion_min", duracionMin)
                .put("distancia_m", distanciaM).put("lecturas", lecturas).put("parques", new JSONArray(parques));
    }

    // '2026-11-08T09:00:00.000Z' --> '10:00' (hora de España); 'HH:mm' se deja igual
    // --------------------------------------------------------------
    // - iso: Text | Ausente
    //       -->
    //     hora() --x
    //       <--
    //   Text | Ausente
    // --------------------------------------------------------------
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
    // --------------------------------------------------------------
    // + metros: Z
    //       -->
    //     distanciaTexto() --x
    //       <--
    //   Text
    // --------------------------------------------------------------
    public static String distanciaTexto(int metros) {
        if (metros < 1000) {
            return metros + " m";
        }
        return String.format(new Locale("es", "ES"), "%.1f km", metros / 1000.0);
    }

    // 45 --> "45 min"; 85 --> "1 h 25 min"; 120 --> "2 h"
    // --------------------------------------------------------------
    // + min: Z
    //       -->
    //     duracionTexto() --x
    //       <--
    //   Text
    // --------------------------------------------------------------
    public static String duracionTexto(int min) {
        if (min < 60) {
            return min + " min";
        }
        return (min / 60) + " h" + (min % 60 == 0 ? "" : " " + (min % 60) + " min");
    }

    // "1 h 25 min · 2,4 km · 34 lecturas"
    // --------------------------------------------------------------
    // +
    //     linea() <--
    //       <--
    //   Text
    // --------------------------------------------------------------
    public String linea() {
        return duracionTexto(duracionMin) + " · " + distanciaTexto(distanciaM) + " · " + lecturas
                + (lecturas == 1 ? " lectura" : " lecturas");
    }
}
