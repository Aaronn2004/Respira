package com.nodia.respira.ble;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

// Test automático de LogicaFake + PeticionarioREST contra un servidor
// HTTP falso levantado en el propio test (no hace falta el servidor real).
public class LogicaFakeTest {

    // Servidor HTTP mínimo con ServerSocket (atiende UNA petición y responde 201)
    private ServerSocket servidorFalso;
    private final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();
    private final AtomicReference<String> rutaRecibida = new AtomicReference<>();

    @Before
    public void arrancarServidorFalso() throws Exception {
        servidorFalso = new ServerSocket(0);
        Thread hilo = new Thread(() -> {
            try (Socket cliente = servidorFalso.accept()) {
                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(cliente.getInputStream(), StandardCharsets.UTF_8));
                String primeraLinea = entrada.readLine(); // "POST /medicion HTTP/1.1"
                rutaRecibida.set(primeraLinea.substring(0, primeraLinea.lastIndexOf(' ')));

                int longitud = 0;
                String linea;
                while ((linea = entrada.readLine()) != null && !linea.isEmpty()) {
                    if (linea.toLowerCase().startsWith("content-length:")) {
                        longitud = Integer.parseInt(linea.substring(15).trim());
                    }
                }
                char[] cuerpo = new char[longitud];
                int leidos = 0;
                while (leidos < longitud) {
                    leidos += entrada.read(cuerpo, leidos, longitud - leidos);
                }
                cuerpoRecibido.set(new String(cuerpo));

                String respuesta = "HTTP/1.1 201 Created\r\nContent-Type: application/json\r\n"
                        + "Content-Length: 8\r\nConnection: close\r\n\r\n{\"id\":1}";
                OutputStream salida = cliente.getOutputStream();
                salida.write(respuesta.getBytes(StandardCharsets.UTF_8));
                salida.flush();
            } catch (Exception ignorada) {
                // el test que no usa el servidor lo cierra sin peticiones
            }
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    @After
    public void pararServidorFalso() throws Exception {
        servidorFalso.close();
    }

    @Test
    public void guardarMedicionHacePOSTConElJSON() throws Exception {
        String url = "http://127.0.0.1:" + servidorFalso.getLocalPort();
        CountDownLatch terminado = new CountDownLatch(1);
        AtomicBoolean exito = new AtomicBoolean(false);

        new LogicaFake(url).guardarMedicion(new Medicion(11, 0.235, 7), (ok, detalle) -> {
            exito.set(ok);
            terminado.countDown();
        });

        assertTrue("no respondió a tiempo", terminado.await(5, TimeUnit.SECONDS));
        assertTrue(exito.get());
        assertEquals("POST /medicion", rutaRecibida.get());
        assertEquals("{\"tipo\":11,\"valor\":0.235,\"contador\":7}", cuerpoRecibido.get());
    }

    @Test
    public void guardarMedicionSinServidorAvisaDelFallo() throws Exception {
        CountDownLatch terminado = new CountDownLatch(1);
        AtomicBoolean exito = new AtomicBoolean(true);

        new LogicaFake("http://127.0.0.1:1").guardarMedicion(new Medicion(11, 0.1, 1), (ok, detalle) -> {
            exito.set(ok);
            terminado.countDown();
        });

        assertTrue(terminado.await(10, TimeUnit.SECONDS));
        assertEquals(false, exito.get());
    }
}
