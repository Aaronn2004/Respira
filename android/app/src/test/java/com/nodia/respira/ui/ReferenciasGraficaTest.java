package com.nodia.respira.ui;

import com.nodia.respira.datos.Magnitud;
import com.nodia.respira.datos.Serie;
import org.junit.Test;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import static org.junit.Assert.*;

public class ReferenciasGraficaTest {
    private static long ms(String t) { return Instant.parse(t).toEpochMilli(); }
    private static Serie serie(boolean semana) {
        Double[] valores = new Double[semana ? 56 : 48];
        Arrays.fill(valores, 0.05);
        long desde = ms("2026-11-22T00:00:00Z"), paso = semana ? 10800000 : 1800000;
        return new Serie(valores, null, desde, desde + paso * valores.length, paso);
    }
    private static void alerta(Serie s, long desde, long hasta) {
        s.alertas.add(new Serie.Evento(desde, hasta, 0.093, "alto", "simulado"));
    }
    private static void pico(Serie s, long instante) {
        s.picos.add(new Serie.Evento(instante, instante, 0.095, "alto", "real"));
    }
    private static List<ReferenciasGrafica.Referencia> eventos(Serie s) {
        return ReferenciasGrafica.eventos(s, 280, 48);
    }

    @Test public void caso24hConHorasExactasYExtremosAgrupados() {
        Serie s = serie(false);
        alerta(s, ms("2026-11-22T16:12:00Z"), ms("2026-11-22T16:42:00Z"));
        pico(s, ms("2026-11-22T16:40:00Z"));
        pico(s, ms("2026-11-22T07:40:00Z"));
        pico(s, ms("2026-11-22T17:50:00Z"));
        List<ReferenciasGrafica.Referencia> refs = eventos(s);
        assertEquals(Arrays.asList("P2", "A1 inicio–fin", "P1", "P3"), refs.stream().map(r -> r.id).collect(Collectors.toList()));
        assertArrayEquals(new long[]{s.alertas.get(0).desde, s.alertas.get(0).hasta}, refs.get(1).instantes);
        assertTrue(refs.get(1).lineas[1].contains("22/11 17:12"));
        assertTrue(refs.get(1).lineas[2].contains("22/11 17:42"));
        assertEquals("22/11 17:40", refs.get(2).lineas[1]);
        assertEquals((16 * 60 + 40) / 1440.0, refs.get(2).posiciones[0], 1e-9);
    }

    @Test public void extremosLejanosTienenReferenciasSeparadas() {
        Serie s = serie(false);
        alerta(s, s.desde + 3600000, s.desde + 18 * 3600000);
        List<ReferenciasGrafica.Referencia> refs = eventos(s);
        assertEquals(2, refs.size());
        assertEquals("A1 inicio", refs.get(0).id);
        assertEquals("A1 fin", refs.get(1).id);
    }

    @Test public void incluyeAlertasMediasQueAparecenEnLaLista() {
        Serie s = serie(false);
        s.alertas.add(new Serie.Evento(s.desde + 100, s.desde + 200, 0.07, "medio", "real"));
        assertEquals(1, eventos(s).size());
    }

    @Test public void recorteNoInventaInicioEnElBordeIzquierdo() {
        Serie s = serie(false);
        alerta(s, s.desde - 3600000, s.desde + 3600000);
        List<ReferenciasGrafica.Referencia> refs = eventos(s);
        assertEquals(1, refs.size());
        assertEquals("A1 fin", refs.get(0).id);
        assertEquals(s.desde + 3600000, refs.get(0).instantes[0]);
    }

    @Test public void recorteNoInventaFinEnElBordeDerecho() {
        Serie s = serie(false);
        alerta(s, s.hasta - 3600000, s.hasta + 3600000);
        assertEquals("A1 inicio", eventos(s).get(0).id);
        assertEquals(1, eventos(s).size());
    }

    @Test public void eventoQueCubreVentanaNoEtiquetaBordesComoExtremosReales() {
        Serie s = serie(false);
        alerta(s, s.desde - 1, s.hasta + 1);
        assertTrue(eventos(s).isEmpty());
        assertEquals(1, GraficaHistorica.altos(s, 1, 1).size());
    }

    @Test public void limitesExactosSonValidosFueraNoSeMuestra() {
        Serie s = serie(false);
        alerta(s, s.desde, s.hasta);
        pico(s, s.desde);
        pico(s, s.hasta);
        pico(s, s.hasta + 1);
        List<ReferenciasGrafica.Referencia> refs = eventos(s);
        assertEquals(4, refs.size());
        for (ReferenciasGrafica.Referencia r : refs) for (double p : r.posiciones) assertTrue(p >= 0 && p <= 1);
    }

    @Test public void sinEventosNiVentanaNoHayReferencias() {
        assertTrue(eventos(serie(false)).isEmpty());
        Serie s = new Serie(new Double[]{0.05, 0.06}, null);
        pico(s, 100);
        assertTrue(eventos(s).isEmpty());
        assertTrue(ReferenciasGrafica.paginas(eventos(s), 280, t -> 10, 4).isEmpty());
    }

    @Test public void simultaneosMantienenIdentificadoresYAnclas() {
        Serie s = serie(false);
        alerta(s, s.desde + 3600000, s.desde + 7200000);
        pico(s, s.desde + 7200000);
        pico(s, s.desde + 7200000);
        List<ReferenciasGrafica.Referencia> refs = eventos(s);
        assertEquals(3, refs.size());
        assertEquals(refs.get(0).posiciones[1], refs.get(1).posiciones[0], 0);
        assertEquals(refs.get(1).posiciones[0], refs.get(2).posiciones[0], 0);
        assertEquals("P1", refs.get(1).id);
        assertEquals("P2", refs.get(2).id);
    }

    @Test public void segundosDistintosNoSeFusionanPorMostrarElMismoMinuto() {
        Serie s = serie(false);
        pico(s, s.desde + 1000);
        pico(s, s.desde + 2000);
        List<ReferenciasGrafica.Referencia> refs = eventos(s);
        assertEquals(refs.get(0).lineas[1], refs.get(1).lineas[1]);
        assertNotEquals(refs.get(0).posiciones[0], refs.get(1).posiciones[0], 0);
    }

    @Test public void sieteDiasIncluyeFechasYConservaCadaHora() {
        Serie s = serie(true);
        alerta(s, ms("2026-11-23T22:50:00Z"), ms("2026-11-23T23:20:00Z"));
        pico(s, ms("2026-11-27T18:50:00Z"));
        List<ReferenciasGrafica.Referencia> refs = eventos(s);
        assertTrue(refs.get(0).lineas[1].contains("23/11 23:50"));
        assertTrue(refs.get(0).lineas[2].contains("24/11 00:20"));
        assertEquals("27/11 19:50", refs.get(1).lineas[1]);
    }

    @Test public void horaRepetidaSeDistingueConOffset() {
        assertEquals("25/10 02:20 +02:00", ReferenciasGrafica.hora(ms("2026-10-25T00:20:00Z")));
        assertEquals("25/10 02:20 +01:00", ReferenciasGrafica.hora(ms("2026-10-25T01:20:00Z")));
        assertEquals("29/03 03:20", ReferenciasGrafica.hora(ms("2026-03-29T01:20:00Z")));
    }

    @Test public void muchasReferenciasSeRepartenSinPerderNingunaNiCrecerEnFilas() {
        Serie s = serie(true);
        for (int i = 0; i < 60; i++) alerta(s, s.desde + 1000 * i, s.desde + 1000 * i + 500);
        List<ReferenciasGrafica.Referencia> refs = eventos(s);
        List<List<ReferenciasGrafica.Etiqueta>> paginas = ReferenciasGrafica.paginas(refs, 220, t -> t.length() * 6, 4);
        List<ReferenciasGrafica.Referencia> recuperadas = new ArrayList<>();
        assertTrue(paginas.size() > 1);
        for (List<ReferenciasGrafica.Etiqueta> pagina : paginas) {
            for (ReferenciasGrafica.Etiqueta e : pagina) {
                assertTrue(e.fila == 0 || e.fila == 1);
                assertTrue(e.izquierda >= 0 && e.izquierda + e.ancho <= 220);
                recuperadas.add(e.referencia);
                for (ReferenciasGrafica.Etiqueta otra : pagina) {
                    if (otra != e && otra.fila == e.fila) {
                        assertTrue(e.izquierda + e.ancho <= otra.izquierda || otra.izquierda + otra.ancho <= e.izquierda);
                    }
                }
            }
        }
        assertEquals(refs, recuperadas);
    }

    @Test public void paginaNoAlejaEtiquetasDeSusAnclasParaHacerlasCaber() {
        Serie s = serie(false);
        pico(s, s.desde);
        pico(s, s.desde + 100);
        pico(s, s.hasta);
        List<List<ReferenciasGrafica.Etiqueta>> paginas = ReferenciasGrafica.paginas(eventos(s), 140, t -> t.length() * 6, 4);
        for (List<ReferenciasGrafica.Etiqueta> pagina : paginas) for (ReferenciasGrafica.Etiqueta e : pagina) {
            double centro = e.izquierda + e.ancho / 2;
            assertTrue(Math.abs(centro - e.referencia.posiciones[0] * 140) <= e.ancho / 2 + 1e-9);
        }
    }

    @Test public void rotulosPicosCoincidentesNoSeSolapanYConectanAlPuntoExacto() {
        Serie s = serie(false);
        for (int i = 0; i < 3; i++) pico(s, s.desde + 12 * 3600000);
        double[] limites = GraficaHistorica.limites(s, Magnitud.O3);
        List<ReferenciasGrafica.RotuloPico> rotulos = ReferenciasGrafica.rotulosPicos(s, 280, 180,
                limites[0], limites[1], 24, 16, 7);
        assertEquals(3, rotulos.size());
        for (ReferenciasGrafica.RotuloPico r : rotulos) {
            assertEquals(140, r.x, 1e-9);
            assertEquals((limites[1] - 0.095) / (limites[1] - limites[0]) * 180, r.y, 1e-9);
            assertTrue(Math.hypot(r.x - r.enlaceX, r.y - r.enlaceY) < 60);
            for (ReferenciasGrafica.RotuloPico otro : rotulos) if (r != otro) {
                assertTrue(r.izquierda + r.ancho <= otro.izquierda || otro.izquierda + otro.ancho <= r.izquierda
                        || r.arriba + r.alto <= otro.arriba || otro.arriba + otro.alto <= r.arriba);
            }
        }
    }

    @Test public void rotulosDePicosEnBordesQuedanDentroDelDibujo() {
        Serie s = serie(true);
        pico(s, s.desde);
        pico(s, s.hasta);
        double[] limites = GraficaHistorica.limites(s, Magnitud.O3);
        List<ReferenciasGrafica.RotuloPico> rotulos = ReferenciasGrafica.rotulosPicos(s, 180, 180,
                limites[0], limites[1], 24, 16, 7);
        assertEquals(2, rotulos.size());
        for (ReferenciasGrafica.RotuloPico r : rotulos) {
            assertTrue(r.izquierda >= 0 && r.izquierda + r.ancho <= 180);
            assertTrue(r.arriba >= 0 && r.arriba + r.alto <= 180);
        }
        assertEquals(0, rotulos.get(0).x, 0);
        assertEquals(180, rotulos.get(1).x, 0);
    }

    @Test public void presentarReferenciasNoModificaDatosNiOrdenDelBackend() {
        Serie s = serie(false);
        alerta(s, s.desde + 5000, s.desde + 6000);
        alerta(s, s.desde + 1000, s.desde + 2000);
        pico(s, s.desde + 3000);
        Double[] valores = s.valores.clone();
        Serie.Evento original = s.alertas.get(0);
        eventos(s);
        assertSame(original, s.alertas.get(0));
        assertEquals(5000 + s.desde, original.desde);
        assertArrayEquals(valores, s.valores);
    }

    @Test public void anchoCeroNoProducePaginasNiCoordenadasInvalidas() {
        Serie s = serie(false);
        pico(s, s.desde);
        assertTrue(ReferenciasGrafica.eventos(s, 0, 48).isEmpty());
        assertTrue(ReferenciasGrafica.paginas(eventos(s), 0, t -> 10, 4).isEmpty());
    }
}
