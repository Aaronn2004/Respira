package com.nodia.respira.ui;

import com.nodia.respira.datos.Magnitud;
import com.nodia.respira.datos.Serie;
import org.junit.Test;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import static org.junit.Assert.*;

public class GraficaHistoricaTest {
    private static long ms(String iso) { return Instant.parse(iso).toEpochMilli(); }

    private static Serie serie(String inicio, boolean semana) {
        int n = semana ? 56 : 48;
        long paso = semana ? 10800000 : 1800000;
        Double[] valores = new Double[n];
        Arrays.fill(valores, 0.050);
        return new Serie(valores, null, ms(inicio), ms(inicio) + n * paso, paso);
    }

    private static void pico(Serie s, long instante, double valor) {
        s.picos.add(new Serie.Evento(instante, instante, valor, "alto", "simulado"));
    }

    private static void alto(Serie s, long desde, long hasta) {
        s.alertas.add(new Serie.Evento(desde, hasta, 0.093, "alto", "simulado"));
    }

    @Test public void altoUsaHorasDelEventoAunqueLaMediaNoSupereElUmbral() {
        Serie s = serie("2026-11-21T23:35:00Z", false);
        alto(s, ms("2026-11-22T12:24:00Z"), ms("2026-11-22T16:24:00Z"));
        GraficaHistorica.Alto a = GraficaHistorica.altos(s, 320, 50).get(0);
        assertEquals(769.0 / 1440, a.inicio, 1e-9);
        assertEquals(1009.0 / 1440, a.fin, 1e-9);
        assertEquals(1.0 / 6, a.fin - a.inicio, 1e-9);
        assertSame(s.alertas.get(0), a.evento);
        assertEquals(0.093, a.evento.valor, 0);
        assertEquals(0.05, s.valores[25], 0);
    }

    @Test public void sinEventosOConMedioNoHayFranjasAltas() {
        Serie s = serie("2026-11-22T00:00:00Z", false);
        assertTrue(GraficaHistorica.altos(s, 320, 50).isEmpty());
        s.alertas.add(new Serie.Evento(s.desde, s.hasta, 0.08, "medio", "real"));
        assertTrue(GraficaHistorica.altos(s, 320, 50).isEmpty());
        assertEquals(0, GraficaHistorica.filasAltos(GraficaHistorica.altos(s, 320, 50)));
    }

    @Test public void altoRecortaPorLaIzquierdaSinModificarElEvento() {
        Serie s = serie("2026-11-22T00:00:00Z", false);
        alto(s, s.desde - 3600000, s.desde + 3600000);
        GraficaHistorica.Alto a = GraficaHistorica.altos(s, 320, 50).get(0);
        assertEquals(s.desde, a.desde);
        assertEquals(0, a.inicio, 0);
        assertEquals(1.0 / 24, a.fin, 1e-9);
        assertEquals(s.desde - 3600000, a.evento.desde);
    }

    @Test public void altoRecortaPorLaDerechaSinModificarElEvento() {
        Serie s = serie("2026-11-22T00:00:00Z", false);
        alto(s, s.hasta - 3600000, s.hasta + 3600000);
        GraficaHistorica.Alto a = GraficaHistorica.altos(s, 320, 50).get(0);
        assertEquals(s.hasta, a.hasta);
        assertEquals(23.0 / 24, a.inicio, 1e-9);
        assertEquals(1, a.fin, 0);
        assertEquals(s.hasta + 3600000, a.evento.hasta);
    }

    @Test public void altoQueCubreTodaLaVentanaSeRecortaEnAmbosBordes() {
        Serie s = serie("2026-11-22T00:00:00Z", true);
        alto(s, s.desde - 86400000, s.hasta + 86400000);
        GraficaHistorica.Alto a = GraficaHistorica.altos(s, 320, 50).get(0);
        assertEquals(0, a.inicio, 0);
        assertEquals(1, a.fin, 0);
    }

    @Test public void altoDeSieteDiasConservaPosicionYDuracionReal() {
        Serie s = serie("2026-11-16T23:35:00Z", true);
        alto(s, s.desde + 2 * 86400000L, s.desde + 3 * 86400000L);
        GraficaHistorica.Alto a = GraficaHistorica.altos(s, 320, 50).get(0);
        assertEquals(2.0 / 7, a.inicio, 1e-9);
        assertEquals(3.0 / 7, a.fin, 1e-9);
    }

    @Test public void altosFueraDeVentanaODuracionNoPositivaNoSeDibujan() {
        Serie s = serie("2026-11-22T00:00:00Z", false);
        alto(s, s.desde - 100, s.desde);
        alto(s, s.hasta, s.hasta + 100);
        alto(s, s.desde + 100, s.desde + 100);
        alto(s, s.desde + 200, s.desde + 100);
        assertTrue(GraficaHistorica.altos(s, 320, 50).isEmpty());
    }

    @Test public void variosAltosSeConservanSeparadosYNoSeReordenaLaRespuesta() {
        Serie s = serie("2026-11-22T00:00:00Z", false);
        alto(s, s.desde + 18 * 3600000, s.desde + 20 * 3600000);
        alto(s, s.desde + 2 * 3600000, s.desde + 4 * 3600000);
        List<GraficaHistorica.Alto> altos = GraficaHistorica.altos(s, 320, 50);
        assertEquals(2, altos.size());
        assertTrue(altos.get(0).fin < altos.get(1).inicio);
        assertSame(s.alertas.get(1), altos.get(0).evento);
        assertSame(s.alertas.get(0), altos.get(1).evento);
        assertEquals(1, GraficaHistorica.filasAltos(altos));
    }

    @Test public void etiquetasDeAltosCercanosOcupanFilasDistintasDentroDelAncho() {
        Serie s = serie("2026-11-22T00:00:00Z", false);
        for (int i = 0; i < 3; i++) alto(s, s.desde + i * 60000, s.desde + (i + 1) * 60000);
        List<GraficaHistorica.Alto> altos = GraficaHistorica.altos(s, 140, 50);
        assertEquals(3, altos.size());
        assertEquals(3, GraficaHistorica.filasAltos(altos));
        for (int i = 0; i < altos.size(); i++) {
            assertEquals(i, altos.get(i).fila);
            double x = altos.get(i).centroEtiqueta * 140;
            assertTrue(x - 25 >= 0);
            assertTrue(x + 25 <= 140);
        }
    }

    @Test public void altoAlCruzarCambioHorarioUsaDuracionDelInstante() {
        Serie s = serie("2026-10-24T22:00:00Z", false);
        // 02:00 +02 a 02:00 +01: misma hora civil, pero una hora real.
        alto(s, ms("2026-10-25T00:00:00Z"), ms("2026-10-25T01:00:00Z"));
        GraficaHistorica.Alto a = GraficaHistorica.altos(s, 320, 50).get(0);
        assertEquals(1.0 / 24, a.fin - a.inicio, 1e-9);
    }

    @Test public void sinVentanaOAnchoUtilNoSeInventanFranjas() {
        Serie s = new Serie(new Double[]{0.05, 0.06}, null);
        alto(s, 100, 200);
        assertTrue(GraficaHistorica.altos(s, 320, 50).isEmpty());
        Serie conTiempo = serie("2026-11-22T00:00:00Z", false);
        alto(conTiempo, conTiempo.desde, conTiempo.hasta);
        assertTrue(GraficaHistorica.altos(conTiempo, 0, 50).isEmpty());
    }

    @Test public void picoUsaSuHoraYValorCrudosSinAjustarseALaMedia() {
        Serie s = serie("2026-11-21T17:52:00Z", false);
        pico(s, ms("2026-11-22T14:20:00Z"), 0.095);
        GraficaHistorica.Pico p = GraficaHistorica.picos(s).get(0);
        assertEquals("P1", p.etiqueta);
        assertEquals(1228.0 / 1440, p.posicion, 1e-9);
        assertEquals(0.095, p.evento.valor, 0);
        assertEquals("simulado", p.evento.origen);
        assertEquals(0.050, s.valores[40], 0);
        assertSame(s.picos.get(0), p.evento);
    }

    @Test public void maximoTresYOrdenTemporalNoCambiaNumerosNiListaOriginal() {
        Serie s = serie("2026-11-22T00:00:00Z", false);
        pico(s, s.desde + 7200000, 0.12);
        pico(s, s.desde + 3600000, 0.11);
        pico(s, s.desde + 10800000, 0.10);
        pico(s, s.desde + 14400000, 0.09);
        List<GraficaHistorica.Pico> ordenados = GraficaHistorica.picosPorTiempo(s);
        assertEquals(Arrays.asList("P2", "P1", "P3"), ordenados.stream().map(p -> p.etiqueta).collect(Collectors.toList()));
        assertEquals("P1", GraficaHistorica.picos(s).get(0).etiqueta);
        assertEquals(4, s.picos.size());
    }

    @Test public void picosCoincidentesConservanIdentidadYCoordenadas() {
        Serie s = serie("2026-11-22T00:00:00Z", false);
        pico(s, s.desde + 3600000, 0.095);
        pico(s, s.desde + 3600000, 0.095);
        List<GraficaHistorica.Pico> p = GraficaHistorica.picosPorTiempo(s);
        assertEquals(2, p.size());
        assertEquals("P1", p.get(0).etiqueta);
        assertEquals("P2", p.get(1).etiqueta);
        assertEquals(p.get(0).posicion, p.get(1).posicion, 0);
    }

    @Test public void picosEnBordesYFueraDeVentana() {
        Serie s = serie("2026-11-22T00:00:00Z", false);
        pico(s, s.desde, 0.10);
        pico(s, s.hasta, 0.11);
        pico(s, s.hasta + 1, 0.12);
        List<GraficaHistorica.Pico> p = GraficaHistorica.picos(s);
        assertEquals(2, p.size());
        assertEquals(0, p.get(0).posicion, 0);
        assertEquals(1, p.get(1).posicion, 0);
    }

    @Test public void picoNoFinitoNoContaminaEscala() {
        Serie s = serie("2026-11-22T00:00:00Z", false);
        pico(s, s.desde + 1, Double.NaN);
        assertTrue(GraficaHistorica.picos(s).isEmpty());
        assertTrue(Double.isFinite(GraficaHistorica.limites(s, Magnitud.O3)[1]));
    }

    @Test public void escalaIncluyePicoAltoConMargenYUmbrales() {
        Serie s = serie("2026-11-22T00:00:00Z", false);
        pico(s, s.desde + 1, 0.200);
        double[] limites = GraficaHistorica.limites(s, Magnitud.O3);
        assertTrue(limites[0] < 0.050);
        assertTrue(limites[1] > 0.200);
        assertTrue(limites[0] < Magnitud.O3.limiteMedio);
        assertTrue(limites[1] > Magnitud.O3.limiteAlto);
    }

    @Test public void escalaIncluyeTemperaturaNegativaEnBordeInferior() {
        Serie s = serie("2026-11-22T00:00:00Z", false);
        Arrays.fill(s.valores, -5.0);
        pico(s, s.desde + 1, -10.0);
        assertTrue(GraficaHistorica.limites(s, Magnitud.TEMP)[0] < -10);
    }

    @Test public void sinPicosMantieneLaEscalaAnterior() {
        Serie s = serie("2026-11-22T00:00:00Z", false);
        assertTrue(GraficaHistorica.picos(s).isEmpty());
        assertArrayEquals(new double[]{0.036, 0.0972}, GraficaHistorica.limites(s, Magnitud.O3), 1e-9);
    }

    @Test public void sinDatosNoInventaCurvaNiEscala() {
        Serie s = serie("2026-11-22T00:00:00Z", false);
        Arrays.fill(s.valores, null);
        assertNull(GraficaHistorica.limites(s, Magnitud.O3));
        assertTrue(GraficaHistorica.picos(s).isEmpty());
        assertNull(GraficaHistorica.limites(new Serie(new Double[0], null), Magnitud.O3));
    }

    @Test public void horasRedondasSeDesplazanRespectoAlInicio0035() {
        Serie s = serie("2026-11-21T23:35:00Z", false);
        List<GraficaHistorica.Marca> marcas = GraficaHistorica.marcas(s, 400, 60);
        assertEquals(Arrays.asList("06:00", "12:00", "18:00", "00:00"), marcas.stream().map(m -> m.hora).collect(Collectors.toList()));
        assertEquals(ms("2026-11-22T05:00:00Z"), marcas.get(0).instante);
        assertEquals(325.0 / 1440, marcas.get(0).posicion, 1e-9);
        assertEquals("23/11", marcas.get(3).fecha);
        assertEquals(ms("2026-11-21T23:35:00Z"), s.desde);
    }

    @Test public void pantallaEstrechaReduceEtiquetasSinSuperponerlas() {
        Serie s = serie("2026-11-21T23:35:00Z", false);
        List<GraficaHistorica.Marca> estrechas = GraficaHistorica.marcas(s, 140, 60);
        assertTrue(estrechas.size() < GraficaHistorica.marcas(s, 500, 60).size());
        double derecha = -1;
        for (GraficaHistorica.Marca m : estrechas) {
            double izquierda = Math.max(0, Math.min(80, m.posicion * 140 - 30));
            assertTrue(izquierda >= derecha);
            derecha = izquierda + 60;
        }
        assertTrue(GraficaHistorica.marcas(s, 30, 60).isEmpty());
    }

    @Test public void etiquetaCercanaAlBordeNoSeSuperponeALaAnterior() {
        Serie s = serie("2026-11-21T23:35:00Z", false);
        // A 320 px, cuatro cajas de 60 px colisionan al ajustar la última al borde.
        List<GraficaHistorica.Marca> marcas = GraficaHistorica.marcas(s, 320, 60);
        assertEquals(Arrays.asList("12:00", "00:00"), marcas.stream().map(m -> m.hora).collect(Collectors.toList()));
    }

    @Test public void picoDeSieteDiasUsaElInstanteYNoElCentroDelTramo() {
        Serie s = serie("2026-11-16T23:35:00Z", true);
        long instante = ms("2026-11-20T11:35:00Z");
        pico(s, instante, 0.15);
        GraficaHistorica.Pico pico = GraficaHistorica.picos(s).get(0);
        assertEquals(0.5, pico.posicion, 1e-9);
        assertEquals(instante, pico.evento.desde);
        assertEquals(0.15, pico.evento.valor, 0);
    }

    @Test public void sieteDiasMarcaMedianochesLocales() {
        Serie s = serie("2026-11-16T23:35:00Z", true);
        List<GraficaHistorica.Marca> marcas = GraficaHistorica.marcas(s, 1000, 60);
        assertEquals(7, marcas.size());
        assertEquals("18/11", marcas.get(0).fecha);
        assertEquals("24/11", marcas.get(6).fecha);
        for (GraficaHistorica.Marca m : marcas) {
            assertEquals("00:00", m.hora);
            assertTrue(m.instante >= s.desde && m.instante <= s.hasta);
        }
    }

    @Test public void primaveraNoInventaLaHoraLocalQueNoExiste() {
        Serie s = serie("2026-03-28T23:00:00Z", false);
        List<GraficaHistorica.Marca> marcas = GraficaHistorica.marcas(s, 3000, 60);
        assertFalse(marcas.stream().anyMatch(m -> m.fecha.equals("29/03") && m.hora.equals("02:00")));
        assertTrue(marcas.stream().anyMatch(m -> m.fecha.equals("29/03") && m.hora.equals("03:00")));
    }

    @Test public void otonoDistingueLasDosHorasRepetidasPorOffset() {
        Serie s = serie("2026-10-24T22:00:00Z", false);
        List<GraficaHistorica.Marca> marcas = GraficaHistorica.marcas(s, 3000, 60);
        List<GraficaHistorica.Marca> repetidas = marcas.stream().filter(m -> m.hora.startsWith("02:00")).collect(Collectors.toList());
        assertEquals(2, repetidas.size());
        assertEquals("02:00 +02", repetidas.get(0).hora);
        assertEquals("02:00 +01", repetidas.get(1).hora);
        assertEquals(3600000, repetidas.get(1).instante - repetidas.get(0).instante);
    }

    @Test public void diasDe23Y25HorasTienenSuAnchuraReal() {
        Serie primavera = serie("2026-03-25T00:00:00Z", true);
        Serie otono = serie("2026-10-22T00:00:00Z", true);
        List<GraficaHistorica.Marca> a = GraficaHistorica.marcas(primavera, 1000, 60);
        List<GraficaHistorica.Marca> b = GraficaHistorica.marcas(otono, 1000, 60);
        assertTrue(haySeparacion(a, 23 * 3600000L));
        assertTrue(haySeparacion(b, 25 * 3600000L));
    }

    private static boolean haySeparacion(List<GraficaHistorica.Marca> marcas, long paso) {
        for (int i = 1; i < marcas.size(); i++) {
            if (marcas.get(i).instante - marcas.get(i - 1).instante == paso) return true;
        }
        return false;
    }

    @Test public void sinMetadatosNoInventaHorasNiPosicionesDePicos() {
        Serie s = new Serie(new Double[]{0.05, 0.06}, null);
        pico(s, ms("2026-11-22T12:00:00Z"), 0.1);
        assertTrue(GraficaHistorica.marcas(s, 320, 60).isEmpty());
        assertTrue(Double.isNaN(GraficaHistorica.picos(s).get(0).posicion));
    }
}
