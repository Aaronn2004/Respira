package com.nodia.respira.datos;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ZonasCostaTest {

    // --------------------------------------------------------------
    // # effects: comprueba el catálogo y sus anclas mediante aserciones.
    // +
    //     catalogoCoincideConLasOchoZonasDeLaWeb() --x
    // --------------------------------------------------------------
    @Test public void catalogoCoincideConLasOchoZonasDeLaWeb() {
        List<Zona> zonas = ZonasCosta.zonas();
        assertEquals(Arrays.asList("clot", "baladre", "castell", "ermita", "oller", "platja-nord", "venecia", "marenys"),
                zonas.stream().map(z -> z.id).collect(Collectors.toList()));
        assertEquals("Clot de la Mota", zonas.get(0).nombreCorto());
        assertArrayEquals(new double[]{39.002841, -0.166719}, zonas.get(0).posicionEtiqueta(), 0);
        assertEquals("Parc del Clot de la Mota", zonas.get(0).nombre);
        assertEquals(2, zonas.get(0).contornos().size());
        assertEquals(2, zonas.get(5).contornos().size());
        assertTrue(zonas.stream().noneMatch(z -> Zona.BARRIO.equals(z.tipo)));
    }

    // --------------------------------------------------------------
    // # effects: comprueba límites, orden de coordenadas y cierre de los anillos.
    // +
    //     losContornosSonGeograficosYCerrados() --x
    // --------------------------------------------------------------
    @Test public void losContornosSonGeograficosYCerrados() {
        int puntos = 0;
        for (Zona z : ZonasCosta.zonas()) {
            for (List<List<double[]>> poligono : z.contornos()) {
                for (List<double[]> anillo : poligono) {
                    assertArrayEquals(anillo.get(0), anillo.get(anillo.size() - 1), 0);
                    for (double[] punto : anillo) {
                        assertTrue(ZonasCosta.enAmbito(punto[0], punto[1]));
                        puntos++;
                    }
                }
            }
        }
        assertEquals(259, puntos);
        assertArrayEquals(new double[]{39.002595, -0.165423}, ZonasCosta.zonas().get(0).contorno.get(0), 0);
    }

    // --------------------------------------------------------------
    // # effects: comprueba que una API antigua no inventa datos en las zonas reales.
    // +
    //     unaRespuestaDelPrototipoNoReintroduceZonasFicticias() --x
    // --------------------------------------------------------------
    @Test public void unaRespuestaDelPrototipoNoReintroduceZonasFicticias() {
        Zona antigua = new Zona("central", Zona.PARQUE, "Parque Central", cuadrado(0, 4),
                0, 40, 0.08, 31.0, 65.0);
        List<Zona> zonas = ZonasCosta.conMediciones(Collections.singletonList(antigua));
        assertEquals(8, zonas.size());
        for (Zona zona : zonas) {
            for (Magnitud m : Magnitud.values()) {
                assertNull(zona.valor(m));
                assertEquals(Reglas.ESTADO_SIN_DATOS, zona.estado(m));
                assertEquals("", zona.dato(m).origen);
            }
        }
    }

    // --------------------------------------------------------------
    // # effects: comprueba que las lecturas conservan origen y hora sin alterar el contorno.
    // +
    //     unaLecturaCosteraSeFusionaSinCambiarSuGeografia() --x
    // --------------------------------------------------------------
    @Test public void unaLecturaCosteraSeFusionaSinCambiarSuGeografia() {
        long ahora = System.currentTimeMillis();
        Zona respuesta = new Zona("clot", Zona.BARRIO, "Nombre antiguo", cuadrado(0, 4),
                0, 8, null, null, null)
                .conDato(Magnitud.TEMP, new Zona.Dato(32.5, Reglas.ESTADO_OK, ahora, "simulado"))
                .conMedia24h(Magnitud.TEMP, 27.5);
        Zona fusionada = ZonasCosta.conMediciones(Collections.singletonList(respuesta)).get(0);
        assertEquals(Zona.PARQUE, fusionada.tipo);
        assertEquals("Parc del Clot de la Mota", fusionada.nombre);
        assertEquals("Clot de la Mota", fusionada.nombreCorto());
        assertEquals(2, fusionada.contornos().size());
        assertEquals(32.5, fusionada.valor(Magnitud.TEMP), 0);
        assertEquals("simulado", fusionada.dato(Magnitud.TEMP).origen);
        assertEquals(ahora, fusionada.dato(Magnitud.TEMP).momento);
        assertEquals(27.5, fusionada.media24h(Magnitud.TEMP), 0);
        assertArrayEquals(new double[]{39.002841, -0.166719}, fusionada.posicionEtiqueta(), 0);
        assertNull(fusionada.valor(Magnitud.O3));
    }

    // --------------------------------------------------------------
    // # effects: comprueba que una modificación local no corrompe futuros catálogos.
    // +
    //     cadaCatalogoTieneSusPropiasCoordenadas() --x
    // --------------------------------------------------------------
    @Test public void cadaCatalogoTieneSusPropiasCoordenadas() {
        ZonasCosta.zonas().get(0).contorno.get(0)[0] = 0;
        assertEquals(39.002595, ZonasCosta.zonas().get(0).contorno.get(0)[0], 0);
    }

    // --------------------------------------------------------------
    // # effects: comprueba el promedio general y la ausencia de magnitudes.
    // +
    //     elNivelGeneralCoincideConLaMediaDeLaWeb() --x
    // --------------------------------------------------------------
    @Test public void elNivelGeneralCoincideConLaMediaDeLaWeb() {
        assertEquals(Nivel.NINGUNO, zona(null, null, null).nivelGeneral());
        assertEquals(Nivel.MEDIO, zona(0.07, 28.0, 60.0).nivelGeneral());
        assertEquals(Nivel.MEDIO, zona(0.04, null, 70.0).nivelGeneral());
        assertEquals(Nivel.ALTO, zona(0.10, 35.0, 60.0).nivelGeneral());
        assertEquals(Nivel.BAJO, zona(0.04, null, null).nivelGeneral());
    }

    // --------------------------------------------------------------
    // # effects: comprueba polígonos separados y huecos interiores.
    // +
    //     unaZonaIncluyeTodosSusPoligonosYExcluyeLosHuecos() --x
    // --------------------------------------------------------------
    @Test public void unaZonaIncluyeTodosSusPoligonosYExcluyeLosHuecos() {
        Zona z = zona(null, null, null).conContornos(Arrays.asList(
                Arrays.asList(cuadrado(0, 4), cuadrado(1, 3)),
                Collections.singletonList(cuadrado(6, 8))));
        assertTrue(z.contiene(0.5, 0.5));
        assertFalse(z.contiene(2, 2));
        assertTrue(z.contiene(7, 7));
        assertFalse(z.contiene(5, 5));
    }

    // --------------------------------------------------------------
    // # effects: comprueba el ámbito público de avisos, igual al de la web.
    // +
    //     elAmbitoIncluyeLasCallesCosterasPeroExcluyeElPueblo() --x
    // --------------------------------------------------------------
    @Test public void elAmbitoIncluyeLasCallesCosterasPeroExcluyeElPueblo() {
        assertTrue(ZonasCosta.enAmbito(39.003, -0.175));
        assertTrue(ZonasCosta.enAmbito(38.981, -0.187));
        assertFalse(ZonasCosta.enAmbito(38.9677, -0.184));
        assertFalse(ZonasCosta.enAmbito(Double.NaN, -0.16));
        assertFalse(ZonasCosta.enAmbito(39, Double.POSITIVE_INFINITY));
    }

    // --------------------------------------------------------------
    // - o3: R | Ausente, temp: R | Ausente, ruido: R | Ausente
    //       -->
    //     zona() --x
    //       <--
    //   Zona
    // --------------------------------------------------------------
    private static Zona zona(Double o3, Double temp, Double ruido) {
        return new Zona("prueba", Zona.PARQUE, "Prueba", cuadrado(0, 4), 0, 3, o3, temp, ruido);
    }

    // --------------------------------------------------------------
    // - min: R, max: R
    //       -->
    //     cuadrado() --x
    //       <--
    //   [[R]_2]
    // --------------------------------------------------------------
    private static List<double[]> cuadrado(double min, double max) {
        return Arrays.asList(new double[]{min, min}, new double[]{min, max},
                new double[]{max, max}, new double[]{max, min}, new double[]{min, min});
    }
}
