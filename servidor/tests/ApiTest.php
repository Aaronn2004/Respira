<?php
// ==============================================================
// Fichero:     ApiTest.php
// Descripción: Tests de integración de la API: arranca el servidor web
//              de PHP con una BD SQLite temporal y hace peticiones HTTP
//              de verdad, como la app y la web.
//              Ejecutar:  php servidor/tests/ApiTest.php
// Autor:       Aaron Blasco Blay
// Fecha:       09/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================

require_once __DIR__ . '/bd_prueba.php';

$fichero = sys_get_temp_dir() . '/respira_api_test.sqlite';
@unlink($fichero);
$bdp = crearBDPrueba('sqlite:' . $fichero);
crearUsuario($bdp, 'U-48213', 'vecino', 'lucia@correo.es', 'Respira2026', ['nombre' => 'lucia']);
crearUsuario($bdp, 'A-0001', 'admin', 'admin@ayto.es', 'Admin2026!', ['nombre' => 'Ana García']);
crearUsuario($bdp, 'A-0003', 'reparto', 'reparto@ayto.es', 'Reparto2026!', ['nombre' => 'Marta Ruiz']);
$bdp = null;
$correo = sys_get_temp_dir() . '/respira_api_correo.txt';
@unlink($correo);

$puerto = 18000 + random_int(0, 999);
$raiz = realpath(__DIR__ . '/..');
$cmd = sprintf('RESPIRA_CORREO_ARCHIVO=%s RESPIRA_DSN=%s php -S 127.0.0.1:%d -t %s > /dev/null 2>&1 & echo $!',
    escapeshellarg($correo), escapeshellarg('sqlite:' . $fichero), $puerto, escapeshellarg($raiz));
$pid = (int) shell_exec($cmd);
usleep(600000);
$BASE = "http://127.0.0.1:$puerto/api/index.php";

function pedir(string $metodo, string $ruta, $cuerpo = null, ?string $token = null, array $cab = []): array {
    global $BASE;
    $h = "Content-Type: application/json\r\n" . ($token ? "X-Respira-Token: $token\r\n" : '');
    foreach ($cab as $k => $v) {
        $h .= "$k: $v\r\n";
    }
    $op = ['http' => ['method' => $metodo, 'ignore_errors' => true, 'header' => $h]];
    if ($cuerpo !== null) {
        $op['http']['content'] = is_string($cuerpo) ? $cuerpo : json_encode($cuerpo);
    }
    $r = file_get_contents($BASE . $ruta, false, stream_context_create($op));
    preg_match('/\s(\d{3})\s/', $http_response_header[0], $m);
    return [(int) $m[1], json_decode($r, true)];
}

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

function cierto($v, string $que = ''): void {
    if ($v !== true) {
        throw new Exception("$que: no se cumple");
    }
}

$Z = json_decode(file_get_contents(__DIR__ . '/zonas_prueba.json'), true);
$ahora = (int) round(microtime(true) * 1000);
$m = ['nodo' => 'RSP-4F7K', 'tipo' => 11, 'valor' => 0.045, 'contador' => 3, 'medida_en' => $ahora - 60000,
    'lat' => $Z['central'][0], 'lng' => $Z['central'][1], 'precision' => 6, 'rssi' => -61];

echo "ApiTest\n";
try {
    prueba('GET /estado responde 200', function () {
        [$c, $j] = pedir('GET', '/estado');
        igual(200, $c, 'código');
        igual(true, $j['ok'], 'ok');
    });
    prueba('POST /mediciones guarda (201) y devuelve la zona', function () use ($m) {
        [$c, $j] = pedir('POST', '/mediciones', $m);
        igual(201, $c, 'código');
        igual('central', $j['zona'], 'zona');
        igual('o3', $j['magnitud'], 'magnitud');
    });
    prueba('el mismo anuncio otra vez da 409', function () use ($m) {
        [$c] = pedir('POST', '/mediciones', $m);
        igual(409, $c, 'código');
    });
    prueba('datos no válidos dan 400 y JSON roto también', function () use ($m) {
        igual(400, pedir('POST', '/mediciones', array_merge($m, ['valor' => 9]))[0], 'fuera de rango');
        igual(400, pedir('POST', '/mediciones', array_merge($m, ['lat' => 40.4168, 'lng' => -3.7038]))[0], 'fuera de las zonas');
        igual(400, pedir('POST', '/mediciones', '{roto')[0], 'json');
    });
    prueba('un sensor que no existe da 404', function () use ($m) {
        igual(404, pedir('POST', '/mediciones', array_merge($m, ['nodo' => 'RSP-ZZZZ']))[0], 'código');
    });
    prueba('POST de un lote', function () use ($m) {
        [$c, $j] = pedir('POST', '/mediciones', ['mediciones' => [
            array_merge($m, ['tipo' => 12, 'valor' => 22.5]), array_merge($m, ['tipo' => 13, 'valor' => 61.2]), $m]]);
        igual(201, $c, 'código');
        igual(2, $j['guardadas'], 'guardadas');
        igual(1, $j['repetidas'], 'repetidas');
    });
    prueba('GET /zonas trae la media de Parque Central', function () {
        [$c, $j] = pedir('GET', '/zonas');
        igual(200, $c, 'código');
        $central = array_values(array_filter($j, fn($z) => $z['id'] === 'central'))[0];
        igual(0.045, $central['o3'], 'o3');
        igual(22.5, $central['temp'], 'temp');
        igual(61.2, $central['ruido'], 'ruido');
        igual(1, $central['hace_min'], 'hace_min');
    });
    prueba('GET /zonas/central/serie?magnitud=o3&periodo=24h', function () {
        [$c, $j] = pedir('GET', '/zonas/central/serie?magnitud=o3&periodo=24h');
        igual(200, $c, 'código');
        igual(48, count($j['valores']), 'tramos');
        igual(0.045, $j['valores'][47], 'último');
    });
    prueba('serie de una zona que no existe da 404', function () {
        igual(404, pedir('GET', '/zonas/nada/serie')[0], 'código');
    });
    prueba('GET /nodos y /nodos/RSP-4F7K', function () {
        [$c, $j] = pedir('GET', '/nodos');
        igual(200, $c, 'código');
        igual('RSP-4F7K', $j[0]['codigo'], 'codigo');
        [$c, $n] = pedir('GET', '/nodos/RSP-4F7K');
        igual(61.2, $n['ultima']['ruido'], 'ruido');
    });
    prueba('también vale index.php?ruta=/zonas', function () {
        igual(200, pedir('GET', '?ruta=/zonas')[0], 'código');
    });
    prueba('ruta desconocida da 404', function () {
        igual(404, pedir('GET', '/nada')[0], 'código');
    });

    // ---------------- cuentas, sesiones y permisos ----------------
    $TOK = [];
    prueba('POST /sesiones: 201 con token; 401 si la contraseña no es', function () use (&$TOK) {
        [$c, $j] = pedir('POST', '/sesiones', ['email' => 'lucia@correo.es', 'clave' => 'Respira2026', 'cliente' => 'app']);
        igual(201, $c, 'código');
        igual('U-48213', $j['usuario']['id']);
        $TOK['lucia'] = $j['token'];
        [$c, $j] = pedir('POST', '/sesiones', ['email' => 'admin@ayto.es', 'clave' => 'mal', 'cliente' => 'web']);
        igual([401, 'Correo o contraseña incorrectos.'], [$c, $j['error']]);
        [, $j] = pedir('POST', '/sesiones', ['email' => 'admin@ayto.es', 'clave' => 'Admin2026!', 'cliente' => 'web']);
        $TOK['admin'] = $j['token'];
        [, $j] = pedir('POST', '/sesiones', ['email' => 'reparto@ayto.es', 'clave' => 'Reparto2026!', 'cliente' => 'web']);
        $TOK['reparto'] = $j['token'];
        igual(403, pedir('POST', '/sesiones', ['email' => 'lucia@correo.es', 'clave' => 'Respira2026', 'cliente' => 'web'])[0], 'vecina en la web');
    });
    prueba('el token vale en X-Respira-Token y en Authorization: Bearer', function () use (&$TOK) {
        igual(200, pedir('GET', '/yo', null, $TOK['lucia'])[0]);
        igual(200, pedir('GET', '/yo', null, null, ['Authorization' => 'Bearer ' . $TOK['lucia']])[0]);
        igual(401, pedir('GET', '/yo')[0], 'sin token');
        igual(401, pedir('GET', '/panel/nodos', null, 'inventado')[0], 'token falso');
    });
    prueba('PUT /yo/seguimiento guarda favoritos y alertas; GET /yo los devuelve con el recorrido de hoy', function () {
        global $TOK;
        [$c, $j] = pedir('PUT', '/yo/seguimiento', ['favoritos' => ['ribera'],
            'alertas' => [['zona' => 'ribera', 'magnitud' => 'o3', 'condicion' => 'alto']]], $TOK['lucia']);
        igual(200, $c);
        igual(['ribera'], $j['favoritos']);
        [, $yo] = pedir('GET', '/yo', null, $TOK['lucia']);
        igual(['ribera'], $yo['seguimiento']['favoritos']);
        igual('alto', $yo['seguimiento']['alertas'][0]['condicion']);
        igual(['inicio', 'fin', 'duracion_min', 'distancia_m', 'lecturas', 'parques'], array_keys($yo['cuenta']['hoy']['recorrido']));
        igual(400, pedir('PUT', '/yo/seguimiento', ['favoritos' => ['luna']], $TOK['lucia'])[0]);
        igual(401, pedir('PUT', '/yo/seguimiento', ['favoritos' => []])[0]);
    });
    prueba('POST /usuarios: registro 201; errores por campo 400', function () {
        [$c, $j] = pedir('POST', '/usuarios', ['nombre' => 'marcos', 'email' => 'marcos@correo.es', 'clave' => 'Marcos2026', 'acepta' => true]);
        igual(201, $c);
        cierto(strlen($j['token']) === 64, 'token');
        [$c, $j] = pedir('POST', '/usuarios', ['nombre' => 'marcos', 'email' => 'x', 'clave' => '1', 'acepta' => false]);
        igual([400, ['nombre', 'email', 'clave', 'acepta']], [$c, array_keys($j['errores'])]);
    });
    prueba('panel: préstamo desde reparto y la app de Lucía ya lo ve', function () use (&$TOK) {
        igual(403, pedir('GET', '/panel/trabajadores', null, $TOK['reparto'])[0], 'reparto no ve trabajadores');
        [$c, $j] = pedir('POST', '/panel/prestamos', ['usuario' => 'U-48213', 'codigo' => 'RSP-4F7K', 'punto' => 1], $TOK['reparto']);
        igual([201, 'Biblioteca Central', 'No devuelto'], [$c, $j['punto'], $j['estado_devolucion']]);
        [$c, $j] = pedir('POST', '/yo/sensor', ['codigo' => 'rsp-4f7k'], $TOK['lucia']);
        igual([200, 'RSP-4F7K'], [$c, $j['codigo']]);
        [, $j] = pedir('GET', '/yo', null, $TOK['lucia']);
        igual('RSP-4F7K', $j['prestamo']['codigo']);
        cierto(strtotime($j['prestamo']['hasta']) - strtotime($j['prestamo']['desde']) === 10 * 86400, 'plazo de 10 días');
        igual(400, pedir('POST', '/panel/prestamos', ['usuario' => 'U-48213', 'codigo' => 'RSP-4F7K'], $TOK['reparto'])[0], 'repetido');
    });
    prueba('POST /mediciones con sesión: el portador es quien lleva el sensor', function () use (&$TOK, $m) {
        global $fichero;
        pedir('POST', '/mediciones', array_merge($m, ['valor' => 0.077, 'medida_en' => $m['medida_en'] + 2000]), $TOK['lucia']);
        $bd = new PDO('sqlite:' . $fichero);
        igual('U-48213', $bd->query('SELECT portador_id FROM medicion WHERE valor = 0.077')->fetchColumn());
    });
    prueba('percepciones: publicar, editar (PUT), borrar (DELETE) y 403 para otros', function () use (&$TOK) {
        [$c, $p] = pedir('POST', '/percepciones', ['tipo' => 'ruido', 'nivel' => 2, 'lat' => 38.9637, 'lng' => -0.1865, 'direccion' => 'C/ Mayor, 43'], $TOK['lucia']);
        igual([201, true], [$c, $p['mia']]);
        [, $l] = pedir('GET', '/percepciones');
        igual(false, $l[0]['mia'], 'sin sesión no es mía');
        [$c, $e] = pedir('PUT', '/percepciones/' . $p['id'], ['tipo' => 'aire', 'nivel' => 3], $TOK['lucia']);
        igual([200, 'aire'], [$c, $e['tipo']]);
        [, $r] = pedir('POST', '/sesiones', ['email' => 'marcos@correo.es', 'clave' => 'Marcos2026']);
        igual(403, pedir('DELETE', '/percepciones/' . $p['id'], null, $r['token'])[0], 'de otra persona');
        igual(200, pedir('POST', '/percepciones/' . $p['id'], null, $TOK['lucia'], ['X-HTTP-Method-Override' => 'DELETE'])[0], 'override');
        igual(0, count(pedir('GET', '/percepciones')[1]));
    });
    prueba('canje sin puntos da 409; regalos públicos', function () use (&$TOK) {
        [$c, $l] = pedir('GET', '/regalos');
        igual([200, 3], [$c, count(array_filter($l, fn($g) => $g['stock'] > 0))]);
        igual(409, pedir('POST', '/canjes', ['regalo' => 'g2'], $TOK['lucia'])[0]);
    });
    prueba('trabajador: alta con enlace, activación y entra en la app', function () use (&$TOK) {
        [$c, $j] = pedir('POST', '/panel/trabajadores', ['nombre' => 'Pablo Sanz Gil', 'email' => 'psanz@ayto.es', 'num_empleado' => '104233', 'servicio' => 'Limpieza viaria'], $TOK['admin']);
        igual(201, $c);
        preg_match('/#activar\/([0-9a-f]{64})$/', $j['enlace'], $mm);
        [, $v] = pedir('GET', '/enlaces/' . $mm[1]);
        igual(['valido', 'activar'], [$v['estado'], $v['tipo']]);
        igual(400, pedir('POST', '/enlaces/' . $mm[1], ['clave' => 'corta'])[0]);
        igual(200, pedir('POST', '/enlaces/' . $mm[1], ['clave' => 'Trabajo2026!'])[0]);
        igual(201, pedir('POST', '/sesiones', ['email' => 'psanz@ayto.es', 'clave' => 'Trabajo2026!'])[0]);
        igual(404, pedir('POST', '/enlaces/' . $mm[1], ['clave' => 'Trabajo2027!'])[0], 'ya usado');
    });
    prueba('recuperar: 202 siempre y el correo trae un enlace que funciona', function () {
        global $correo;
        igual(202, pedir('POST', '/recuperaciones', ['email' => 'nadie@correo.es'])[0]);
        igual(202, pedir('POST', '/recuperaciones', ['email' => 'lucia@correo.es'])[0]);
        // el último correo es el de Lucía (antes está el de activación del trabajador)
        preg_match_all('/#activar\/([0-9a-f]{64})/', (string) @file_get_contents($correo), $mm);
        cierto(count($mm[1]) === 2, 'dos correos: activación del trabajador y recuperación');
        igual(200, pedir('POST', '/enlaces/' . end($mm[1]), ['clave' => 'Nueva2026'])[0]);
        igual(201, pedir('POST', '/sesiones', ['email' => 'lucia@correo.es', 'clave' => 'Nueva2026'])[0]);
    });
    prueba('cerrar sesión: el token deja de valer', function () use (&$TOK) {
        [, $j] = pedir('POST', '/sesiones', ['email' => 'admin@ayto.es', 'clave' => 'Admin2026!', 'cliente' => 'web']);
        igual(200, pedir('DELETE', '/sesiones', null, $j['token'])[0]);
        igual(401, pedir('GET', '/panel/nodos', null, $j['token'])[0]);
    });
    prueba('CORS: deja pasar la cabecera del token y PUT/DELETE', function () {
        global $BASE;
        $h = get_headers($BASE . '/estado', true);
        cierto(strpos($h['Access-Control-Allow-Headers'], 'X-Respira-Token') !== false, 'cabecera');
        cierto(strpos($h['Access-Control-Allow-Methods'], 'DELETE') !== false, 'métodos');
    });
} finally {
    exec("kill $pid");
    @unlink($fichero);
    @unlink($correo);
}
echo "\n" . ($total - $fallos) . " de $total tests correctos\n";
exit($fallos ? 1 : 0);
