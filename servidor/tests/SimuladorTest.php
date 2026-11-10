<?php
// ==============================================================
// Fichero:     SimuladorTest.php
// Descripción: Tests de los datos simulados, de la aproximación de
//              huecos cortos y de las alertas/picos del historial.
//              Ejecutar:  php servidor/tests/SimuladorTest.php
// Autor:       Aaron Blasco Blay
// Fecha:       10/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================

require_once __DIR__ . '/../logica/Logica.php';
require_once __DIR__ . '/../logica/Simulador.php';
require_once __DIR__ . '/bd_prueba.php';
require_once __DIR__ . '/mini_test.php';

$T = strtotime('2026-11-09 12:00:00 UTC') * 1000;
$reloj = function () use (&$T) { return $T; };

echo "SimuladorTest\n";

prueba('aproximar: rellena huecos cortos con dato a los dos lados', function () {
    $media = 1800000;
    igual([null, 2.0, null], Logica::aproximar([1.0, null, 3.0], $media, 1));
    igual([null, 1.5, 2.0, 2.5, null], Logica::aproximar([1.0, null, null, null, 3.0], $media, 1));
    igual(array_fill(0, 7, null), Logica::aproximar([1.0, null, null, null, null, null, 3.0], $media, 1), 'más de 2 h: gris');
    igual([null, null, null], Logica::aproximar([null, null, 3.0], $media, 1), 'sin dato antes: gris');
    igual([null, 2.0, null], Logica::aproximar([1.0, null, 3.0], 3 * 3600000, 1), 'tramos de 3 h: se aproxima 1 tramo');
});

prueba('eventos: ratos en Alto y picos en tramos no seguidos', function () {
    $v = [50.0, 66.0, 67.0, 60.0, 70.0, 52.0];
    $mx = [51.0, 68.0, 69.0, 61.0, 72.0, 53.0];
    $e = Logica::eventos($v, $mx, array_fill(0, 6, null), 'ruido', 0, 1800000);
    igual(2, count($e['alertas']));
    igual(['alto', 67.0], [$e['alertas'][0]['nivel'], $e['alertas'][0]['max']]);
    igual([72.0, 69.0, 51.0], array_column($e['picos'], 'valor'), 'el 68 está al lado del 69');
    $e = Logica::eventos([0.05, 0.07, 0.065, 0.04], [0.05, 0.07, 0.066, 0.04], array_fill(0, 4, null), 'o3', 0, 1800000);
    igual('medio', $e['alertas'][0]['nivel'], 'sin Alto, se enseñan los ratos en Medio');
});

prueba('valores: deterministas y dentro de lo normal para noviembre', function () {
    global $T;
    $z = ['id' => 'centro', 'tipo' => 'barrio'];
    igual(Simulador::medida($z, 'ruido', $T), Simulador::medida($z, 'ruido', $T), 'mismo instante, mismo valor');
    for ($t = $T - 7 * 86400000; $t < $T; $t += 3600000) {
        $o = Simulador::medida($z, 'o3', $t);
        $c = Simulador::medida($z, 'temp', $t);
        $r = Simulador::medida($z, 'ruido', $t);
        cierto($o > 0 && $o < 0.15, "o3 $o");
        cierto($c > 3 && $c < 35, "temp $c");
        cierto($r >= 32 && $r < 95, "ruido $r");
    }
});

prueba('simulador: todas las zonas con datos salvo oeste (Sin datos) y estacion (Antiguo)', function () use ($reloj) {
    global $T;
    $bd = crearBDPrueba();
    $s = new Simulador($bd, '', $reloj);
    for ($i = 0; $i < 7; $i++) {
        $s->alDia(true);        // el historial se rellena de 5 en 5 días
    }
    $min = $bd->query("SELECT MIN(medida_en) FROM medicion WHERE origen = 'simulado'")->fetchColumn();
    cierto(Logica::msDeFechaBD($min) <= $T - 29 * 86400000, '30 días de historial');
    $zonas = [];
    foreach ((new Logica($bd, '', $reloj))->zonas() as $z) {
        $zonas[$z['id']] = $z;
    }
    igual(null, $zonas['oeste']['hace_min'], 'oeste sin datos');
    cierto($zonas['estacion']['hace_min'] >= 180 && $zonas['estacion']['hace_min'] < 200, 'estacion antiguo');
    foreach (['central', 'centro', 'campus', 'norte'] as $id) {
        cierto($zonas[$id]['o3'] !== null && $zonas[$id]['ruido'] !== null && $zonas[$id]['media_24h']['temp'] !== null, $id);
        igual(true, $zonas[$id]['simulado'], "$id simulado");
    }
    $n = (int) $bd->query('SELECT COUNT(*) FROM medicion')->fetchColumn();
    $s->alDia(true);
    igual($n, (int) $bd->query('SELECT COUNT(*) FROM medicion')->fetchColumn(), 'sin tiempo nuevo no añade nada');
    $T += 3600000;
    $s->alDia(true);
    cierto((int) $bd->query('SELECT COUNT(*) FROM medicion')->fetchColumn() > $n, 'una hora después añade la hora');
    cierto((int) $bd->query("SELECT COUNT(*) FROM percepcion")->fetchColumn() === 0, 'sin cuentas de demo no inventa percepciones');
    $T -= 3600000;
});

prueba('los datos reales mandan sobre los simulados', function () use ($reloj) {
    global $T;
    $bd = crearBDPrueba();
    (new Simulador($bd, '', $reloj))->alDia(true);
    $l = new Logica($bd, '', $reloj);
    $Z = json_decode(file_get_contents(__DIR__ . '/zonas_prueba.json'), true);
    $l->guardarMedicion(['nodo' => 'RSP-4F7K', 'magnitud' => 'o3', 'valor' => 0.111, 'contador' => 1,
        'medida_en' => $T - 60000, 'lat' => $Z['central'][0], 'lng' => $Z['central'][1]]);
    foreach ($l->zonas() as $z) {
        if ($z['id'] === 'central') {
            igual([0.111, false], [$z['o3'], $z['simulado']]);
        }
    }
    $s = $l->serie('central', 'o3', '24h');
    igual(0.111, $s['valores'][47], 'en la gráfica, el tramo con dato real es el real');
});

resumen();
