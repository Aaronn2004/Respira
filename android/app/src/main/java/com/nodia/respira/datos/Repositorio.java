package com.nodia.respira.datos;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// ==============================================================
// Repositorio: único punto de acceso a los datos de la app.
//
// Todo lo que se comparte vive en el SERVIDOR (API de Respira):
// cuentas y sesión, sensor prestado, puntos, regalos, canjes,
// percepciones y zonas. Aquí se guarda una copia (caché) para que
// la app se abra al momento y se vea algo sin conexión.
//
// Parques seguidos (favoritos) y sus alertas: con sesión se guardan en la
// CUENTA (PUT /yo/seguimiento) y se recuperan en cualquier móvil; sin
// sesión, solo en este móvil, y se suman a la cuenta al iniciar sesión.
// Solo en este móvil: la bienvenida y el recorrido en curso.
//
//   - Las lecturas (usuario(), cuenta(), regalos()…) devuelven la caché.
//   - refrescar() la pone al día y avisa a las pantallas (observar).
//   - Lo que cambia algo en el servidor (iniciar sesión, canjear,
//     publicar…) responde por callback (Hecho) en el hilo principal.
// ==============================================================
public final class Repositorio {

    // ---------- tipos auxiliares ----------
    public static class Cuenta {
        public int puntos;
        public final List<Movimiento> historial = new ArrayList<>();
        public final List<Canje> canjes = new ArrayList<>();
        public int hoyMin, hoyMinParque, hoyDescartados, hoyPuntos;
        public ResumenRecorrido hoyRecorrido;   // distancia, duración… de hoy (null si no hay)
        // reglas de hoy (para el texto «Cómo se ganan»)
        public int minPorPunto = 5, multParque = 2, maxDia = 100;
    }

    // Resultado de una operación con el servidor (llega en el hilo principal)
    public interface Hecho<T> {
        void ok(T resultado);

        // mensaje para enseñar; campos = errores de cada campo del formulario
        void error(String mensaje, Map<String, String> campos);
    }

    // Lo usa RespiraApp para enseñar la notificación de una alerta
    public interface AvisoDeAlerta {
        void avisar(Alerta alerta, Zona zona);
    }

    public static final int MAX_PERCEPCIONES_HORA = 3;
    private static final String PREFS = "respira";
    private static final String CLAVE = "estado_v2";
    private static final String CLAVE_V1 = "estado_v1";
    private static final long REFRESCO_MIN_MS = 30_000;
    private static final long ALERTA_CADA_MS = 3_600_000;   // una misma alerta, como mucho cada hora
    private static Repositorio instancia;

    private final SharedPreferences prefs;
    private final Handler hiloPrincipal = new Handler(Looper.getMainLooper());
    private final List<Runnable> observadores = new ArrayList<>();
    private final ApiRespira api = new ApiRespira();

    // Zonas: las de demostración hasta que responde la API; luego, las reales
    private final List<Zona> zonas = new ArrayList<>(DatosDemo.zonas());
    private boolean datosReales = false;
    private final Map<String, Double> ultimosValores = new HashMap<>();

    // ---------- copia de lo del servidor ----------
    private String token;
    private Usuario usuario;
    private Cuenta cuenta;
    private String sensorPrestado;            // código del préstamo en curso (o null)
    private long prestamoHasta;               // fecha límite para devolverlo (ms; 0 = sin préstamo)
    private final List<Regalo> regalos = new ArrayList<>();
    private final List<Percepcion> percepciones = new ArrayList<>();
    private final List<Percepcion> mias = new ArrayList<>();
    private long ultimoRefresco;

    // ---------- seguimiento (en la cuenta si hay sesión) ----------
    private boolean seguimientoPendiente;   // cambios aún sin subir a la cuenta
    private boolean fusionarSeguimiento;    // recién iniciada la sesión: sumar lo del móvil a la cuenta
    private final Set<String> favoritos = new LinkedHashSet<>();
    private final Map<String, Alerta> alertas = new LinkedHashMap<>();
    private final Map<String, Long> alertaAvisada = new HashMap<>();
    private boolean onboardingVisto;
    private long bloqueoHasta;
    private Recorrido recorrido;
    private AvisoDeAlerta avisoDeAlerta;

    // --------------------------------------------------------------
    // contexto: Context --> obtener() --> Repositorio
    // --------------------------------------------------------------
    public static synchronized Repositorio obtener(Context contexto) {
        if (instancia == null) {
            instancia = new Repositorio(contexto.getApplicationContext());
        }
        return instancia;
    }

    private Repositorio(Context contexto) {
        prefs = contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(CLAVE, null);
        try {
            if (json != null) {
                cargar(new JSONObject(json));
            } else if (prefs.getString(CLAVE_V1, null) != null) {
                cargarV1(new JSONObject(prefs.getString(CLAVE_V1, "{}")));
            }
        } catch (JSONException e) {
            Log.w("Respira", "Datos guardados no válidos: se empieza de cero", e);
        }
    }

    // ==============================================================
    // Observadores: las pantallas se refrescan cuando cambia algo
    // ==============================================================
    public synchronized void observar(Runnable r) {
        observadores.add(r);
    }

    public synchronized void dejarDeObservar(Runnable r) {
        observadores.remove(r);
    }

    private void cambio() {
        guardar();
        final List<Runnable> copia;
        synchronized (this) {
            copia = new ArrayList<>(observadores);
        }
        hiloPrincipal.post(() -> {
            for (Runnable r : copia) {
                r.run();
            }
        });
    }

    public void alAvisarDeAlerta(AvisoDeAlerta a) {
        avisoDeAlerta = a;
    }

    // ==============================================================
    // Zonas, favoritos y alertas
    // ==============================================================
    public synchronized List<Zona> zonas() {
        return Collections.unmodifiableList(new ArrayList<>(zonas));
    }

    // --------------------------------------------------------------
    // datosReales() --> B   (F = la API no responde: se ven los de demostración)
    // --------------------------------------------------------------
    public synchronized boolean datosReales() {
        return datosReales;
    }

    // --------------------------------------------------------------
    // actualizarZonas(): GET /zonas y, si responde, sustituye las zonas
    // y comprueba las alertas de este móvil con los datos nuevos
    // --------------------------------------------------------------
    public void actualizarZonas() {
        api.obtenerZonas(lista -> hiloPrincipal.post(() -> {
            synchronized (this) {
                zonas.clear();
                zonas.addAll(lista);
                datosReales = true;
            }
            comprobarAlertas();
            cambio();
        }), error -> Log.w("Respira", "No se pudieron leer las zonas de la API: " + error));
    }

    // --------------------------------------------------------------
    // zonaId, magnitud, periodo --> serie() --> Serie | null (en el hilo principal)
    // --------------------------------------------------------------
    public void serie(String zonaId, Magnitud m, String periodo, ApiRespira.Listo<Serie> listo) {
        api.obtenerSerie(zonaId, m, periodo, d -> hiloPrincipal.post(() -> listo.ok(d)),
                e -> hiloPrincipal.post(() -> listo.ok(null)));
    }

    // --------------------------------------------------------------
    // Alertas: con datos reales, si una zona que sigues cumple tu
    // condición, se avisa (una vez por hora y alerta como mucho).
    // Funciona mientras la app está abierta o el recorrido en marcha:
    // sin un servicio de notificaciones push no se puede avisar con
    // la app cerrada.
    // --------------------------------------------------------------
    private void comprobarAlertas() {
        List<Alerta> saltan = new ArrayList<>();
        List<Zona> donde = new ArrayList<>();
        long ahora = System.currentTimeMillis();
        synchronized (this) {
            if (!datosReales) {
                return;
            }
            for (Alerta a : alertas.values()) {
                Zona z = zona(a.zonaId);
                if (z == null || !Reglas.ESTADO_OK.equals(z.estado()) || !seCumple(a, z)) {
                    continue;
                }
                Long antes = alertaAvisada.get(a.zonaId);
                if (antes != null && ahora - antes < ALERTA_CADA_MS) {
                    continue;
                }
                alertaAvisada.put(a.zonaId, ahora);
                saltan.add(a);
                donde.add(z);
            }
        }
        if (avisoDeAlerta != null) {
            for (int i = 0; i < saltan.size(); i++) {
                avisoDeAlerta.avisar(saltan.get(i), donde.get(i));
            }
        }
    }

    // alerta, zona --> seCumple() --> B
    public static boolean seCumple(Alerta a, Zona z) {
        Double v = z.valor(a.magnitud);
        if (v == null) {
            return false;
        }
        if ("custom".equals(a.condicion)) {
            try {
                return v > Double.parseDouble(a.valor.replace(',', '.'));
            } catch (Exception e) {
                return false;
            }
        }
        Nivel n = a.magnitud.nivel(v);
        if ("bajo".equals(a.condicion)) {
            return n == Nivel.BAJO;
        }
        if ("alto".equals(a.condicion)) {
            return n == Nivel.ALTO;
        }
        return n == Nivel.MEDIO || n == Nivel.ALTO;    // «medio»: medio o peor
    }

    // Último valor recibido del sensor del recorrido (pantalla «Mi sensor»)
    public void anotarUltimoValor(String magnitud, double valor) {
        synchronized (this) {
            ultimosValores.put(magnitud, valor);
        }
        cambio();
    }

    public synchronized Double ultimoValor(String magnitud) {
        return ultimosValores.get(magnitud);
    }

    // Las que estaban pendientes (sin red) ya han llegado al servidor
    public void marcarPendientesEnviadas(int n) {
        synchronized (this) {
            if (recorrido != null) {
                int k = Math.min(n, recorrido.pendientes);
                recorrido.pendientes -= k;
                recorrido.enviadas += k;
            }
        }
        cambio();
    }

    public synchronized List<Zona> parques() {
        List<Zona> p = new ArrayList<>();
        for (Zona z : zonas) {
            if (z.esParque()) {
                p.add(z);
            }
        }
        return p;
    }

    public synchronized Zona zona(String id) {
        for (Zona z : zonas) {
            if (z.id.equals(id)) {
                return z;
            }
        }
        return null;
    }

    // Parque (no barrio) que contiene el punto, o null
    public synchronized Zona parqueEn(double lat, double lng) {
        for (Zona z : zonas) {
            if (z.esParque() && Reglas.dentro(lat, lng, z.contorno)) {
                return z;
            }
        }
        return null;
    }

    public synchronized List<Zona> favoritos() {
        List<Zona> f = new ArrayList<>();
        for (String id : favoritos) {
            Zona z = zona(id);
            if (z != null) {
                f.add(z);
            }
        }
        return f;
    }

    public synchronized boolean esFavorito(String id) {
        return favoritos.contains(id);
    }

    public void alternarFavorito(String id) {
        synchronized (this) {
            if (!favoritos.remove(id)) {
                favoritos.add(id);
            } else {
                alertas.remove(id);      // sin seguir el parque, su alerta no tiene sentido
            }
            seguimientoPendiente = token != null;
        }
        cambio();
        subirSeguimiento();
    }

    public synchronized Alerta alerta(String zonaId) {
        return alertas.get(zonaId);
    }

    public synchronized List<Alerta> alertas() {
        return new ArrayList<>(alertas.values());
    }

    public void guardarAlerta(Alerta a) {
        synchronized (this) {
            alertas.put(a.zonaId, a);
            alertaAvisada.remove(a.zonaId);
            // Una alerta solo tiene sentido en un parque que sigues
            favoritos.add(a.zonaId);
            seguimientoPendiente = token != null;
        }
        comprobarAlertas();
        cambio();
        subirSeguimiento();
    }

    public void borrarAlerta(String zonaId) {
        synchronized (this) {
            alertas.remove(zonaId);
            seguimientoPendiente = token != null;
        }
        cambio();
        subirSeguimiento();
    }

    // --------------------------------------------------------------
    // Sube a la cuenta los parques seguidos y sus alertas (lista completa).
    // Sin red se queda pendiente y se reintenta en el siguiente refresco.
    // --------------------------------------------------------------
    private void subirSeguimiento() {
        final String t;
        final JSONObject c = new JSONObject();
        synchronized (this) {
            t = token;
            if (t == null || !seguimientoPendiente) {
                return;
            }
            try {
                JSONArray al = new JSONArray();
                for (Alerta a : alertas.values()) {
                    JSONObject x = new JSONObject().put("zona", a.zonaId).put("magnitud", a.magnitud.id).put("condicion", a.condicion);
                    if ("custom".equals(a.condicion)) {
                        x.put("valor", Double.parseDouble(a.valor.replace(',', '.')));
                    }
                    al.put(x);
                }
                c.put("favoritos", new JSONArray(favoritos)).put("alertas", al);
            } catch (JSONException | NumberFormatException | NullPointerException e) {
                Log.w("Respira", "seguimiento: " + e);
                return;
            }
        }
        api.pedir("PUT", "/yo/seguimiento", c, t, new ApiRespira.Respuesta() {
            @Override
            public void ok(String cuerpo) {
                synchronized (Repositorio.this) {
                    if (t.equals(token)) {
                        seguimientoPendiente = false;
                    }
                }
                guardar();
            }

            @Override
            public void error(ApiRespira.Error e) {
                if (e.codigo == 401) {
                    sesionCaducada();
                } else if (e.codigo == 400) {
                    // algo que el servidor no admite (p. ej. una zona que ya no existe): manda la cuenta
                    synchronized (Repositorio.this) {
                        seguimientoPendiente = false;
                    }
                    refrescar(true);
                }
                // sin red: sigue pendiente
            }
        });
    }

    // --------------------------------------------------------------
    // {favoritos: [id], alertas: [{zona, magnitud, condicion, valor}]} --> aplicarSeguimiento()
    //   --> true si hay que subir lo del móvil a la cuenta
    // Recién iniciada la sesión se suman los dos (en una misma zona, manda
    // la alerta del móvil); con cambios sin subir manda el móvil; si no, la cuenta.
    // (llamar con el monitor de this)
    // --------------------------------------------------------------
    private boolean aplicarSeguimiento(JSONObject s) throws JSONException {
        Set<String> fav = new LinkedHashSet<>();
        Map<String, Alerta> al = new LinkedHashMap<>();
        JSONArray f = s.optJSONArray("favoritos");
        for (int i = 0; f != null && i < f.length(); i++) {
            fav.add(f.getString(i));
        }
        JSONArray a = s.optJSONArray("alertas");
        for (int i = 0; a != null && i < a.length(); i++) {
            JSONObject x = a.getJSONObject(i);
            String v = x.isNull("valor") ? "" : numero(x.getDouble("valor"));
            al.put(x.getString("zona"), new Alerta(x.getString("zona"), Magnitud.desdeId(x.getString("magnitud")),
                    x.getString("condicion"), v));
        }
        boolean subir;
        if (fusionarSeguimiento) {
            Set<String> antes = new LinkedHashSet<>(fav);
            int alertasAntes = al.size();
            fav.addAll(favoritos);
            al.putAll(alertas);
            subir = !fav.equals(antes) || al.size() != alertasAntes || !alertas.isEmpty();
        } else if (seguimientoPendiente) {
            return true;                       // lo del móvil es lo último: se sube tal cual
        } else {
            subir = false;
        }
        favoritos.clear();
        favoritos.addAll(fav);
        alertas.clear();
        alertas.putAll(al);
        alertaAvisada.keySet().retainAll(alertas.keySet());
        fusionarSeguimiento = false;
        seguimientoPendiente = subir;
        return subir;
    }

    // 60.0 --> "60"; 0.075 --> "0.075"
    static String numero(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    // Al cerrar sesión los parques seguidos se quedan en la cuenta, no en el móvil
    private void olvidarSeguimiento() {
        synchronized (this) {
            favoritos.clear();
            alertas.clear();
            alertaAvisada.clear();
            seguimientoPendiente = false;
            fusionarSeguimiento = false;
        }
    }

    // ==============================================================
    // Bienvenida
    // ==============================================================
    public synchronized boolean onboardingVisto() {
        return onboardingVisto;
    }

    public void marcarOnboardingVisto() {
        synchronized (this) {
            onboardingVisto = true;
        }
        cambio();
    }

    // ==============================================================
    // Sesión y cuenta (servidor)
    // ==============================================================
    public synchronized Usuario usuario() {
        return usuario;
    }

    public synchronized boolean haySesion() {
        return usuario != null && token != null;
    }

    // token de la sesión (para mandar las mediciones a su nombre)
    public synchronized String token() {
        return token;
    }

    public synchronized long bloqueadoHasta() {
        return bloqueoHasta > System.currentTimeMillis() ? bloqueoHasta : 0;
    }

    // Pasa el resultado al hilo principal
    private <T> void enPrincipal(Hecho<T> h, T r) {
        hiloPrincipal.post(() -> h.ok(r));
    }

    private void errorEnPrincipal(Hecho<?> h, ApiRespira.Error e) {
        if (e.codigo == 401 && haySesion()) {
            sesionCaducada();
        }
        hiloPrincipal.post(() -> h.error(e.mensaje, e.campos));
    }

    private void sesionCaducada() {
        synchronized (this) {
            token = null;
            usuario = null;
            cuenta = null;
            sensorPrestado = null;
            prestamoHasta = 0;
            mias.clear();
        }
        cambio();
    }

    private static Usuario usuarioDeJSON(JSONObject u) throws JSONException {
        return new Usuario(u.getString("id"), u.getString("nombre"), u.getString("email"), u.optString("rol", Usuario.VECINO));
    }

    // Respuesta de POST /sesiones o /usuarios: {token, usuario}
    private Usuario abrirSesion(String cuerpo) throws JSONException {
        JSONObject o = new JSONObject(cuerpo);
        Usuario u = usuarioDeJSON(o.getJSONObject("usuario"));
        synchronized (this) {
            token = o.getString("token");
            usuario = u;
            cuenta = new Cuenta();
            bloqueoHasta = 0;
            recorrido = null;
            ultimoRefresco = 0;
            fusionarSeguimiento = true;
        }
        refrescar(true);
        return u;
    }

    // --------------------------------------------------------------
    // email, clave --> iniciarSesion() --> Usuario (por Hecho)
    // --------------------------------------------------------------
    public void iniciarSesion(String email, String clave, Hecho<Usuario> hecho) {
        JSONObject c = new JSONObject();
        try {
            c.put("email", email.trim()).put("clave", clave).put("cliente", "app");
        } catch (JSONException ignorada) {
            // no pasa con textos
        }
        api.pedir("POST", "/sesiones", c, null, new ApiRespira.Respuesta() {
            @Override
            public void ok(String cuerpo) {
                try {
                    enPrincipal(hecho, abrirSesion(cuerpo));
                    cambio();
                } catch (JSONException e) {
                    errorEnPrincipal(hecho, ApiRespira.error(500, ""));
                }
            }

            @Override
            public void error(ApiRespira.Error e) {
                if (e.codigo == 423) {
                    synchronized (Repositorio.this) {
                        bloqueoHasta = System.currentTimeMillis() + Reglas.BLOQUEO_LOGIN_MS;
                    }
                    guardar();
                }
                errorEnPrincipal(hecho, e);
            }
        });
    }

    // --------------------------------------------------------------
    // nombre, email, clave, acepta --> registrar() --> Usuario (deja la sesión abierta)
    // Errores por campo: nombre, email, clave, acepta
    // --------------------------------------------------------------
    public void registrar(String nombre, String email, String clave, boolean acepta, Hecho<Usuario> hecho) {
        JSONObject c = new JSONObject();
        try {
            c.put("nombre", nombre.trim()).put("email", email.trim()).put("clave", clave).put("acepta", acepta);
        } catch (JSONException ignorada) {
            // no pasa con textos
        }
        api.pedir("POST", "/usuarios", c, null, new ApiRespira.Respuesta() {
            @Override
            public void ok(String cuerpo) {
                try {
                    enPrincipal(hecho, abrirSesion(cuerpo));
                    cambio();
                } catch (JSONException e) {
                    errorEnPrincipal(hecho, ApiRespira.error(500, ""));
                }
            }

            @Override
            public void error(ApiRespira.Error e) {
                errorEnPrincipal(hecho, e);
            }
        });
    }

    // Enlace para crear una contraseña nueva (llega por correo). La
    // respuesta es la misma exista o no la cuenta.
    public void recuperar(String email, Hecho<Void> hecho) {
        JSONObject c = new JSONObject();
        try {
            c.put("email", email.trim());
        } catch (JSONException ignorada) {
            // no pasa
        }
        api.pedir("POST", "/recuperaciones", c, null, respuestaVacia(hecho));
    }

    private ApiRespira.Respuesta respuestaVacia(Hecho<Void> hecho) {
        return new ApiRespira.Respuesta() {
            @Override
            public void ok(String cuerpo) {
                enPrincipal(hecho, null);
            }

            @Override
            public void error(ApiRespira.Error e) {
                errorEnPrincipal(hecho, e);
            }
        };
    }

    public void cerrarSesion() {
        String t;
        synchronized (this) {
            t = token;
            recorrido = null;
        }
        olvidarSeguimiento();
        if (t != null) {
            api.pedir("DELETE", "/sesiones", null, t, new ApiRespira.Respuesta() {
                @Override
                public void ok(String cuerpo) {
                    // nada más que hacer
                }

                @Override
                public void error(ApiRespira.Error e) {
                    // sin red: la sesión caduca sola en el servidor
                }
            });
        }
        sesionCaducada();
    }

    // Borra la cuenta en el servidor (no se puede con un sensor prestado)
    public void borrarCuenta(Hecho<Void> hecho) {
        api.pedir("DELETE", "/yo", null, token(), new ApiRespira.Respuesta() {
            @Override
            public void ok(String cuerpo) {
                olvidarSeguimiento();
                sesionCaducada();
                enPrincipal(hecho, null);
            }

            @Override
            public void error(ApiRespira.Error e) {
                errorEnPrincipal(hecho, e);
            }
        });
    }

    // --------------------------------------------------------------
    // refrescar(forzar): pone al día la caché (cuenta, préstamo, regalos,
    // percepciones). Sin forzar, como mucho cada 30 s.
    // --------------------------------------------------------------
    public void refrescar() {
        refrescar(false);
    }

    public void refrescar(boolean forzar) {
        final String t;
        synchronized (this) {
            long ahora = System.currentTimeMillis();
            if (!forzar && ahora - ultimoRefresco < REFRESCO_MIN_MS) {
                return;
            }
            ultimoRefresco = ahora;
            t = token;
        }
        api.pedir("GET", "/percepciones", null, t, new ApiRespira.Respuesta() {
            @Override
            public void ok(String cuerpo) {
                try {
                    List<Percepcion> l;
                    synchronized (Repositorio.this) {
                        l = percepcionesDeJSON(new JSONArray(cuerpo));
                        percepciones.clear();
                        percepciones.addAll(l);
                    }
                    cambio();
                } catch (JSONException e) {
                    Log.w("Respira", "percepciones: " + e);
                }
            }

            @Override
            public void error(ApiRespira.Error e) {
                Log.w("Respira", "percepciones: " + e.mensaje);
            }
        });
        api.pedir("GET", "/regalos", null, null, new ApiRespira.Respuesta() {
            @Override
            public void ok(String cuerpo) {
                try {
                    JSONArray a = new JSONArray(cuerpo);
                    List<Regalo> l = new ArrayList<>();
                    for (int i = 0; i < a.length(); i++) {
                        JSONObject g = a.getJSONObject(i);
                        l.add(new Regalo(g.getString("id"), g.getString("nombre"), g.getInt("coste"), g.getInt("stock")));
                    }
                    synchronized (Repositorio.this) {
                        regalos.clear();
                        regalos.addAll(l);
                    }
                    cambio();
                } catch (JSONException e) {
                    Log.w("Respira", "regalos: " + e);
                }
            }

            @Override
            public void error(ApiRespira.Error e) {
                Log.w("Respira", "regalos: " + e.mensaje);
            }
        });
        if (t == null) {
            return;
        }
        api.pedir("GET", "/yo", null, t, new ApiRespira.Respuesta() {
            @Override
            public void ok(String cuerpo) {
                try {
                    JSONObject o = new JSONObject(cuerpo);
                    Usuario u = usuarioDeJSON(o.getJSONObject("usuario"));
                    Cuenta c = o.has("cuenta") ? cuentaDeJSON(o.getJSONObject("cuenta")) : new Cuenta();
                    JSONObject p = o.optJSONObject("prestamo");
                    JSONObject seg = o.optJSONObject("seguimiento");
                    boolean subir = false;
                    synchronized (Repositorio.this) {
                        if (!t.equals(token)) {
                            return;           // mientras tanto se cerró la sesión
                        }
                        usuario = u;
                        cuenta = c;
                        sensorPrestado = p == null ? null : p.optString("codigo", null);
                        prestamoHasta = p == null || p.isNull("hasta") ? 0 : msDeISO(p.optString("hasta"));
                        if (seg != null) {
                            subir = aplicarSeguimiento(seg);
                        }
                    }
                    cambio();
                    if (subir) {
                        subirSeguimiento();
                    }
                } catch (JSONException e) {
                    Log.w("Respira", "yo: " + e);
                }
            }

            @Override
            public void error(ApiRespira.Error e) {
                if (e.codigo == 401) {
                    sesionCaducada();
                }
            }
        });
        api.pedir("GET", "/yo/percepciones", null, t, new ApiRespira.Respuesta() {
            @Override
            public void ok(String cuerpo) {
                try {
                    synchronized (Repositorio.this) {
                        List<Percepcion> l = percepcionesDeJSON(new JSONArray(cuerpo));
                        mias.clear();
                        mias.addAll(l);
                    }
                    cambio();
                } catch (JSONException e) {
                    Log.w("Respira", "mis percepciones: " + e);
                }
            }

            @Override
            public void error(ApiRespira.Error e) {
                Log.w("Respira", "mis percepciones: " + e.mensaje);
            }
        });
    }

    // ---------- JSON del servidor --> modelo de la app ----------

    // 'YYYY-MM-DD' --> '6 nov'
    static String fechaCorta(String iso) {
        try {
            LocalDate d = LocalDate.parse(iso.substring(0, 10));
            String[] m = {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"};
            return d.getDayOfMonth() + " " + m[d.getMonthValue() - 1];
        } catch (Exception e) {
            return iso;
        }
    }

    private static Cuenta cuentaDeJSON(JSONObject o) throws JSONException {
        Cuenta c = new Cuenta();
        c.puntos = o.getInt("puntos");
        JSONObject h = o.getJSONObject("hoy");
        c.hoyMin = h.getInt("minutos");
        c.hoyMinParque = h.getInt("minutos_parque");
        c.hoyDescartados = h.getInt("descartados");
        c.hoyPuntos = h.getInt("puntos");
        c.hoyRecorrido = ResumenRecorrido.deJSON(h.optJSONObject("recorrido"));
        JSONObject r = o.optJSONObject("reglas");
        if (r != null) {
            c.minPorPunto = r.getInt("min");
            c.multParque = r.getInt("mult");
            c.maxDia = r.getInt("max");
        }
        JSONArray hist = o.getJSONArray("historial");
        for (int i = 0; i < hist.length(); i++) {
            JSONObject m = hist.getJSONObject(i);
            String rev = m.isNull("revision") ? null : m.getString("revision");
            String razon = m.isNull("razon") ? null : m.getString("razon");
            String nota = "anulado".equals(rev) ? "Anulado" + (razon == null ? "" : ": " + razon)
                    : "pend".equals(rev) ? "En revisión: el ayuntamiento lo está comprobando" : razon;
            c.historial.add(new Movimiento(fechaCorta(m.getString("fecha")), m.getInt("minutos"),
                    m.getInt("minutos_parque"), m.getInt("descartados"), m.getInt("puntos"),
                    m.isNull("canje") ? null : m.getString("canje"), nota,
                    ResumenRecorrido.deJSON(m.optJSONObject("recorrido"))));
        }
        JSONArray cj = o.getJSONArray("canjes");
        for (int i = 0; i < cj.length(); i++) {
            JSONObject x = cj.getJSONObject(i);
            c.canjes.add(new Canje(x.getString("codigo"), x.getString("regalo"), fechaCorta(x.getString("fecha")),
                    fechaCorta(x.getString("caduca")), x.getString("estado")));
        }
        return c;
    }

    // (llamar con el monitor de this: usa el usuario de la sesión)
    private Percepcion percepcionDeJSON(JSONObject p) throws JSONException {
        long cuando;
        try {
            cuando = Instant.parse(p.getString("creada")).toEpochMilli();
        } catch (Exception e) {
            cuando = System.currentTimeMillis() - p.optInt("hace_min", 0) * 60_000L;
        }
        String autor = p.optBoolean("mia", false) && usuario != null ? usuario.id : null;
        return new Percepcion(p.getLong("id"), p.getString("tipo"), p.getInt("nivel"), p.getDouble("lat"),
                p.getDouble("lng"), cuando, p.getString("direccion"), autor);
    }

    private List<Percepcion> percepcionesDeJSON(JSONArray a) throws JSONException {
        List<Percepcion> l = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            l.add(percepcionDeJSON(a.getJSONObject(i)));
        }
        return l;
    }

    // ==============================================================
    // Sensor y recorrido
    // ==============================================================

    // código del préstamo en curso de la cuenta (o null)
    public synchronized String sensorPrestado() {
        return sensorPrestado;
    }

    // fecha límite para devolver el sensor prestado (ms) o 0: 10 días desde la recogida
    public synchronized long prestamoHasta() {
        return prestamoHasta;
    }

    private static long msDeISO(String iso) {
        try {
            return java.time.Instant.parse(iso).toEpochMilli();
        } catch (Exception e) {
            return 0;
        }
    }

    // --------------------------------------------------------------
    // codigo, conRed --> comprobarSensor() --> código normalizado (por Hecho)
    // Primero lo que se puede ver en el móvil (vacío, formato, sin red);
    // luego pregunta al servidor si ese sensor es de quien tiene la sesión.
    // --------------------------------------------------------------
    public void comprobarSensor(String codigo, boolean conRed, Hecho<String> hecho) {
        String c = codigo.trim().toUpperCase(Locale.ROOT);
        String local = c.isEmpty() ? "Escribe el código que lleva impreso el sensor."
                : !Reglas.esCodigoSensor(c) ? "Formato no válido: es RSP- seguido de 4 letras o números. Ejemplo: RSP-4F7K."
                : !conRed ? "Sin conexión no podemos comprobar el código. Inténtalo cuando tengas internet."
                : !haySesion() ? "Inicia sesión para usar un sensor." : null;
        if (local != null) {
            Map<String, String> campos = new HashMap<>();
            campos.put("codigo", local);
            hiloPrincipal.post(() -> hecho.error(local, campos));
            return;
        }
        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put("codigo", c);
        } catch (JSONException ignorada) {
            // no pasa
        }
        api.pedir("POST", "/yo/sensor", cuerpo, token(), new ApiRespira.Respuesta() {
            @Override
            public void ok(String texto) {
                String cod = c;
                try {
                    cod = new JSONObject(texto).getString("codigo");
                } catch (JSONException ignorada) {
                    // se queda el que escribió
                }
                enPrincipal(hecho, cod);
            }

            @Override
            public void error(ApiRespira.Error e) {
                errorEnPrincipal(hecho, e);
            }
        });
    }

    public synchronized Recorrido recorrido() {
        return recorrido;
    }

    public void iniciarRecorrido(String codigo) {
        synchronized (this) {
            recorrido = new Recorrido(codigo.trim().toUpperCase(Locale.ROOT), hora(new Date()));
            ultimosValores.clear();
        }
        cambio();
    }

    // --------------------------------------------------------------
    // Llega una medición NUEVA del sensor (la llama el servicio BLE).
    // Los minutos y los puntos los cuenta el servidor: aquí solo se
    // lleva la cuenta de enviadas/pendientes y se pide el «hoy» al
    // servidor de vez en cuando.
    // --------------------------------------------------------------
    public void registrarMedicion(boolean enParque, boolean enviada) {
        synchronized (this) {
            if (recorrido == null || !recorrido.enCurso()) {
                return;
            }
            long ahora = System.currentTimeMillis();
            recorrido.estado = Recorrido.ACTIVO;
            recorrido.ultimaMs = ahora;
            recorrido.ultima = hora(new Date(ahora));
            if (enviada) {
                recorrido.enviadas++;
            } else {
                recorrido.pendientes++;
            }
        }
        refrescar(false);
        cambio();
    }

    public void terminarRecorrido() {
        synchronized (this) {
            if (recorrido != null) {
                recorrido.estado = Recorrido.TERMINADO;
            }
        }
        refrescar(true);
        cambio();
    }

    public void olvidarRecorrido() {
        synchronized (this) {
            recorrido = null;
        }
        cambio();
    }

    // {minutos válidos, en parques, descartados} de hoy (los calcula el servidor)
    public synchronized int[] hoy() {
        return cuenta == null ? new int[]{0, 0, 0} : new int[]{cuenta.hoyMin, cuenta.hoyMinParque, cuenta.hoyDescartados};
    }

    // ==============================================================
    // Puntos, regalos y canjes
    // ==============================================================
    public synchronized Cuenta cuenta() {
        if (usuario == null) {
            return null;
        }
        if (cuenta == null) {
            cuenta = new Cuenta();
        }
        return cuenta;
    }

    public synchronized List<Regalo> regalos() {
        return new ArrayList<>(regalos);
    }

    // --------------------------------------------------------------
    // regaloId --> canjear() --> Canje (por Hecho). El servidor comprueba
    // el saldo y las existencias y resta los puntos.
    // --------------------------------------------------------------
    public void canjear(String regaloId, Hecho<Canje> hecho) {
        JSONObject c = new JSONObject();
        try {
            c.put("regalo", regaloId);
        } catch (JSONException ignorada) {
            // no pasa
        }
        api.pedir("POST", "/canjes", c, token(), new ApiRespira.Respuesta() {
            @Override
            public void ok(String cuerpo) {
                try {
                    JSONObject x = new JSONObject(cuerpo);
                    Canje k = new Canje(x.getString("codigo"), x.getString("regalo"), fechaCorta(x.getString("fecha")),
                            fechaCorta(x.getString("caduca")), x.getString("estado"));
                    refrescar(true);
                    enPrincipal(hecho, k);
                } catch (JSONException e) {
                    errorEnPrincipal(hecho, ApiRespira.error(500, ""));
                }
            }

            @Override
            public void error(ApiRespira.Error e) {
                refrescar(true);
                errorEnPrincipal(hecho, e);
            }
        });
    }

    // ==============================================================
    // Percepciones de los vecinos
    // ==============================================================
    public synchronized List<Percepcion> percepciones() {
        long limite = System.currentTimeMillis() - 24 * 3_600_000L;
        List<Percepcion> l = new ArrayList<>();
        for (Percepcion p : percepciones) {
            if (p.cuando >= limite) {
                l.add(p);
            }
        }
        return l;
    }

    public synchronized List<Percepcion> misPercepciones() {
        return usuario == null ? new ArrayList<>() : new ArrayList<>(mias);
    }

    public synchronized Percepcion percepcion(long id) {
        for (Percepcion p : percepciones) {
            if (p.id == id) {
                return p;
            }
        }
        for (Percepcion p : mias) {
            if (p.id == id) {
                return p;
            }
        }
        return null;
    }

    // Solo quien la publicó puede editarla o borrarla
    public synchronized boolean esMia(Percepcion p) {
        return p != null && usuario != null && usuario.id.equals(p.autorId);
    }

    private ApiRespira.Respuesta respuestaPercepcion(Hecho<Percepcion> hecho) {
        return new ApiRespira.Respuesta() {
            @Override
            public void ok(String cuerpo) {
                try {
                    Percepcion p;
                    synchronized (Repositorio.this) {
                        p = percepcionDeJSON(new JSONObject(cuerpo));
                    }
                    refrescar(true);
                    enPrincipal(hecho, p);
                } catch (JSONException e) {
                    errorEnPrincipal(hecho, ApiRespira.error(500, ""));
                }
            }

            @Override
            public void error(ApiRespira.Error e) {
                errorEnPrincipal(hecho, e);
            }
        };
    }

    // --------------------------------------------------------------
    // tipo, nivel, lat, lng, direccion --> nuevaPercepcion() --> Percepcion
    // El servidor limita a 3 por hora y cuenta.
    // --------------------------------------------------------------
    public void nuevaPercepcion(String tipo, int nivel, double lat, double lng, String direccion, Hecho<Percepcion> hecho) {
        JSONObject c = new JSONObject();
        try {
            c.put("tipo", tipo).put("nivel", nivel).put("lat", lat).put("lng", lng).put("direccion", direccion);
        } catch (JSONException ignorada) {
            // no pasa
        }
        api.pedir("POST", "/percepciones", c, token(), respuestaPercepcion(hecho));
    }

    // Cambia tipo y nivel; conserva lugar, hora y autor. Solo la propia.
    public void editarPercepcion(long id, String tipo, int nivel, Hecho<Percepcion> hecho) {
        JSONObject c = new JSONObject();
        try {
            c.put("tipo", tipo).put("nivel", nivel);
        } catch (JSONException ignorada) {
            // no pasa
        }
        api.pedir("PUT", "/percepciones/" + id, c, token(), respuestaPercepcion(hecho));
    }

    public void borrarPercepcion(long id, Hecho<Void> hecho) {
        api.pedir("DELETE", "/percepciones/" + id, null, token(), new ApiRespira.Respuesta() {
            @Override
            public void ok(String cuerpo) {
                synchronized (Repositorio.this) {
                    percepciones.removeIf(p -> p.id == id);
                    mias.removeIf(p -> p.id == id);
                }
                cambio();
                refrescar(true);
                enPrincipal(hecho, null);
            }

            @Override
            public void error(ApiRespira.Error e) {
                errorEnPrincipal(hecho, e);
            }
        });
    }

    // ==============================================================
    // Borrar lo guardado en este móvil (favoritos, alertas, sesión…)
    // ==============================================================
    public void olvidarEsteMovil() {
        cerrarSesion();
        synchronized (this) {
            favoritos.clear();
            alertas.clear();
            alertaAvisada.clear();
            onboardingVisto = false;
            bloqueoHasta = 0;
            percepciones.clear();
            regalos.clear();
        }
        cambio();
    }

    // ==============================================================
    // Utilidades privadas
    // ==============================================================
    private static String hora(Date d) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(d);
    }

    // ---------- guardar / cargar (SharedPreferences, JSON) ----------
    private synchronized void guardar() {
        try {
            JSONObject o = new JSONObject();
            o.put("token", token == null ? JSONObject.NULL : token);
            if (usuario != null) {
                o.put("usuario", new JSONObject().put("id", usuario.id).put("nombre", usuario.nombre)
                        .put("email", usuario.email).put("rol", usuario.rol));
            }
            if (cuenta != null) {
                JSONArray h = new JSONArray();
                for (Movimiento m : cuenta.historial) {
                    h.put(new JSONObject().put("fecha", m.fecha).put("min", m.minutos).put("minParque", m.minutosParque)
                            .put("desc", m.descartados).put("pts", m.puntos)
                            .put("canje", m.canje == null ? JSONObject.NULL : m.canje)
                            .put("nota", m.nota == null ? JSONObject.NULL : m.nota)
                            .put("rec", m.recorrido == null ? JSONObject.NULL : m.recorrido.json()));
                }
                JSONArray cj = new JSONArray();
                for (Canje k : cuenta.canjes) {
                    cj.put(new JSONObject().put("codigo", k.codigo).put("regalo", k.regalo)
                            .put("fecha", k.fecha).put("caduca", k.caduca).put("estado", k.estado));
                }
                o.put("cuenta", new JSONObject().put("puntos", cuenta.puntos).put("historial", h).put("canjes", cj)
                        .put("hoy", new JSONArray().put(cuenta.hoyMin).put(cuenta.hoyMinParque)
                                .put(cuenta.hoyDescartados).put(cuenta.hoyPuntos))
                        .put("reglas", new JSONArray().put(cuenta.minPorPunto).put(cuenta.multParque).put(cuenta.maxDia))
                        .put("hoyRec", cuenta.hoyRecorrido == null ? JSONObject.NULL : cuenta.hoyRecorrido.json()));
            }
            o.put("sensor", sensorPrestado == null ? JSONObject.NULL : sensorPrestado);
            o.put("sensorHasta", prestamoHasta);
            JSONArray rg = new JSONArray();
            for (Regalo g : regalos) {
                rg.put(new JSONObject().put("id", g.id).put("nombre", g.nombre).put("coste", g.coste).put("stock", g.stock));
            }
            o.put("regalos", rg);
            o.put("favoritos", new JSONArray(favoritos));
            JSONArray al = new JSONArray();
            for (Alerta a : alertas.values()) {
                al.put(new JSONObject().put("zona", a.zonaId).put("mag", a.magnitud.id)
                        .put("cond", a.condicion).put("valor", a.valor == null ? "" : a.valor));
            }
            o.put("alertas", al);
            JSONObject av = new JSONObject();
            for (Map.Entry<String, Long> e : alertaAvisada.entrySet()) {
                av.put(e.getKey(), e.getValue());
            }
            o.put("alertaAvisada", av);
            o.put("onboarding", onboardingVisto);
            o.put("bloqueo", bloqueoHasta);
            o.put("segPendiente", seguimientoPendiente);
            o.put("segFusionar", fusionarSeguimiento);
            if (recorrido != null) {
                o.put("recorrido", new JSONObject().put("codigo", recorrido.codigo).put("inicio", recorrido.inicio)
                        .put("estado", recorrido.estado).put("enviadas", recorrido.enviadas)
                        .put("pendientes", recorrido.pendientes)
                        .put("ultima", recorrido.ultima == null ? "" : recorrido.ultima)
                        .put("ultimaMs", recorrido.ultimaMs));
            }
            prefs.edit().putString(CLAVE, o.toString()).apply();
        } catch (JSONException e) {
            // No debería pasar: solo se meten tipos básicos
        }
    }

    private void cargar(JSONObject o) throws JSONException {
        token = o.isNull("token") ? null : o.getString("token");
        JSONObject u = o.optJSONObject("usuario");
        usuario = u == null ? null : usuarioDeJSON(u);
        JSONObject c = o.optJSONObject("cuenta");
        if (c != null) {
            cuenta = new Cuenta();
            cuenta.puntos = c.getInt("puntos");
            JSONArray h = c.getJSONArray("historial");
            for (int i = 0; i < h.length(); i++) {
                JSONObject m = h.getJSONObject(i);
                cuenta.historial.add(new Movimiento(m.getString("fecha"), m.getInt("min"), m.getInt("minParque"),
                        m.getInt("desc"), m.getInt("pts"), m.isNull("canje") ? null : m.getString("canje"),
                        m.isNull("nota") ? null : m.getString("nota"),
                        ResumenRecorrido.deJSON(m.optJSONObject("rec"))));
            }
            JSONArray cj = c.getJSONArray("canjes");
            for (int i = 0; i < cj.length(); i++) {
                JSONObject x = cj.getJSONObject(i);
                cuenta.canjes.add(new Canje(x.getString("codigo"), x.getString("regalo"), x.getString("fecha"),
                        x.getString("caduca"), x.getString("estado")));
            }
            JSONArray hoy = c.getJSONArray("hoy");
            cuenta.hoyMin = hoy.getInt(0);
            cuenta.hoyMinParque = hoy.getInt(1);
            cuenta.hoyDescartados = hoy.getInt(2);
            cuenta.hoyPuntos = hoy.getInt(3);
            cuenta.hoyRecorrido = ResumenRecorrido.deJSON(c.optJSONObject("hoyRec"));
            JSONArray r = c.getJSONArray("reglas");
            cuenta.minPorPunto = r.getInt(0);
            cuenta.multParque = r.getInt(1);
            cuenta.maxDia = r.getInt(2);
        }
        sensorPrestado = o.isNull("sensor") ? null : o.getString("sensor");
        prestamoHasta = o.optLong("sensorHasta", 0);
        JSONArray rg = o.getJSONArray("regalos");
        for (int i = 0; i < rg.length(); i++) {
            JSONObject g = rg.getJSONObject(i);
            regalos.add(new Regalo(g.getString("id"), g.getString("nombre"), g.getInt("coste"), g.getInt("stock")));
        }
        cargarLocales(o);
        JSONObject av = o.optJSONObject("alertaAvisada");
        if (av != null) {
            JSONArray k = av.names();
            for (int i = 0; k != null && i < k.length(); i++) {
                alertaAvisada.put(k.getString(i), av.getLong(k.getString(i)));
            }
        }
        bloqueoHasta = o.optLong("bloqueo", 0);
        seguimientoPendiente = o.optBoolean("segPendiente", false);
        fusionarSeguimiento = o.optBoolean("segFusionar", false);
        if (o.has("recorrido")) {
            JSONObject r = o.getJSONObject("recorrido");
            recorrido = new Recorrido(r.getString("codigo"), r.getString("inicio"));
            recorrido.estado = r.getString("estado");
            recorrido.enviadas = r.getInt("enviadas");
            recorrido.pendientes = r.getInt("pendientes");
            recorrido.ultima = r.getString("ultima").isEmpty() ? null : r.getString("ultima");
            recorrido.ultimaMs = r.getLong("ultimaMs");
        }
    }

    // favoritos, alertas y bienvenida (iguales en la versión 1 y la 2)
    private void cargarLocales(JSONObject o) throws JSONException {
        JSONArray fv = o.optJSONArray("favoritos");
        for (int i = 0; fv != null && i < fv.length(); i++) {
            favoritos.add(fv.getString(i));
        }
        JSONArray al = o.optJSONArray("alertas");
        for (int i = 0; al != null && i < al.length(); i++) {
            JSONObject a = al.getJSONObject(i);
            alertas.put(a.getString("zona"), new Alerta(a.getString("zona"), Magnitud.desdeId(a.getString("mag")),
                    a.getString("cond"), a.getString("valor")));
        }
        onboardingVisto = o.optBoolean("onboarding", false);
    }

    // Versión 1 (datos de demostración en el móvil): solo se conservan
    // favoritos, alertas y la bienvenida. Las cuentas ahora son del servidor.
    private void cargarV1(JSONObject o) throws JSONException {
        cargarLocales(o);
        prefs.edit().remove(CLAVE_V1).apply();
        guardar();
    }
}
