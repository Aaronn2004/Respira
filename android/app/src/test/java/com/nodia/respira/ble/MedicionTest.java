package com.nodia.respira.ble;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

// Tests de Medicion: de la trama iBeacon del nodo Respira al JSON de la API.
public class MedicionTest {

    private static final String UUID = "RESPIRA-RSP-4F7K";

    @Test
    public void ozonoVieneEnMilesimasDePpm() {
        TramaIBeacon t = new TramaIBeacon(TramaIBeaconTest.construirAnuncio(true, UUID, (11 << 8) + 7, 42));
        Medicion m = Medicion.desdeTrama(t, "RSP-4F7K", 1000L);
        assertEquals("o3", m.getMagnitud());
        assertEquals(0.042, m.getValor(), 1e-9);
        assertEquals(7, m.getContador());
    }

    @Test
    public void temperaturaEnDecimasYConSigno() {
        TramaIBeacon t = new TramaIBeacon(TramaIBeaconTest.construirAnuncio(true, UUID, (12 << 8) + 3, 234));
        assertEquals(23.4, Medicion.desdeTrama(t, "RSP-4F7K", 0L).getValor(), 1e-9);
        // -5.5 °C viaja como el entero de 16 bits -55 (0xFFC9)
        t = new TramaIBeacon(TramaIBeaconTest.construirAnuncio(true, UUID, (12 << 8) + 4, 0xFFC9));
        assertEquals(-5.5, Medicion.desdeTrama(t, "RSP-4F7K", 0L).getValor(), 1e-9);
    }

    @Test
    public void ruidoEnDecimasDeDecibelio() {
        TramaIBeacon t = new TramaIBeacon(TramaIBeaconTest.construirAnuncio(true, UUID, (13 << 8) + 9, 633));
        Medicion m = Medicion.desdeTrama(t, "RSP-4F7K", 0L);
        assertEquals("ruido", m.getMagnitud());
        assertEquals(63.3, m.getValor(), 1e-9);
    }

    @Test
    public void unTipoDesconocidoSeIgnora() {
        TramaIBeacon t = new TramaIBeacon(TramaIBeaconTest.construirAnuncio(true, UUID, (99 << 8) + 1, 5));
        assertNull(Medicion.desdeTrama(t, "RSP-4F7K", 0L));
    }

    @Test
    public void jsonConUbicacionRssiYPortador() {
        Medicion m = new Medicion("RSP-4F7K", "o3", 0.042, 7, 1762423200000L)
                .conUbicacion(38.9657, -0.1827, 8).conRssi(-61).conPortador("U-48213");
        assertEquals("{\"nodo\":\"RSP-4F7K\",\"magnitud\":\"o3\",\"valor\":0.042,\"contador\":7,"
                + "\"medida_en\":1762423200000,\"lat\":38.965700,\"lng\":-0.182700,\"precision\":8,"
                + "\"rssi\":-61,\"portador\":\"U-48213\"}", m.toJSON());
    }

    @Test
    public void jsonSinUbicacion() {
        assertEquals("{\"nodo\":\"RSP-4F7K\",\"magnitud\":\"temp\",\"valor\":21.0,\"contador\":1,\"medida_en\":5}",
                new Medicion("RSP-4F7K", "temp", 21.0, 1, 5L).toJSON());
    }

    @Test
    public void uuidDelSensor() {
        assertEquals(UUID, Medicion.uuidDelSensor("RSP-4F7K"));
    }
}
