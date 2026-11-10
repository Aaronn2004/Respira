package com.nodia.respira.ble;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// ==============================================================
// PeticionarioREST: hace peticiones HTTP en un hilo aparte
// (Android no deja usar la red en el hilo principal).
// ==============================================================
public class PeticionarioREST {

    public interface RespuestaREST {
        void callback(int codigo, String cuerpo);
    }

    private static final ExecutorService hilos = Executors.newSingleThreadExecutor();

    // --------------------------------------------------------------
    // metodo: Text, urlDestino: Text, cuerpo: Text
    //    --> hacerPeticionREST()
    //    --> codigo: Z, cuerpoRespuesta: Text   (por callback; codigo -1 = error de red)
    // --------------------------------------------------------------
    public void hacerPeticionREST(String metodo, String urlDestino, String cuerpo, RespuestaREST laRespuesta) {
        hacerPeticionREST(metodo, urlDestino, cuerpo, null, laRespuesta);
    }

    // --------------------------------------------------------------
    // Igual, con el token de la sesión (cabecera X-Respira-Token)
    // --------------------------------------------------------------
    public void hacerPeticionREST(String metodo, String urlDestino, String cuerpo, String token, RespuestaREST laRespuesta) {
        hilos.execute(() -> {
            HttpURLConnection conexion = null;
            try {
                conexion = (HttpURLConnection) new URL(urlDestino).openConnection();
                conexion.setRequestMethod(metodo);
                conexion.setConnectTimeout(5000);
                conexion.setReadTimeout(10000);
                conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conexion.setRequestProperty("Accept", "application/json");
                if (token != null) {
                    conexion.setRequestProperty("X-Respira-Token", token);
                }

                if (cuerpo != null) {
                    conexion.setDoOutput(true);
                    try (OutputStream salida = conexion.getOutputStream()) {
                        salida.write(cuerpo.getBytes(StandardCharsets.UTF_8));
                    }
                }

                int codigo = conexion.getResponseCode();
                InputStream entrada = codigo < 400 ? conexion.getInputStream() : conexion.getErrorStream();
                laRespuesta.callback(codigo, leerTodo(entrada));
            } catch (Exception error) {
                laRespuesta.callback(-1, error.toString());
            } finally {
                if (conexion != null) {
                    conexion.disconnect();
                }
            }
        });
    }

    // --------------------------------------------------------------
    // entrada: InputStream --> leerTodo() --> Text
    // --------------------------------------------------------------
    private static String leerTodo(InputStream entrada) throws java.io.IOException {
        if (entrada == null) {
            return "";
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int leidos;
        while ((leidos = entrada.read(buffer)) != -1) {
            bytes.write(buffer, 0, leidos);
        }
        entrada.close();
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }
}
