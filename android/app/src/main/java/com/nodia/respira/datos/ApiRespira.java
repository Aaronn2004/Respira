package com.nodia.respira.datos;

import com.nodia.respira.BuildConfig;
import com.nodia.respira.ble.PeticionarioREST;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// ==============================================================
// ApiRespira: cliente de la API de Respira (servidor/api/index.php).
//   - Zonas y series de las gráficas (públicas).
//   - Todo lo de la cuenta (sesión, puntos, canjes, percepciones,
//     sensor prestado), con el token de la sesión.
// Las respuestas llegan en un hilo aparte: quien llama las pasa al
// hilo principal (Repositorio lo hace).
// ==============================================================
public class ApiRespira {

    public interface Listo<T> {
        void ok(T datos);
    }

    public interface Fallo {
        void error(String detalle);
    }

    // Error de la API ya traducido: mensaje para enseñar y, si los hay,
    // los errores de cada campo del formulario
    public static class Error {
        public final int codigo;            // HTTP; -1 = sin conexión
        public final String mensaje;
        public final Map<String, String> campos;
        public final Integer quedan;        // intentos de inicio de sesión que quedan
        public final long hasta;

        Error(int codigo, String mensaje, Map<String, String> campos, Integer quedan, long hasta) {
            this.hasta = hasta;
            this.codigo = codigo;
            this.mensaje = mensaje;
            this.campos = campos;
            this.quedan = quedan;
        }
    }

    public interface Respuesta {
        void ok(String cuerpo);

        void error(Error e);
    }

    public static final String SIN_CONEXION = "Sin conexión con el servidor. Comprueba que tienes internet e inténtalo de nuevo.";

    private final String base;
    private final PeticionarioREST peticionario = new PeticionarioREST();

    public ApiRespira() {
        String u = BuildConfig.URL_SERVIDOR;
        base = u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }

    // --------------------------------------------------------------
    // metodo, ruta ("/yo"), cuerpo | null, token | null --> pedir() --> Respuesta
    // 2xx --> ok(cuerpo); lo demás --> error(Error)
    // --------------------------------------------------------------
    public void pedir(String metodo, String ruta, JSONObject cuerpo, String token, Respuesta r) {
        peticionario.hacerPeticionREST(metodo, base + ruta, cuerpo == null ? null : cuerpo.toString(), token,
                (codigo, texto) -> {
                    if (codigo >= 200 && codigo < 300) {
                        r.ok(texto);
                    } else {
                        r.error(error(codigo, texto));
                    }
                });
    }

    // --------------------------------------------------------------
    // codigo, cuerpo --> error() --> Error   ({error, errores: {campo: msg}, campo, quedan})
    // --------------------------------------------------------------
    static Error error(int codigo, String cuerpo) {
        if (codigo == -1) {
            return new Error(-1, SIN_CONEXION, new HashMap<>(), null, 0);
        }
        Map<String, String> campos = new HashMap<>();
        String mensaje = "El servidor ha respondido con un error (" + codigo + "). Inténtalo más tarde.";
        Integer quedan = null;
        long hasta = 0;
        try {
            JSONObject o = new JSONObject(cuerpo);
            hasta = momento(o, "hasta");
            if (o.has("error")) {
                mensaje = o.getString("error");
            }
            JSONObject e = o.optJSONObject("errores");
            if (e != null) {
                Iterator<String> k = e.keys();
                while (k.hasNext()) {
                    String c = k.next();
                    campos.put(c, e.getString(c));
                }
            }
            if (o.has("campo") && !o.isNull("campo")) {
                campos.put(o.getString("campo"), mensaje);
            }
            if (o.has("quedan") && !o.isNull("quedan")) {
                quedan = o.getInt("quedan");
            }
        } catch (Exception ignorada) {
            // respuesta que no es JSON (p. ej. una página de error del hosting)
        }
        return new Error(codigo, mensaje, campos, quedan, hasta);
    }

    // --------------------------------------------------------------
    // obtenerZonas() --> [Zona] (por callback)
    // --------------------------------------------------------------
    public void obtenerZonas(Listo<List<Zona>> listo, Fallo fallo) {
        peticionario.hacerPeticionREST("GET", base + "/zonas", null, (codigo, cuerpo) -> {
            if (codigo != 200) {
                fallo.error(codigo + " " + cuerpo);
                return;
            }
            try {
                listo.ok(zonasDeJSON(cuerpo));
            } catch (Exception e) {
                fallo.error("Respuesta no válida: " + e);
            }
        });
    }

    // --------------------------------------------------------------
    // json: Text --> zonasDeJSON() --> [Zona]   (barrios primero: se dibujan debajo)
    // --------------------------------------------------------------
    static List<Zona> zonasDeJSON(String json) throws Exception {
        JSONArray a = new JSONArray(json);
        List<Zona> barrios = new ArrayList<>(), parques = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            List<double[]> contorno = anilloDeJSON(o.getJSONArray("poligono"));
            Zona z = new Zona(o.getString("id"), o.getString("tipo"), o.getString("nombre"), contorno,
                    o.isNull("hace_min") ? null : o.getInt("hace_min"), o.optInt("n", 0),
                    numero(o, "o3"), numero(o, "temp"), numero(o, "ruido"));
            z.conSimulado(o.optBoolean("simulado"));
            JSONArray poligonos = o.optJSONArray("poligonos");
            if (poligonos != null) {
                List<List<List<double[]>>> contornos = new ArrayList<>();
                for (int j = 0; j < poligonos.length(); j++) {
                    JSONArray poligono = poligonos.getJSONArray(j);
                    List<List<double[]>> anillos = new ArrayList<>();
                    for (int k = 0; k < poligono.length(); k++) {
                        anillos.add(anilloDeJSON(poligono.getJSONArray(k)));
                    }
                    if (!anillos.isEmpty()) contornos.add(anillos);
                }
                z.conContornos(contornos);
            }
            JSONObject detalle = o.optJSONObject("detalle");
            if (detalle != null) {
                for (Magnitud m : Magnitud.values()) {
                    JSONObject d = detalle.optJSONObject(m.id);
                    if (d == null) continue;
                    String estado = d.optString("estado", Reglas.ESTADO_SIN_DATOS);
                    if ("reciente".equals(estado)) estado = Reglas.ESTADO_OK;
                    z.conDato(m, new Zona.Dato(numero(d, "valor"), estado, momento(d, "momento"),
                            d.isNull("origen") ? "" : d.optString("origen")));
                }
            }
            JSONObject m24 = o.optJSONObject("media_24h");
            if (m24 != null) {
                for (Magnitud m : Magnitud.values()) {
                    z.conMedia24h(m, numero(m24, m.id));
                }
            }
            JSONArray e = o.optJSONArray("etiqueta");
            if (e != null && e.length() == 2) {
                z.conEtiquetaEn(new double[]{e.getDouble(0), e.getDouble(1)});
            }
            (z.esParque() ? parques : barrios).add(z);
        }
        barrios.addAll(parques);
        return barrios;
    }

    private static List<double[]> anilloDeJSON(JSONArray p) throws Exception {
        List<double[]> anillo = new ArrayList<>();
        for (int i = 0; i < p.length(); i++) {
            JSONArray punto = p.getJSONArray(i);
            anillo.add(new double[]{punto.getDouble(0), punto.getDouble(1)});
        }
        return anillo;
    }

    private static long momento(JSONObject o, String k) {
        try {
            return java.time.Instant.parse(o.optString(k)).toEpochMilli();
        } catch (java.time.format.DateTimeParseException e) {
            return 0;
        }
    }

    private static Double numero(JSONObject o, String k) {
        double valor = o.optDouble(k);
        return Double.isFinite(valor) ? valor : null;
    }

    private static Double[] numeros(JSONArray v) throws Exception {
        Double[] out = new Double[v.length()];
        for (int i = 0; i < v.length(); i++) {
            out[i] = v.isNull(i) ? null : v.getDouble(i);
        }
        return out;
    }

    // {valores, aprox, eventos: {alertas: [{desde, hasta, max, nivel}], picos: [{momento, valor}]}} --> Serie
    static Serie serieDeJSON(JSONObject o) throws Exception {
        JSONArray a = o.optJSONArray("aprox");
        Serie s = new Serie(numeros(o.getJSONArray("valores")), a == null ? null : numeros(a));
        JSONArray origen = o.optJSONArray("origen");
        if (origen != null) {
            s.origen = new String[origen.length()];
            for (int i = 0; i < origen.length(); i++) s.origen[i] = origen.isNull(i) ? null : origen.getString(i);
        }
        JSONObject resumen = o.optJSONObject("resumen");
        if (resumen != null) {
            s.conResumen = true;
            s.minimo = numero(resumen, "minimo");
            s.media = numero(resumen, "media");
            s.maximo = numero(resumen, "maximo");
        }
        JSONObject ev = o.optJSONObject("eventos");
        if (ev != null) {
            JSONArray al = ev.getJSONArray("alertas");
            for (int i = 0; i < al.length(); i++) {
                JSONObject x = al.getJSONObject(i);
                s.alertas.add(new Serie.Evento(java.time.Instant.parse(x.getString("desde")).toEpochMilli(),
                        java.time.Instant.parse(x.getString("hasta")).toEpochMilli(), x.getDouble("max"), x.getString("nivel"), x.optString("origen")));
            }
            JSONArray pi = ev.getJSONArray("picos");
            for (int i = 0; i < pi.length(); i++) {
                JSONObject x = pi.getJSONObject(i);
                long t = java.time.Instant.parse(x.getString("momento")).toEpochMilli();
                s.picos.add(new Serie.Evento(t, t, x.getDouble("valor"), x.optString("nivel"), x.optString("origen")));
            }
            JSONArray av = ev.optJSONArray("avisos");
            for (int i = 0; av != null && i < av.length(); i++) {
                JSONObject x = av.getJSONObject(i);
                long t = momento(x, "momento");
                s.avisos.add(new Serie.Evento(t, t, x.getDouble("valor"), x.optString("nivel"), x.optString("origen")));
            }
        }
        return s;
    }

    // --------------------------------------------------------------
    // zonaId, magnitud, periodo ("24h" | "7d") --> obtenerSerie() --> Serie
    // --------------------------------------------------------------
    public void obtenerSerie(String zonaId, Magnitud magnitud, String periodo, Listo<Serie> listo, Fallo fallo) {
        String url = base + "/zonas/" + zonaId + "/serie?magnitud=" + magnitud.id + "&periodo=" + periodo;
        peticionario.hacerPeticionREST("GET", url, null, (codigo, cuerpo) -> {
            if (codigo != 200) {
                fallo.error(codigo + " " + cuerpo);
                return;
            }
            try {
                listo.ok(serieDeJSON(new JSONObject(cuerpo)));
            } catch (Exception e) {
                fallo.error("Respuesta no válida: " + e);
            }
        });
    }
}
