package com.nodia.respira.ble;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

// Test automático de LogicaFake + PeticionarioREST contra un servidor
// HTTP falso levantado en el propio test (no hace falta la API real).
public class LogicaFakeTest {

    private ServerSocket servidorFalso;
    private final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();
    private final AtomicReference<String> rutaRecibida = new AtomicReference<>();
    private final AtomicReference<String> tokenRecibido = new AtomicReference<>();

    // Servidor HTTP mínimo: atiende UNA petición y responde con el código dado
    private String arrancarServidorFalso(int codigo, String respuesta) throws Exception {
        servidorFalso = new ServerSocket(0);
        Thread hilo = new Thread(() -> {
            try (Socket cliente = servidorFalso.accept()) {
                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(cliente.getInputStream(), StandardCharsets.UTF_8));
                String primeraLinea = entrada.readLine(); // "POST /api/index.php/mediciones HTTP/1.1"
                rutaRecibida.set(primeraLinea.substring(0, primeraLinea.lastIndexOf(' ')));
                int longitud = 0;
                String linea;
                while ((linea = entrada.readLine()) != null && !linea.isEmpty()) {
                    if (linea.toLowerCase().startsWith("content-length:")) {
                        longitud = Integer.parseInt(linea.substring(15).trim());
                    }
                    if (linea.toLowerCase().startsWith("x-respira-token:")) {
                        tokenRecibido.set(linea.substring(16).trim());
                    }
                }
                char[] cuerpo = new char[longitud];
                int leidos = 0;
                while (leidos < longitud) {
                    leidos += entrada.read(cuerpo, leidos, longitud - leidos);
                }
                cuerpoRecibido.set(new String(cuerpo));
                byte[] r = respuesta.getBytes(StandardCharsets.UTF_8);
                String cab = "HTTP/1.1 " + codigo + " X\r\nContent-Type: application/json\r\nContent-Length: "
                        + r.length + "\r\nConnection: close\r\n\r\n";
                OutputStream salida = cliente.getOutputStream();
                salida.write(cab.getBytes(StandardCharsets.UTF_8));
                salida.write(r);
                salida.flush();
            } catch (Exception ignorada) {
                // el test que no usa el servidor lo cierra sin peticiones
            }
        });
        hilo.setDaemon(true);
        hilo.start();
        return "http://127.0.0.1:" + servidorFalso.getLocalPort() + "/api/index.php";
    }

    @After
    public void pararServidorFalso() throws Exception {
        if (servidorFalso != null) {
            servidorFalso.close();
        }
    }

    private static Medicion ejemplo() {
        return new Medicion("RSP-4F7K", "o3", 0.042, 7, 1000L).conUbicacion(38.9657, -0.1827, 8);
    }

    @Test
    public void guardarMedicionHacePOSTConElJSON() throws Exception {
        String url = arrancarServidorFalso(201, "{\"id\":1}");
        CountDownLatch terminado = new CountDownLatch(1);
        AtomicBoolean exito = new AtomicBoolean(false);
        new LogicaFake(url).guardarMedicion(ejemplo(), (ok, codigo, detalle) -> {
            exito.set(ok);
            terminado.countDown();
        });
        assertTrue("no respondió a tiempo", terminado.await(5, TimeUnit.SECONDS));
        assertTrue(exito.get());
        assertEquals("POST /api/index.php/mediciones", rutaRecibida.get());
        assertEquals(ejemplo().toJSON(), cuerpoRecibido.get());
    }

    // --------------------------------------------------------------
    // Con sesión, el token va en la cabecera X-Respira-Token (así el
    // servidor sabe a quién darle los puntos); el JSON no cambia
    // --------------------------------------------------------------
    @Test
    public void conSesionMandaElToken() throws Exception {
        String url = arrancarServidorFalso(201, "{\"id\":1}");
        CountDownLatch terminado = new CountDownLatch(1);
        LogicaFake l = new LogicaFake(url);
        l.conToken("abc123");
        l.guardarMedicion(ejemplo(), (ok, codigo, detalle) -> terminado.countDown());
        assertTrue(terminado.await(5, TimeUnit.SECONDS));
        assertEquals("abc123", tokenRecibido.get());
    }

    @Test
    public void unaRepetida409CuentaComoEntregada() throws Exception {
        String url = arrancarServidorFalso(409, "{\"error\":\"repetida\"}");
        CountDownLatch terminado = new CountDownLatch(1);
        AtomicBoolean exito = new AtomicBoolean(false);
        AtomicInteger cod = new AtomicInteger();
        new LogicaFake(url).guardarMedicion(ejemplo(), (ok, codigo, detalle) -> {
            exito.set(ok);
            cod.set(codigo);
            terminado.countDown();
        });
        assertTrue(terminado.await(5, TimeUnit.SECONDS));
        assertTrue(exito.get());
        assertEquals(409, cod.get());
    }

    @Test
    public void loteDePendientes() throws Exception {
        String url = arrancarServidorFalso(201, "{\"guardadas\":2,\"repetidas\":0,\"errores\":[]}");
        CountDownLatch terminado = new CountDownLatch(1);
        new LogicaFake(url + "/").guardarMediciones(Arrays.asList(ejemplo(), ejemplo()), (ok, codigo, detalle) -> terminado.countDown());
        assertTrue(terminado.await(5, TimeUnit.SECONDS));
        assertEquals("{\"mediciones\":[" + ejemplo().toJSON() + "," + ejemplo().toJSON() + "]}", cuerpoRecibido.get());
    }

    @Test
    public void sinServidorAvisaDelFalloConCodigoMenosUno() throws Exception {
        CountDownLatch terminado = new CountDownLatch(1);
        AtomicBoolean exito = new AtomicBoolean(true);
        AtomicInteger cod = new AtomicInteger();
        new LogicaFake("http://127.0.0.1:1").guardarMedicion(ejemplo(), (ok, codigo, detalle) -> {
            exito.set(ok);
            cod.set(codigo);
            terminado.countDown();
        });
        assertTrue(terminado.await(10, TimeUnit.SECONDS));
        assertEquals(false, exito.get());
        assertEquals(-1, cod.get());
    }
}
