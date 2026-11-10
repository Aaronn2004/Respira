package com.nodia.respira.ble;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

// Tests automáticos (JUnit, sin móvil): botón derecho > Run 'Tests in ...'
public class TramaIBeaconTest {

    // --------------------------------------------------------------
    // conFlags: B, uuid: Text, major: N, minor: N --> construirAnuncio() --> [N]
    // Construye los bytes de un anuncio iBeacon como los emite la placa.
    // --------------------------------------------------------------
    static byte[] construirAnuncio(boolean conFlags, String uuid, int major, int minor) {
        byte[] flags = { 0x02, 0x01, 0x06 };
        byte[] cabecera = { 0x1A, (byte) 0xFF, 0x4C, 0x00, 0x02, 0x15 };
        byte[] uuidBytes = uuid.getBytes(StandardCharsets.US_ASCII);
        byte[] resto = { (byte) (major >> 8), (byte) major, (byte) (minor >> 8), (byte) minor, (byte) -53 };

        int inicio = conFlags ? flags.length : 0;
        byte[] anuncio = new byte[inicio + cabecera.length + 16 + resto.length + 10]; // +10: bytes de relleno
        if (conFlags) System.arraycopy(flags, 0, anuncio, 0, flags.length);
        System.arraycopy(cabecera, 0, anuncio, inicio, cabecera.length);
        System.arraycopy(uuidBytes, 0, anuncio, inicio + cabecera.length, 16);
        System.arraycopy(resto, 0, anuncio, inicio + cabecera.length + 16, resto.length);
        return anuncio;
    }

    @Test
    public void extraeUUIDMajorMinorConFlags() {
        TramaIBeacon trama = new TramaIBeacon(construirAnuncio(true, "RESPIRA-RSP-4F7K", (11 << 8) + 7, 235));
        assertTrue(trama.esIBeacon());
        assertEquals("RESPIRA-RSP-4F7K", trama.getUUIDComoTexto());
        assertEquals(11, trama.getTipoMedicion());
        assertEquals(7, trama.getContador());
        assertEquals(235, trama.getValorEntero());
        assertEquals(-53, trama.getTxPower());
    }

    @Test
    public void funcionaTambienSinFlags() {
        TramaIBeacon trama = new TramaIBeacon(construirAnuncio(false, "RESPIRA-RSP-4F7K", (11 << 8) + 200, 65000));
        assertTrue(trama.esIBeacon());
        assertEquals(200, trama.getContador());
        assertEquals(65000, trama.getValorEntero()); // sin signo
    }

    @Test
    public void valorConSignoParaTemperaturasNegativas() {
        TramaIBeacon trama = new TramaIBeacon(construirAnuncio(true, "RESPIRA-RSP-4F7K", (12 << 8) + 1, 0xFFC9));
        assertEquals(-55, trama.getValorConSigno());
        assertEquals(65481, trama.getValorEntero());
    }

    @Test
    public void noEsIBeaconSiNoHayCabecera() {
        assertFalse(new TramaIBeacon(new byte[] { 0x02, 0x01, 0x06, 0x05, 0x09, 'H', 'o', 'l', 'a' }).esIBeacon());
        assertFalse(new TramaIBeacon(null).esIBeacon());
        assertFalse(new TramaIBeacon(new byte[0]).esIBeacon());
    }
}
