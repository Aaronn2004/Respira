package com.nodia.respira.datos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.junit.Test;

public class SerieTest {
    private static long ms(String iso) {
        return Instant.parse(iso).toEpochMilli();
    }

    @Test
    public void ventana24hNoEmpiezaAMedianocheNiColocaMediasEnLosExtremos() {
        Serie s = new Serie(new Double[48], null, ms("2026-11-21T17:52:00Z"),
                ms("2026-11-22T17:52:00Z"), 1800000);
        assertTrue(s.tieneIntervalo());
        assertEquals(ms("2026-11-21T18:07:00Z"), s.instanteDelTramo(0));
        assertEquals(ms("2026-11-22T17:37:00Z"), s.instanteDelTramo(47));
        assertEquals(0.0, s.posicionTemporal(s.desde), 0.000001);
        assertEquals(1.0, s.posicionTemporal(s.hasta), 0.000001);
        assertEquals(1.0 / 96, s.posicionTemporal(s.instanteDelTramo(0)), 0.000001);
        assertEquals(95.0 / 96, s.posicionTemporal(s.instanteDelTramo(47)), 0.000001);
    }

    @Test
    public void picoDeLas1520ComparteLaEscalaTemporalDelGrafico() {
        Serie s = new Serie(new Double[48], null, ms("2026-11-21T17:52:00Z"),
                ms("2026-11-22T17:52:00Z"), 1800000);
        // 15:20 en Madrid: 14:20 UTC; 20 h 28 min desde el inicio de la ventana.
        assertEquals(1228.0 / 1440, s.posicionTemporal(ms("2026-11-22T14:20:00Z")), 0.000001);
    }

    @Test
    public void sieteDiasUsa56IntervalosDeTresHoras() {
        Serie s = new Serie(new Double[56], null, ms("2026-11-16T17:52:00Z"),
                ms("2026-11-23T17:52:00Z"), 10800000);
        assertEquals(ms("2026-11-16T19:22:00Z"), s.instanteDelTramo(0));
        assertEquals(ms("2026-11-23T16:22:00Z"), s.instanteDelTramo(55));
        assertEquals(0.5, s.posicionTemporal(ms("2026-11-20T05:52:00Z")), 0.000001);
    }

    @Test
    public void cambioDeHoraNoConfundeDosInstantesConLaMismaHoraLocal() {
        Serie s = new Serie(new Double[48], null, ms("2026-10-24T12:00:00Z"),
                ms("2026-10-25T12:00:00Z"), 1800000);
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM HH:mm XXX")
                .withZone(ZoneId.of("Europe/Madrid"));
        assertEquals("25/10 02:15 +02:00", f.format(Instant.ofEpochMilli(s.instanteDelTramo(24))));
        assertEquals("25/10 02:15 +01:00", f.format(Instant.ofEpochMilli(s.instanteDelTramo(26))));
        assertEquals(1.0 / 24, s.posicionTemporal(s.instanteDelTramo(26))
                - s.posicionTemporal(s.instanteDelTramo(24)), 0.000001);
    }

    @Test
    public void ultimoIntervaloParcialNoSeSaleDeLaVentana() {
        Serie s = new Serie(new Double[3], null, 0, 300000, 120000);
        assertEquals(270000, s.instanteDelTramo(2));
        assertEquals(0.9, s.posicionTemporal(s.instanteDelTramo(2)), 0.000001);
    }

    @Test
    public void serieAntiguaNoInventaFechas() {
        assertFalse(new Serie(new Double[48], null).tieneIntervalo());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaPasoIncompatibleConLaCantidadDeTramos() {
        new Serie(new Double[48], null, 0, 86400000, 10800000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaVentanaInvertida() {
        new Serie(new Double[48], null, 86400000, 0, 1800000);
    }
}
