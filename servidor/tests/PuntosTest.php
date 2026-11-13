<?php
// ==============================================================
// Fichero:     PuntosTest.php
// Descripción: Tests de Puntos: portador de las mediciones, cálculo de
//              un día (huecos, vehículo, parado, parques), cierre de
//              días, saldo, canjes, caducidad, revisiones y reglas.
//              Ejecutar:  php servidor/tests/PuntosTest.php
// Autor:       Aaron Blasco Blay
// Fecha:       09/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================

require_once __DIR__ . '/../logica/Logica.php';
require_once __DIR__ . '/../logica/Puntos.php';
require_once __DIR__ . '/bd_prueba.php';
require_once __DIR__ . '/mini_test.php';

// 8 nov 2026, 10:00 en España (09:00 UTC)
$T = strtotime('2026-11-08 09:00:00 UTC') * 1000;
$Z = json_decode(file_get_contents(__DIR__ . '/zonas_prueba.json'), true);

function entorno(): array {
    global $T;
    $bd = crearBDPrueba();
    $reloj = function () use (&$T) { return $T; };
    crearUsuario($bd, 'U-48213', 'vecino', 'lucia@correo.es', 'Respira2026');
    crearUsuario($bd, 'A-0001', 'admin', 'admin@ayto.es', 'Admin2026!');
    $bd->exec("INSERT INTO prestamo (nodo, usuario_id, punto_reparto_id, inicio, fecha_limite, estado_devolucion)
        VALUES ('RSP-4F7K', 'U-48213', 1, '2026-11-01 08:00:00.000', '2026-11-11 08:00:00.000', 'No devuelto')");
    $bd->exec("UPDATE nodo SET estado = 'prestado'");
    return [$bd, new Logica($bd, '', $reloj), new Puntos($bd, '', $reloj)];
}

// Un día andando por el barrio Centro (fuera de los parques): una lectura por
// minuto desde las 9:00 UTC, unos 60 m cada minuto. Puntos = minutos / 5 (máx. 100)
function andar(PDO $bd, string $dia, int $minutos): void {
    $st = $bd->prepare("INSERT INTO medicion (id_nodo, tipo, valor, fecha_medicion, lat, lng, zona_id, portador_id, origen)
        VALUES ('RSP-4F7K', 'temp', ?, ?, 38.9628, ?, 'centro', 'U-48213', 'real')");
    $ini = strtotime("$dia 09:00:00 UTC") * 1000;
    for ($k = 0; $k <= $minutos; $k++) {
        $paso = $k % 120 < 60 ? $k % 120 : 120 - $k % 120;
        $st->execute([20 + ($k % 30) / 10, Base::fechaBD($ini + $k * 60000), round(-0.204 + 0.000693 * $paso, 6)]);
    }
}

// Recorrido: n mediciones cada «cada» min desde «desde», moviéndose dlat por paso
function paseo(Logica $l, int $desde, int $n, int $cada, array $ini, float $dlat, int &$cont, float $dlng = 0): void {
    $u = ['id' => 'U-48213', 'rol' => 'vecino'];
    for ($i = 0; $i < $n; $i++) {
        $t = $desde + $i * $cada * 60000;
        foreach (['o3' => 0.04, 'temp' => 20.0, 'ruido' => 55.0] as $m => $v) {
            $l->guardarMedicion(['nodo' => 'RSP-4F7K', 'magnitud' => $m, 'valor' => $v,
                'medida_en' => $t, 'lat' => $ini[0] + $i * $dlat, 'lng' => $ini[1] + $i * $dlng], $u);
        }
        $cont++;
    }
}

echo "PuntosTest\n";

prueba('fórmula: ⌊min/N⌋ + ⌊parque/N⌋·(mult−1), con máximo', function () {
    igual(32, Puntos::puntosDe(120, 40, ['min' => 5, 'mult' => 2, 'max' => 100]));
    igual(100, Puntos::puntosDe(600, 0, ['min' => 5, 'mult' => 2, 'max' => 100]));
    igual(40, Puntos::puntosDe(120, 40, ['min' => 5, 'mult' => 3, 'max' => 100]));
});

prueba('portador: solo quien tiene el sensor prestado; sin sesión, sin portador', function () {
    global $T;
    [$bd, $l] = entorno();
    global $Z;
    $m = ['nodo' => 'RSP-4F7K', 'magnitud' => 'o3', 'valor' => 0.041, 'medida_en' => $T - 1000,
        'lat' => $Z['central'][0], 'lng' => $Z['central'][1]];
    $l->guardarMedicion($m, ['id' => 'U-48213', 'rol' => 'vecino']);
    $l->guardarMedicion(array_merge($m, ['valor' => 0.042]), ['id' => 'U-99999', 'rol' => 'vecino']);
    $l->guardarMedicion(array_merge($m, ['valor' => 0.043]), null);
    $l->guardarMedicion(array_merge($m, ['valor' => 0.044, 'portador' => 'U-48213']), null);   // el móvil no lo decide
    igual(['U-48213', null, null, null], array_map(function ($f) { return $f['portador_id']; },
        $bd->query('SELECT portador_id FROM medicion ORDER BY valor')->fetchAll(PDO::FETCH_ASSOC)));
});

prueba('día: andando por un parque cuentan los minutos y valen el doble', function () {
    global $T, $Z;
    [, $l, $pu] = entorno();
    $cont = 0;
    // 61 puntos cada 2 min = 120 min, avanzando ~5 m/min (dentro del parque central)
    paseo($l, $T - 6 * 3600000, 61, 2, [$Z['central'][0] - 0.0015, $Z['central'][1]], 0.00005, $cont);
    $d = $pu->calcularDia('U-48213', '2026-11-08', $T);
    igual(120, $d['minutos']);
    igual(120, $d['minutos_parque']);
    igual(48, $d['puntos'], '120/5 + 120/5');
    igual(null, $d['motivo']);
});

prueba('recorrido: inicio, fin, duración, distancia, lecturas y parques del día', function () {
    global $T, $Z;
    [, $l, $pu] = entorno();
    $cont = 0;
    // 61 puntos cada 2 min (120 min) avanzando 0,00005° de latitud por paso (~5,6 m)
    paseo($l, $T - 6 * 3600000, 61, 2, [$Z['central'][0] - 0.0015, $Z['central'][1]], 0.00005, $cont);
    $d = $pu->calcularDia('U-48213', '2026-11-08', $T);
    igual(Base::fechaISO($T - 6 * 3600000), $d['inicio']);
    igual(Base::fechaISO($T - 6 * 3600000 + 120 * 60000), $d['fin']);
    igual(120, $d['duracion_min']);
    igual(61, $d['lecturas'], 'una por anuncio (las 3 magnitudes juntas)');
    cierto(abs($d['distancia_m'] - 334) <= 3, 'unos 60 × 5,56 m = 334 m; da ' . $d['distancia_m']);
    igual(['Parque Central'], $d['parques']);
    // en vehículo no suma distancia
    [, $l, $pu] = entorno();
    $cont = 0;
    paseo($l, $T - 6 * 3600000, 6, 1, [$Z['centro_barrio'][0], -0.2045], 0, $cont, 0.0055);
    igual(0, $pu->calcularDia('U-48213', '2026-11-08', $T)['distancia_m'], 'vehículo');
    // día sin datos
    $v = $pu->calcularDia('U-48213', '2026-11-01', null);
    igual([null, null, 0, 0, 0, []], [$v['inicio'], $v['fin'], $v['duracion_min'], $v['distancia_m'], $v['lecturas'], $v['parques']]);
});

prueba('día: huecos de más de 15 min y velocidad de vehículo no cuentan', function () {
    global $T, $Z;
    [, $l, $pu] = entorno();
    $cont = 0;
    $b = [$Z['centro_barrio'][0], $Z['centro_barrio'][1]];
    paseo($l, $T - 6 * 3600000, 11, 2, $b, 0.00005, $cont);                    // 20 min andando
    paseo($l, $T - 5 * 3600000, 11, 2, $b, 0.00005, $cont);                    // hueco de 40 min antes
    paseo($l, $T - 4 * 3600000, 6, 2, [$b[0], -0.2045], 0, $cont, 0.0025);      // ~ 215 m/2 min = 6,5 km/h
    $d = $pu->calcularDia('U-48213', '2026-11-08', $T);
    igual(50, $d['minutos'], '20 + 20 + 10');
    // en autobús: ~ 475 m en 1 min = 28 km/h
    [, $l, $pu] = entorno();
    $cont = 0;
    paseo($l, $T - 6 * 3600000, 6, 1, [$b[0], -0.2045], 0, $cont, 0.0055);
    igual(0, $pu->calcularDia('U-48213', '2026-11-08', $T)['minutos'], 'vehículo');
});

prueba('día: parado más de 15 min seguidos no cuenta (y se marca si es mucho)', function () {
    global $T, $Z;
    [, $l, $pu] = entorno();
    $cont = 0;
    $b = [$Z['centro_barrio'][0], $Z['centro_barrio'][1]];
    paseo($l, $T - 8 * 3600000, 11, 2, $b, 0.00005, $cont);                  // 20 min andando
    paseo($l, $T - 8 * 3600000 + 22 * 60000, 91, 2, [$b[0] + 0.0006, $b[1]], 0, $cont);   // 3 h quieto
    $d = $pu->calcularDia('U-48213', '2026-11-08', $T);
    igual(22, $d['minutos'], 'solo lo andado (+ el paso al punto donde se para)');
    cierto($d['descartados'] >= 180, 'descartados');
    cierto(strpos((string) $d['motivo'], 'sin moverse') !== false, 'motivo');
});

prueba('días: los acabados suman al saldo (sin guardarse); hoy va aparte; nada que revisar', function () {
    global $T, $Z;
    [$bd, $l, $pu] = entorno();
    $cont = 0;
    paseo($l, $T - 30 * 3600000, 31, 2, [$Z['central'][0] - 0.0015, $Z['central'][1]], 0.00005, $cont); // ayer
    paseo($l, $T - 2 * 3600000, 16, 2, [$Z['central'][0] - 0.0015, $Z['central'][1]], 0.00005, $cont);  // hoy
    $pu->cerrarDias(null);
    $d = $pu->dias('U-48213');
    igual(['2026-11-07'], array_keys($d));
    igual([60, 24], [$d['2026-11-07']['minutos'], $d['2026-11-07']['puntos']]);
    igual(0, (int) $bd->query('SELECT COUNT(*) FROM revision')->fetchColumn(), 'sin días sospechosos');
    $c = $pu->cuenta(['id' => 'U-48213']);
    igual(24, $c['puntos']);
    igual(30, $c['hoy']['minutos'], 'hoy en directo');
});

prueba('canje: resta puntos y stock; sin saldo o agotado, no', function () {
    global $T;
    [$bd, , $pu] = entorno();
    andar($bd, '2026-11-01', 450);      // 90 puntos
    andar($bd, '2026-11-02', 550);      // 100 (máximo diario)
    $bd->exec("INSERT INTO medicion (id_nodo, tipo, valor, fecha_medicion, lat, lng, zona_id, portador_id, origen)
        VALUES ('RSP-4F7K', 'temp', 20, '2026-11-03 09:00:00.000', 38.9628, -0.2, 'centro', 'U-48213', 'real')");
    andar($bd, '2026-11-04', 50);       // 10 → 200 en total
    $u = ['id' => 'U-48213', 'rol' => 'vecino'];
    $c = $pu->canjear($u, 'g2');
    cierto((bool) preg_match('/^CJ-[A-Z0-9]{4}$/', $c['codigo']), 'código');
    igual('2026-11-15', $c['caduca'], '7 días');
    igual(50, $pu->saldo('U-48213'));
    igual(11, (int) $bd->query("SELECT stock FROM regalo WHERE id = 'g2'")->fetchColumn());
    $e = lanza(ConflictoException::class, function () use ($pu, $u) { $pu->canjear($u, 'g3'); });
    igual('Te faltan 200 puntos para este regalo.', $e->getMessage());
    lanza(ConflictoException::class, function () use ($pu, $u) { $pu->canjear($u, 'g1'); });   // stock 0
    lanza(NoAutorizadoException::class, function () use ($pu) { $pu->canjear(null, 'g2'); });
});

prueba('canje: sin recoger en 7 días caduca, devuelve puntos y unidad', function () {
    global $T;
    [$bd, , $pu] = entorno();
    andar($bd, '2026-11-01', 450);      // 90 puntos
    andar($bd, '2026-11-02', 550);      // 100 (máximo diario)
    $bd->exec("INSERT INTO medicion (id_nodo, tipo, valor, fecha_medicion, lat, lng, zona_id, portador_id, origen)
        VALUES ('RSP-4F7K', 'temp', 20, '2026-11-03 09:00:00.000', 38.9628, -0.2, 'centro', 'U-48213', 'real')");
    andar($bd, '2026-11-04', 50);       // 10 → 200 en total
    $c = $pu->canjear(['id' => 'U-48213', 'rol' => 'vecino'], 'g2');
    $T += 9 * 86400000;
    $admin = ['id' => 'A-0001', 'rol' => 'admin'];
    $e = lanza(ConflictoException::class, function () use ($pu, $admin, $c) { $pu->comprobarCanje($admin, $c['codigo']); });
    cierto(strpos($e->getMessage(), 'caducó') !== false, 'mensaje');
    igual(200, $pu->saldo('U-48213'));
    igual(12, (int) $bd->query("SELECT stock FROM regalo WHERE id = 'g2'")->fetchColumn());
    $T -= 9 * 86400000;
});

prueba('entrega: reparto la marca; no se puede entregar dos veces', function () {
    [$bd, , $pu] = entorno();
    andar($bd, '2026-11-01', 450);      // 90 puntos
    andar($bd, '2026-11-02', 550);      // 100 (máximo diario)
    $bd->exec("INSERT INTO medicion (id_nodo, tipo, valor, fecha_medicion, lat, lng, zona_id, portador_id, origen)
        VALUES ('RSP-4F7K', 'temp', 20, '2026-11-03 09:00:00.000', 38.9628, -0.2, 'centro', 'U-48213', 'real')");
    andar($bd, '2026-11-04', 50);       // 10 → 200 en total
    $c = $pu->canjear(['id' => 'U-48213', 'rol' => 'vecino'], 'g2');
    $rep = ['id' => 'A-0003', 'rol' => 'reparto'];
    igual('Gorra Respira', $pu->comprobarCanje($rep, strtolower($c['codigo']))['regalo']);
    igual('entregado', $pu->entregarCanje($rep, $c['codigo'])['estado']);
    lanza(ConflictoException::class, function () use ($pu, $rep, $c) { $pu->entregarCanje($rep, $c['codigo']); });
    lanza(NoEncontradoException::class, function () use ($pu, $rep) { $pu->comprobarCanje($rep, 'CJ-ZZZZ'); });
    lanza(ProhibidoException::class, function () use ($pu) { $pu->comprobarCanje(['id' => 'A-0002', 'rol' => 'tecnico'], 'CJ-ZZZZ'); });
});

prueba('revisión: aprobar mantiene los puntos; anular los quita y guarda el motivo', function () {
    [$bd, , $pu] = entorno();
    andar($bd, '2026-11-05', 520);      // 100: «Llega al máximo diario»
    andar($bd, '2026-11-06', 450);      // 90: «Llega al 90 % del máximo diario»
    $admin = ['id' => 'A-0001', 'rol' => 'admin'];
    igual(2, count($pu->revisiones($admin)));
    $ids = array_column($pu->revisiones($admin), 'id', 'fecha');
    $ids = [$ids['2026-11-05'], $ids['2026-11-06']];
    igual('ok', $pu->revisar($admin, $ids[0], 'aprobar')['estado']);
    lanza(DatosNoValidosException::class, function () use ($pu, $admin, $ids) { $pu->revisar($admin, $ids[1], 'anular', 'corto'); });
    $x = $pu->revisar($admin, $ids[1], 'anular', 'El sensor estuvo en un coche');
    igual(['anulado', 'El sensor estuvo en un coche'], [$x['estado'], $x['razon']]);
    igual(100, $pu->saldo('U-48213'));
    lanza(ConflictoException::class, function () use ($pu, $admin, $ids) { $pu->revisar($admin, $ids[0], 'anular', 'otra vez no se puede'); });
});

prueba('reglas: se validan y se aplican desde mañana', function () {
    [, , $pu] = entorno();
    $admin = ['id' => 'A-0001', 'rol' => 'admin'];
    $e = lanza(ErroresDeCamposException::class, function () use ($pu, $admin) { $pu->guardarReglas($admin, '0', 'x', '9999'); });
    igual(['min', 'mult', 'max'], array_keys($e->errores));
    $r = $pu->guardarReglas($admin, '10', '3', '150');
    igual(5, $r['min'], 'hoy sigue igual');
    igual(['min' => 10, 'mult' => 3, 'max' => 150], $r['desde_manana']);
    igual(10, $pu->reglasDe('2026-11-09')['min']);
    lanza(ProhibidoException::class, function () use ($pu) { $pu->reglas(['id' => 'A-0002', 'rol' => 'tecnico']); });
});

resumen();
