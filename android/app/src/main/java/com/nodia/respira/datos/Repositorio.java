package com.nodia.respira.datos;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// ==============================================================
// Repositorio: único punto de acceso a los datos de la app.
//
// HOY: todo es de demostración y se guarda en SharedPreferences
// (como JSON), igual que el prototipo. Las cuentas, préstamos,
// puntos y canjes "simulan" lo que en la versión real estará en el
// servidor. Cuando exista la API, solo cambia ESTA clase: las
// pantallas no saben de dónde salen los datos.
// ==============================================================
public final class Repositorio {

    // ---------- tipos auxiliares ----------
    public static class Cuenta {
        public int puntos;
        public final List<Movimiento> historial = new ArrayList<>();
        public final List<Canje> canjes = new ArrayList<>();
    }

    public static class Prestamo {
        public final String usuarioId;
        public final String estado; // prestado | devuelto | disponible

        Prestamo(String usuarioId, String estado) {
            this.usuarioId = usuarioId;
            this.estado = estado;
        }
    }

    public static class ResultadoLogin {
        public final Usuario usuario;
        public final String error;

        ResultadoLogin(Usuario usuario, String error) {
            this.usuario = usuario;
            this.error = error;
        }
    }

    private static final String PREFS = "respira";
    private static final String CLAVE = "estado_v1";
    private static Repositorio instancia;

    private final SharedPreferences prefs;
    private final Handler hiloPrincipal = new Handler(Looper.getMainLooper());
    private final List<Runnable> observadores = new ArrayList<>();
    private final SecureRandom azar = new SecureRandom();

    private final List<Zona> zonas = DatosDemo.zonas();

    // "servidor" simulado
    private final List<Usuario> usuarios = new ArrayList<>();
    private final Map<String, Prestamo> prestamos = new LinkedHashMap<>();
    private final List<Regalo> regalos = new ArrayList<>();
    private final Map<String, Cuenta> cuentas = new HashMap<>();
    private final List<Percepcion> percepciones = new ArrayList<>();

    // solo en este móvil
    private final Set<String> favoritos = new LinkedHashSet<>();
    private final Map<String, Alerta> alertas = new LinkedHashMap<>();
    private String sesionId;
    private boolean onboardingVisto;
    private int intentosFallidos;
    private long bloqueoHasta;
    private Recorrido recorrido;
    private int hoyMin, hoyMinParque, hoyDescartados;

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
        boolean cargado = false;
        if (json != null) {
            try {
                cargar(new JSONObject(json));
                cargado = true;
            } catch (JSONException e) {
                limpiar();
            }
        }
        if (!cargado) {
            sembrarDemo();
            guardar();
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

    // ==============================================================
    // Zonas, favoritos y alertas
    // ==============================================================
    public List<Zona> zonas() {
        return Collections.unmodifiableList(zonas);
    }

    public List<Zona> parques() {
        List<Zona> p = new ArrayList<>();
        for (Zona z : zonas) {
            if (z.esParque()) {
                p.add(z);
            }
        }
        return p;
    }

    public Zona zona(String id) {
        for (Zona z : zonas) {
            if (z.id.equals(id)) {
                return z;
            }
        }
        return null;
    }

    // Parque (no barrio) que contiene el punto, o null
    public Zona parqueEn(double lat, double lng) {
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
            }
        }
        cambio();
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
            // Una alerta solo tiene sentido en un parque que sigues
            favoritos.add(a.zonaId);
        }
        cambio();
    }

    public void borrarAlerta(String zonaId) {
        synchronized (this) {
            alertas.remove(zonaId);
        }
        cambio();
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
    // Cuenta de usuario
    // ==============================================================
    public synchronized Usuario usuario() {
        if (sesionId == null) {
            return null;
        }
        for (Usuario u : usuarios) {
            if (u.id.equals(sesionId)) {
                return u;
            }
        }
        return null;
    }

    public boolean haySesion() {
        return usuario() != null;
    }

    public synchronized long bloqueadoHasta() {
        return bloqueoHasta > System.currentTimeMillis() ? bloqueoHasta : 0;
    }

    // --------------------------------------------------------------
    // email, clave: Text --> iniciarSesion() --> ResultadoLogin
    // --------------------------------------------------------------
    public ResultadoLogin iniciarSesion(String email, String clave) {
        ResultadoLogin r;
        synchronized (this) {
            if (bloqueadoHasta() > 0) {
                return new ResultadoLogin(null,
                        "Has fallado " + Reglas.MAX_INTENTOS_LOGIN + " veces. Por seguridad, el acceso queda bloqueado 15 minutos.");
            }
            Usuario encontrado = null;
            String m = email.trim().toLowerCase(Locale.ROOT);
            for (Usuario u : usuarios) {
                if (u.email.equals(m) && u.clave.equals(clave)) {
                    encontrado = u;
                }
            }
            if (encontrado == null) {
                intentosFallidos++;
                int quedan = Reglas.MAX_INTENTOS_LOGIN - intentosFallidos;
                if (quedan <= 0) {
                    bloqueoHasta = System.currentTimeMillis() + Reglas.BLOQUEO_LOGIN_MS;
                    intentosFallidos = 0;
                    r = new ResultadoLogin(null,
                            "Has fallado " + Reglas.MAX_INTENTOS_LOGIN + " veces. Por seguridad, el acceso queda bloqueado 15 minutos.");
                } else {
                    r = new ResultadoLogin(null, "Correo o contraseña incorrectos. Te quedan "
                            + quedan + (quedan == 1 ? " intento." : " intentos."));
                }
            } else {
                intentosFallidos = 0;
                sesionId = encontrado.id;
                hoyMin = hoyMinParque = hoyDescartados = 0;
                r = new ResultadoLogin(encontrado, null);
            }
        }
        cambio();
        return r;
    }

    // --------------------------------------------------------------
    // nombre, email, clave: Text --> registrar() --> {campo: error} (vacío = OK)
    // --------------------------------------------------------------
    public Map<String, String> registrar(String nombre, String email, String clave, boolean acepta) {
        Map<String, String> err = new HashMap<>();
        String n = nombre.trim();
        String m = email.trim().toLowerCase(Locale.ROOT);
        synchronized (this) {
            if (n.isEmpty()) {
                err.put("nombre", "Elige un nombre de usuario.");
            } else if (!Reglas.esNombreUsuario(n)) {
                err.put("nombre", "Usa de 3 a 20 letras, números, puntos o guiones bajos, sin espacios.");
            } else {
                for (Usuario u : usuarios) {
                    if (u.nombre.equalsIgnoreCase(n)) {
                        err.put("nombre", "Ese nombre de usuario ya existe. Prueba con otro.");
                    }
                }
            }
            if (m.isEmpty()) {
                err.put("email", "Escribe tu correo.");
            } else if (!Reglas.esEmail(m)) {
                err.put("email", "El correo no tiene un formato válido. Ejemplo: nombre@correo.es");
            } else {
                for (Usuario u : usuarios) {
                    if (u.email.equals(m)) {
                        err.put("email", "Ya hay una cuenta con este correo. Inicia sesión o recupera la contraseña.");
                    }
                }
            }
            if (clave.isEmpty()) {
                err.put("clave", "Elige una contraseña.");
            } else if (clave.length() < 8) {
                err.put("clave", "La contraseña debe tener al menos 8 caracteres.");
            }
            if (!acepta) {
                err.put("acepta", "Tienes que aceptar los términos para crear la cuenta.");
            }
            if (!err.isEmpty()) {
                return err;
            }
            String id;
            do {
                id = "U-" + (10000 + azar.nextInt(89999));
            } while (buscarUsuarioPorId(id) != null);
            usuarios.add(new Usuario(id, n, m, clave));
            cuentas.put(id, new Cuenta());
            sesionId = id;
            hoyMin = hoyMinParque = hoyDescartados = 0;
        }
        cambio();
        return err;
    }

    private Usuario buscarUsuarioPorId(String id) {
        for (Usuario u : usuarios) {
            if (u.id.equals(id)) {
                return u;
            }
        }
        return null;
    }

    public void cerrarSesion() {
        synchronized (this) {
            sesionId = null;
            recorrido = null;
            hoyMin = hoyMinParque = hoyDescartados = 0;
        }
        cambio();
    }

    public void borrarCuenta() {
        synchronized (this) {
            Usuario u = usuario();
            if (u == null) {
                return;
            }
            Iterator<Percepcion> it = percepciones.iterator();
            while (it.hasNext()) {
                if (u.id.equals(it.next().autorId)) {
                    it.remove();
                }
            }
            usuarios.remove(u);
            cuentas.remove(u.id);
            sesionId = null;
            recorrido = null;
        }
        cambio();
    }

    // ==============================================================
    // Sensor (préstamo por código RSP-XXXX)
    // ==============================================================

    // --------------------------------------------------------------
    // codigo: Text, conRed: B --> errorCodigo() --> Text | null
    // --------------------------------------------------------------
    public synchronized String errorCodigo(String codigo, boolean conRed) {
        String c = codigo.trim().toUpperCase(Locale.ROOT);
        Usuario u = usuario();
        if (c.isEmpty()) {
            return "Escribe el código que lleva impreso el sensor.";
        }
        if (!Reglas.esCodigoSensor(c)) {
            return "Formato no válido: es RSP- seguido de 4 letras o números. Ejemplo: RSP-4F7K.";
        }
        if (!conRed) {
            return "Sin conexión no podemos comprobar el código. Inténtalo cuando tengas internet.";
        }
        Prestamo p = prestamos.get(c);
        if (p == null) {
            return "No existe ningún sensor con el código " + c + ". Revisa que lo has copiado bien.";
        }
        if ("devuelto".equals(p.estado)) {
            return "El sensor " + c + " ya se devolvió. Pide uno nuevo en el punto de reparto.";
        }
        if ("disponible".equals(p.estado)) {
            return "El sensor " + c + " no está prestado a nadie. Pide en el punto de reparto que lo registren con tu ID "
                    + (u == null ? "" : u.id) + ".";
        }
        if (u == null || !u.id.equals(p.usuarioId)) {
            return "El sensor " + c + " está prestado a otra persona. Comprueba el código o pregunta en el punto de reparto.";
        }
        return null;
    }

    public synchronized Recorrido recorrido() {
        return recorrido;
    }

    public void iniciarRecorrido(String codigo) {
        synchronized (this) {
            recorrido = new Recorrido(codigo.trim().toUpperCase(Locale.ROOT), hora(new Date()));
        }
        cambio();
    }

    // --------------------------------------------------------------
    // Llega una medición NUEVA del sensor (la llama el servicio BLE).
    // enParque: B, enviada: B
    // Minutos válidos: el tiempo desde la anterior, si son <= 15 min
    // (si el sensor estuvo parado o lejos, ese rato no cuenta).
    // --------------------------------------------------------------
    public void registrarMedicion(boolean enParque, boolean enviada) {
        synchronized (this) {
            if (recorrido == null || !recorrido.enCurso()) {
                return;
            }
            long ahora = System.currentTimeMillis();
            if (recorrido.ultimaMs > 0) {
                int min = (int) ((ahora - recorrido.ultimaMs) / 60_000L);
                if (min <= 15) {
                    hoyMin += min;
                    if (enParque) {
                        hoyMinParque += min;
                    }
                } else {
                    hoyDescartados += min;
                }
            }
            recorrido.estado = Recorrido.ACTIVO;
            recorrido.ultimaMs = ahora;
            recorrido.ultima = hora(new Date(ahora));
            if (enviada) {
                recorrido.enviadas++;
            } else {
                recorrido.pendientes++;
            }
        }
        cambio();
    }

    public void terminarRecorrido() {
        synchronized (this) {
            if (recorrido != null) {
                recorrido.estado = Recorrido.TERMINADO;
            }
        }
        cambio();
    }

    public void olvidarRecorrido() {
        synchronized (this) {
            recorrido = null;
        }
        cambio();
    }

    public synchronized int[] hoy() {
        return new int[]{hoyMin, hoyMinParque, hoyDescartados};
    }

    // ==============================================================
    // Puntos, regalos y canjes
    // ==============================================================
    public synchronized Cuenta cuenta() {
        Usuario u = usuario();
        if (u == null) {
            return null;
        }
        Cuenta c = cuentas.get(u.id);
        if (c == null) {
            c = new Cuenta();
            cuentas.put(u.id, c);
        }
        return c;
    }

    public synchronized List<Regalo> regalos() {
        return new ArrayList<>(regalos);
    }

    // --------------------------------------------------------------
    // regaloId: Text --> canjear() --> Canje | null (sin saldo o sin stock)
    // --------------------------------------------------------------
    public Canje canjear(String regaloId) {
        Canje canje = null;
        synchronized (this) {
            Cuenta c = cuenta();
            Regalo g = null;
            for (Regalo r : regalos) {
                if (r.id.equals(regaloId)) {
                    g = r;
                }
            }
            if (c == null || g == null || g.stock <= 0 || c.puntos < g.coste) {
                return null;
            }
            c.puntos -= g.coste;
            g.stock--;
            Date hoy = new Date();
            canje = new Canje(nuevoCodigo("CJ-"), g.nombre, fecha(hoy), fecha(sumarDias(hoy, 7)), "pendiente");
            c.canjes.add(0, canje);
            c.historial.add(0, new Movimiento(fecha(hoy), 0, 0, 0, -g.coste, g.nombre));
        }
        cambio();
        return canje;
    }

    // --------------------------------------------------------------
    // Cierre del día (lo hará el servidor cada noche).
    // cerrarDia() --> puntos ganados | -1 si no hay minutos válidos
    // --------------------------------------------------------------
    public int cerrarDia() {
        int pts;
        synchronized (this) {
            Cuenta c = cuenta();
            if (c == null || hoyMin == 0) {
                return -1;
            }
            pts = Reglas.puntosDelDia(hoyMin, hoyMinParque);
            c.puntos += pts;
            c.historial.add(0, new Movimiento(fecha(new Date()), hoyMin, hoyMinParque, hoyDescartados, pts, null));
            hoyMin = hoyMinParque = hoyDescartados = 0;
        }
        cambio();
        return pts;
    }

    // Demo: el punto de reparto presta RSP-9C2D a la cuenta abierta
    public String simularPrestamo() {
        String codigo = "RSP-9C2D";
        synchronized (this) {
            Usuario u = usuario();
            if (u == null) {
                return null;
            }
            prestamos.put(codigo, new Prestamo(u.id, "prestado"));
        }
        cambio();
        return codigo;
    }

    // Demo: suma minutos como si se hubiera paseado con el sensor
    public void simularPaseo(int minutos, int enParque) {
        synchronized (this) {
            hoyMin += minutos;
            hoyMinParque += enParque;
            if (recorrido != null && recorrido.enCurso()) {
                recorrido.estado = Recorrido.ACTIVO;
                recorrido.enviadas += minutos / 2;
                recorrido.ultima = hora(new Date());
            }
        }
        cambio();
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
        Usuario u = usuario();
        List<Percepcion> l = new ArrayList<>();
        if (u != null) {
            for (Percepcion p : percepciones) {
                if (u.id.equals(p.autorId)) {
                    l.add(0, p);
                }
            }
        }
        return l;
    }

    public void nuevaPercepcion(String tipo, int nivel, double lat, double lng, String direccion) {
        synchronized (this) {
            Usuario u = usuario();
            if (u == null) {
                return;
            }
            long id = System.currentTimeMillis();
            percepciones.add(new Percepcion(id, tipo, nivel, lat, lng, id, direccion, u.id));
        }
        cambio();
    }

    public void borrarPercepcion(long id) {
        synchronized (this) {
            Iterator<Percepcion> it = percepciones.iterator();
            while (it.hasNext()) {
                if (it.next().id == id) {
                    it.remove();
                }
            }
        }
        cambio();
    }

    // ==============================================================
    // Demo: volver a empezar
    // ==============================================================
    public void reiniciarDemo() {
        synchronized (this) {
            limpiar();
            sembrarDemo();
        }
        cambio();
    }

    // ==============================================================
    // Utilidades privadas
    // ==============================================================
    private String nuevoCodigo(String prefijo) {
        String abc = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(prefijo);
        for (int i = 0; i < 4; i++) {
            sb.append(abc.charAt(azar.nextInt(abc.length())));
        }
        return sb.toString();
    }

    private static String hora(Date d) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(d);
    }

    private static String fecha(Date d) {
        return new SimpleDateFormat("d MMM", new Locale("es", "ES")).format(d);
    }

    private static Date sumarDias(Date d, int dias) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.add(Calendar.DAY_OF_YEAR, dias);
        return c.getTime();
    }

    private void limpiar() {
        usuarios.clear();
        prestamos.clear();
        regalos.clear();
        cuentas.clear();
        percepciones.clear();
        favoritos.clear();
        alertas.clear();
        sesionId = null;
        onboardingVisto = false;
        intentosFallidos = 0;
        bloqueoHasta = 0;
        recorrido = null;
        hoyMin = hoyMinParque = hoyDescartados = 0;
    }

    private void sembrarDemo() {
        usuarios.add(new Usuario("U-48213", "lucia", "lucia@correo.es", "Respira2026"));
        usuarios.add(new Usuario("U-11902", "marcos", "marcos@correo.es", "Marcos2026"));

        prestamos.put("RSP-4F7K", new Prestamo("U-48213", "prestado"));
        prestamos.put("RSP-2B9Q", new Prestamo("U-11902", "prestado"));
        prestamos.put("RSP-7H3M", new Prestamo("U-48213", "devuelto"));
        prestamos.put("RSP-5K8P", new Prestamo(null, "disponible"));

        regalos.add(new Regalo("g1", "Bolsa de tela", 100, 0));
        regalos.add(new Regalo("g2", "Gorra Respira", 150, 12));
        regalos.add(new Regalo("g3", "Cantimplora", 250, 8));
        regalos.add(new Regalo("g4", "Mochila del ayuntamiento", 600, 3));

        Cuenta lucia = new Cuenta();
        lucia.puntos = 340;
        lucia.historial.add(new Movimiento("1 nov", 120, 40, 12, 32, null));
        lucia.historial.add(new Movimiento("31 oct", 310, 150, 0, 91, null));
        lucia.historial.add(new Movimiento("30 oct", 0, 0, 0, -150, "Gorra Respira"));
        lucia.historial.add(new Movimiento("28 oct", 95, 20, 30, 23, null));
        lucia.canjes.add(new Canje("CJ-8R2T", "Gorra Respira", "30 oct", "6 nov", "pendiente"));
        cuentas.put("U-48213", lucia);

        Cuenta marcos = new Cuenta();
        marcos.puntos = 120;
        marcos.historial.add(new Movimiento("1 nov", 180, 60, 0, 48, null));
        marcos.historial.add(new Movimiento("31 oct", 240, 0, 0, 72, null));
        cuentas.put("U-11902", marcos);

        percepciones.addAll(DatosDemo.percepciones(System.currentTimeMillis()));
    }

    // ---------- JSON ----------
    private synchronized void guardar() {
        try {
            JSONObject o = new JSONObject();
            JSONArray us = new JSONArray();
            for (Usuario u : usuarios) {
                us.put(new JSONObject().put("id", u.id).put("nombre", u.nombre)
                        .put("email", u.email).put("clave", u.clave));
            }
            o.put("usuarios", us);

            JSONObject pr = new JSONObject();
            for (Map.Entry<String, Prestamo> e : prestamos.entrySet()) {
                pr.put(e.getKey(), new JSONObject()
                        .put("usuario", e.getValue().usuarioId == null ? JSONObject.NULL : e.getValue().usuarioId)
                        .put("estado", e.getValue().estado));
            }
            o.put("prestamos", pr);

            JSONArray rg = new JSONArray();
            for (Regalo g : regalos) {
                rg.put(new JSONObject().put("id", g.id).put("nombre", g.nombre)
                        .put("coste", g.coste).put("stock", g.stock));
            }
            o.put("regalos", rg);

            JSONObject cs = new JSONObject();
            for (Map.Entry<String, Cuenta> e : cuentas.entrySet()) {
                Cuenta c = e.getValue();
                JSONArray h = new JSONArray();
                for (Movimiento m : c.historial) {
                    h.put(new JSONObject().put("fecha", m.fecha).put("min", m.minutos)
                            .put("minParque", m.minutosParque).put("desc", m.descartados)
                            .put("pts", m.puntos).put("canje", m.canje == null ? JSONObject.NULL : m.canje));
                }
                JSONArray cj = new JSONArray();
                for (Canje k : c.canjes) {
                    cj.put(new JSONObject().put("codigo", k.codigo).put("regalo", k.regalo)
                            .put("fecha", k.fecha).put("caduca", k.caduca).put("estado", k.estado));
                }
                cs.put(e.getKey(), new JSONObject().put("puntos", c.puntos).put("historial", h).put("canjes", cj));
            }
            o.put("cuentas", cs);

            JSONArray pc = new JSONArray();
            for (Percepcion p : percepciones) {
                pc.put(new JSONObject().put("id", p.id).put("tipo", p.tipo).put("nivel", p.nivel)
                        .put("lat", p.lat).put("lng", p.lng).put("cuando", p.cuando)
                        .put("dir", p.direccion).put("autor", p.autorId == null ? JSONObject.NULL : p.autorId));
            }
            o.put("percepciones", pc);

            o.put("favoritos", new JSONArray(favoritos));
            JSONArray al = new JSONArray();
            for (Alerta a : alertas.values()) {
                al.put(new JSONObject().put("zona", a.zonaId).put("mag", a.magnitud.id)
                        .put("cond", a.condicion).put("valor", a.valor == null ? "" : a.valor));
            }
            o.put("alertas", al);

            o.put("sesion", sesionId == null ? JSONObject.NULL : sesionId);
            o.put("onboarding", onboardingVisto);
            o.put("intentos", intentosFallidos);
            o.put("bloqueo", bloqueoHasta);
            o.put("hoy", new JSONArray().put(hoyMin).put(hoyMinParque).put(hoyDescartados));
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
        limpiar();
        JSONArray us = o.getJSONArray("usuarios");
        for (int i = 0; i < us.length(); i++) {
            JSONObject u = us.getJSONObject(i);
            usuarios.add(new Usuario(u.getString("id"), u.getString("nombre"), u.getString("email"), u.getString("clave")));
        }
        JSONObject pr = o.getJSONObject("prestamos");
        Iterator<String> claves = pr.keys();
        while (claves.hasNext()) {
            String k = claves.next();
            JSONObject p = pr.getJSONObject(k);
            prestamos.put(k, new Prestamo(p.isNull("usuario") ? null : p.getString("usuario"), p.getString("estado")));
        }
        JSONArray rg = o.getJSONArray("regalos");
        for (int i = 0; i < rg.length(); i++) {
            JSONObject g = rg.getJSONObject(i);
            regalos.add(new Regalo(g.getString("id"), g.getString("nombre"), g.getInt("coste"), g.getInt("stock")));
        }
        JSONObject cs = o.getJSONObject("cuentas");
        claves = cs.keys();
        while (claves.hasNext()) {
            String k = claves.next();
            JSONObject c = cs.getJSONObject(k);
            Cuenta cuenta = new Cuenta();
            cuenta.puntos = c.getInt("puntos");
            JSONArray h = c.getJSONArray("historial");
            for (int i = 0; i < h.length(); i++) {
                JSONObject m = h.getJSONObject(i);
                cuenta.historial.add(new Movimiento(m.getString("fecha"), m.getInt("min"), m.getInt("minParque"),
                        m.getInt("desc"), m.getInt("pts"), m.isNull("canje") ? null : m.getString("canje")));
            }
            JSONArray cj = c.getJSONArray("canjes");
            for (int i = 0; i < cj.length(); i++) {
                JSONObject x = cj.getJSONObject(i);
                cuenta.canjes.add(new Canje(x.getString("codigo"), x.getString("regalo"), x.getString("fecha"),
                        x.getString("caduca"), x.getString("estado")));
            }
            cuentas.put(k, cuenta);
        }
        JSONArray pc = o.getJSONArray("percepciones");
        for (int i = 0; i < pc.length(); i++) {
            JSONObject p = pc.getJSONObject(i);
            percepciones.add(new Percepcion(p.getLong("id"), p.getString("tipo"), p.getInt("nivel"),
                    p.getDouble("lat"), p.getDouble("lng"), p.getLong("cuando"), p.getString("dir"),
                    p.isNull("autor") ? null : p.getString("autor")));
        }
        JSONArray fv = o.getJSONArray("favoritos");
        for (int i = 0; i < fv.length(); i++) {
            favoritos.add(fv.getString(i));
        }
        JSONArray al = o.getJSONArray("alertas");
        for (int i = 0; i < al.length(); i++) {
            JSONObject a = al.getJSONObject(i);
            alertas.put(a.getString("zona"), new Alerta(a.getString("zona"), Magnitud.desdeId(a.getString("mag")),
                    a.getString("cond"), a.getString("valor")));
        }
        sesionId = o.isNull("sesion") ? null : o.getString("sesion");
        onboardingVisto = o.getBoolean("onboarding");
        intentosFallidos = o.getInt("intentos");
        bloqueoHasta = o.getLong("bloqueo");
        JSONArray hoy = o.getJSONArray("hoy");
        hoyMin = hoy.getInt(0);
        hoyMinParque = hoy.getInt(1);
        hoyDescartados = hoy.getInt(2);
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
}
