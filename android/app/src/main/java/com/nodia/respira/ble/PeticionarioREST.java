package com.nodia.respira.ble;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// # Ausente representa la ausencia lógica (null en el código).
// # ErrorDatos = { detalle: Text }; fallo al leer o interpretar datos.

// ==============================================================
// PeticionarioREST: hace peticiones HTTP en un hilo aparte
// (Android no deja usar la red en el hilo principal).
// ==============================================================
public class PeticionarioREST {

    public interface RespuestaREST {
        // --------------------------------------------------------------
        // # effects: entrega el resultado a quien recibe la respuesta.
        // + codigo: Z, cuerpo: Text
        //       -->
        //     callback() -->
        // --------------------------------------------------------------
        void callback(int codigo, String cuerpo);
    }

    private static final ExecutorService hilos = Executors.newSingleThreadExecutor();

    // --------------------------------------------------------------
    // # La respuesta llega desde el hilo de red; el código -1 representa un fallo de conexión.
    // # effects: accede a la red; entrega el resultado de forma asíncrona.
    // + metodo: Text, url_destino: Text, cuerpo: Text | Ausente
    //       -->
    //     hacerPeticionREST() <--
    //       <--
    //   (codigo: Z, cuerpo: Text)
    // --------------------------------------------------------------
    public void hacerPeticionREST(String metodo, String urlDestino, String cuerpo, RespuestaREST laRespuesta) {
        hacerPeticionREST(metodo, urlDestino, cuerpo, null, laRespuesta);
    }

    // --------------------------------------------------------------
    // # La respuesta llega desde el hilo de red; el código -1 representa un fallo de conexión.
    // # effects: accede a la red; entrega el resultado de forma asíncrona.
    // + metodo: Text, url_destino: Text, cuerpo: Text | Ausente, token: Text | Ausente
    //       -->
    //     hacerPeticionREST() <--
    //       <--
    //   (codigo: Z, cuerpo: Text)
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
    // # effects: consume y cierra el flujo de entrada.
    // - entrada: [Z] | Ausente
    //       -->
    //     leerTodo() --x
    //       <--
    //   Text | ErrorDatos
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
