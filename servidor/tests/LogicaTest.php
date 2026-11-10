<?php
// ==============================================================
// Fichero:     LogicaTest.php
// Descripción: Tests de la lógica del negocio con una BD SQLite en memoria.
//              Ejecutar:  php servidor/tests/LogicaTest.php
// Autor:       Aaron Blasco Blay
// Fecha:       06/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================

require_once __DIR__ . '/../logica/Logica.php';
require_once __DIR__ . '/bd_prueba.php';

$fallos = 0;
$total = 0;
function prueba(string $nombre, callable $f): void {
    global $fallos, $total;
    $total++;
    try {
        $f();
        echo "  ok    $nombre\n";
    } catch (Throwable $e) {
        $fallos++;
        echo "  FALLA $nombre: " . $e->getMessage() . "\n";
    }
}
function igual($esperado, $real, string $que = ''): void {
    if ($esperado !== $real) {
        throw new Exception("$que: esperaba " . var_export($esperado, true) . ' y salió ' . var_export($real, true));
    }
}
function lanza(string $clase, callable $f): void {
    try {
        $f();
    } catch (Throwable $e) {
        if ($e instanceof $clase) {
            return;
        }
        throw new Exception("esperaba $clase y salió " . get_class($e) . ': ' . $e->getMessage());
    }
    throw new Exception("esperaba $clase y no lanzó nada");
}

// Reloj de prueba: 06/11/2026 10:00:00 UTC, se puede mover
$AHORA = strtotime('2026-11-06 10:00:00 UTC') * 1000;
function nueva(): Logica {
    global $AHORA;
    return new Logica(crearBDPrueba(), '', function () { global $AHORA; return $AHORA; });
}
// Centro del Parque Central y un punto del barrio Centro fuera de los parques
$Z = json_decode(file_get_contents(__DIR__ . '/zonas_prueba.json'), true);
$CENTRAL = $Z['central'];
$CENTRO_BARRIO = $Z['centro_barrio'];
function med(array $extra = []): array {
    global $AHORA, $CENTRAL;
    return array_merge(['nodo' => 'RSP-4F7K', 'magnitud' => 'o3', 'valor' => 0.042, 'contador' => 7,
        'medida_en' => $AHORA - 60000, 'lat' => $CENTRAL[0], 'lng' => $CENTRAL[1], 'precision' => 8, 'rssi' => -60], $extra);
}

echo "LogicaTest\n";

prueba('guarda una medición y le asigna el parque donde se tomó', function () {
    $r = nueva()->guardarMedicion(med());
    igual('central', $r['zona'], 'zona');
    igual(true, $r['valida'], 'valida');
    igual(0.042, $r['valor'], 'valor');
});

prueba('un punto de un barrio fuera de los parques cuenta para el barrio', function () {
    global $CENTRO_BARRIO;
    $r = nueva()->guardarMedicion(med(['lat' => $CENTRO_BARRIO[0], 'lng' => $CENTRO_BARRIO[1]]));
    igual('centro', $r['zona'], 'zona');
});

prueba('sin ubicación se guarda pero sin zona', function () {
    $r = nueva()->guardarMedicion(med(['lat' => null, 'lng' => null]));
    igual(null, $r['zona'], 'zona');
});

prueba('el tipo del beacon (12) se traduce a temperatura', function () {
    $d = med(['valor' => 21.4]);
    unset($d['magnitud']);
    $d['tipo'] = 12;
    igual('temp', nueva()->guardarMedicion($d)['magnitud'], 'magnitud');
});

prueba('el mismo anuncio oído dos veces (o por dos móviles) se guarda una sola vez', function () {
    global $AHORA;
    $l = nueva();
    igual(true, $l->guardarMedicion(med()) !== null, 'primera');
    igual(null, $l->guardarMedicion(med(['medida_en' => $AHORA - 30000])), 'segunda');
    // el contador da la vuelta (0..255): el mismo número 10 min después es otra medición
    igual(true, $l->guardarMedicion(med(['medida_en' => $AHORA])) === null, 'mismo minuto');
    igual(true, $l->guardarMedicion(med(['medida_en' => $AHORA - 11 * 60000])) !== null, '10 min antes');
    // otra magnitud con el mismo contador no es repetida
    igual(true, $l->guardarMedicion(med(['magnitud' => 'temp', 'valor' => 20.0])) !== null, 'otra magnitud');
});

prueba('un valor fuera del rango del sensor se guarda como no válido y no cuenta en la media', function () {
    $l = nueva();
    $r = $l->guardarMedicion(med(['valor' => 9.0]));
    igual(false, $r['valida'], 'valida');
    $central = array_values(array_filter($l->zonas(), fn($z) => $z['id'] === 'central'))[0];
    igual(null, $central['o3'], 'media');
});

prueba('rechaza datos mal formados', function () {
    $l = nueva();
    lanza('DatosNoValidosException', fn() => $l->guardarMedicion(med(['nodo' => 'XX-1'])));
    lanza('DatosNoValidosException', fn() => $l->guardarMedicion(med(['magnitud' => 'co2'])));
    lanza('DatosNoValidosException', fn() => $l->guardarMedicion(med(['valor' => 'abc'])));
    lanza('DatosNoValidosException', fn() => $l->guardarMedicion(med(['contador' => 300])));
    lanza('DatosNoValidosException', fn() => $l->guardarMedicion(med(['lng' => null])));
    lanza('DatosNoValidosException', fn() => $l->guardarMedicion(med(['lat' => 120])));
});

prueba('rechaza mediciones del futuro y de hace más de 7 días', function () {
    global $AHORA;
    $l = nueva();
    lanza('DatosNoValidosException', fn() => $l->guardarMedicion(med(['medida_en' => $AHORA + 3600000])));
    lanza('DatosNoValidosException', fn() => $l->guardarMedicion(med(['medida_en' => $AHORA - 8 * 86400000])));
});

prueba('un sensor que no existe da NoEncontrado', function () {
    lanza('NoEncontradoException', fn() => nueva()->guardarMedicion(med(['nodo' => 'RSP-ZZZZ'])));
});

prueba('zonas(): media de la última hora, minutos desde el último dato y zonas sin datos', function () {
    global $AHORA;
    $l = nueva();
    $l->guardarMedicion(med(['valor' => 0.040, 'contador' => 1, 'medida_en' => $AHORA - 20 * 60000]));
    $l->guardarMedicion(med(['valor' => 0.060, 'contador' => 2, 'medida_en' => $AHORA - 5 * 60000]));
    $l->guardarMedicion(med(['magnitud' => 'temp', 'valor' => 21.0, 'contador' => 2, 'medida_en' => $AHORA - 5 * 60000]));
    // de hace 3 h: fuera de la ventana de la última hora
    $l->guardarMedicion(med(['valor' => 0.300, 'contador' => 3, 'medida_en' => $AHORA - 180 * 60000]));
    $zonas = [];
    foreach ($l->zonas() as $z) {
        $zonas[$z['id']] = $z;
    }
    igual(0.05, $zonas['central']['o3'], 'media o3');
    igual(21.0, $zonas['central']['temp'], 'media temp');
    igual(null, $zonas['central']['ruido'], 'ruido sin datos');
    igual(5, $zonas['central']['hace_min'], 'hace_min');
    igual(2, $zonas['central']['n'], 'n');
    igual(null, $zonas['ribera']['hace_min'], 'ribera sin datos');
    igual(true, count($zonas['central']['poligono']) >= 3, 'poligono');
});

prueba('una zona cuyo último dato tiene más de 24 h queda sin datos', function () {
    global $AHORA;
    $l = nueva();
    $l->guardarMedicion(med(['medida_en' => $AHORA - 30 * 3600000]));
    $central = array_values(array_filter($l->zonas(), fn($z) => $z['id'] === 'central'))[0];
    igual(null, $central['hace_min'], 'hace_min');
});

prueba('serie de 24 h: 48 tramos de 30 min, con huecos donde no hubo datos', function () {
    global $AHORA;
    $l = nueva();
    $l->guardarMedicion(med(['valor' => 0.050, 'contador' => 1, 'medida_en' => $AHORA - 10 * 60000]));
    $l->guardarMedicion(med(['valor' => 0.070, 'contador' => 2, 'medida_en' => $AHORA - 20 * 60000]));
    $l->guardarMedicion(med(['valor' => 0.020, 'contador' => 3, 'medida_en' => $AHORA - 23 * 3600000]));
    $s = $l->serie('central', 'o3', '24h');
    igual(48, count($s['valores']), 'tramos');
    igual(0.06, $s['valores'][47], 'último tramo');
    igual(0.02, $s['valores'][1], 'tramo de hace 23 h (entre 23,5 y 23 h)');
    igual(null, $s['valores'][20], 'hueco');
    igual(30, $s['paso_min'], 'paso');
});

prueba('serie de 7 y 30 días y por rango de fechas', function () {
    $l = nueva();
    igual(56, count($l->serie('central', 'temp', '7d')['valores']), '7d');
    igual(60, count($l->serie('central', 'temp', '30d')['valores']), '30d');
    igual(60, count($l->serie('central', 'temp', 'rango', '2026-11-01', '2026-11-05')['valores']), 'rango');
    lanza('DatosNoValidosException', fn() => $l->serie('central', 'temp', 'rango', '2026-11-05', '2026-11-01'));
    lanza('DatosNoValidosException', fn() => $l->serie('central', 'temp', '1h'));
    lanza('NoEncontradoException', fn() => $l->serie('nada', 'temp', '24h'));
});

prueba('nodos(): último valor de cada magnitud y minutos desde el último dato', function () {
    global $AHORA;
    $l = nueva();
    $l->guardarMedicion(med(['valor' => 0.031, 'contador' => 1, 'medida_en' => $AHORA - 9 * 60000]));
    $l->guardarMedicion(med(['magnitud' => 'ruido', 'valor' => 58.4, 'contador' => 1, 'medida_en' => $AHORA - 9 * 60000]));
    $n = $l->nodos('RSP-4F7K')[0];
    igual(0.031, $n['ultima']['o3'], 'o3');
    igual(58.4, $n['ultima']['ruido'], 'ruido');
    igual(null, $n['ultima']['temp'], 'temp');
    igual(9, $n['ultima_min'], 'ultima_min');
    igual(['o3', 'temp', 'ruido'], $n['magnitudes'], 'magnitudes');
    lanza('NoEncontradoException', fn() => $l->nodos('RSP-0000'));
});

prueba('lote: cuenta guardadas, repetidas y errores sin pararse', function () {
    $r = nueva()->guardarMediciones([med(), med(), med(['nodo' => 'mal']), 'no es objeto', med(['contador' => 8])]);
    igual(2, $r['guardadas'], 'guardadas');
    igual(1, $r['repetidas'], 'repetidas');
    igual(2, count($r['errores']), 'errores');
    igual(2, $r['errores'][0]['i'], 'índice');
});

echo "\n" . ($total - $fallos) . " de $total tests correctos\n";
exit($fallos ? 1 : 0);
