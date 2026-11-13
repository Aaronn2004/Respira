package com.nodia.respira.datos;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// Textos de las estadísticas del recorrido (pantalla «Mi sensor» y Puntos)
public class ResumenRecorridoTest {

    @Test
    public void distanciaEnMetrosYKilometros() {
        assertEquals("0 m", ResumenRecorrido.distanciaTexto(0));
        assertEquals("850 m", ResumenRecorrido.distanciaTexto(850));
        assertEquals("1,0 km", ResumenRecorrido.distanciaTexto(1000));
        assertEquals("2,4 km", ResumenRecorrido.distanciaTexto(2430));
    }

    @Test
    public void duracionEnMinutosYHoras() {
        assertEquals("45 min", ResumenRecorrido.duracionTexto(45));
        assertEquals("1 h 25 min", ResumenRecorrido.duracionTexto(85));
        assertEquals("2 h", ResumenRecorrido.duracionTexto(120));
    }

    @Test
    public void horaDelServidorEnHoraDeEspana() {
        assertEquals("10:00", ResumenRecorrido.hora("2026-11-08T09:00:00.000Z"));   // invierno: UTC+1
        assertEquals("11:30", ResumenRecorrido.hora("2026-07-01T09:30:00.000Z"));   // verano: UTC+2
        assertEquals("10:42", ResumenRecorrido.hora("10:42"));                      // copia del móvil
        assertEquals(null, ResumenRecorrido.hora(null));
    }
}
