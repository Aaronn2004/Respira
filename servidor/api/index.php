<?php
// ==============================================================
// Fichero:     index.php  (API REST de Respira)
// Descripción: Traduce HTTP <-> lógica del negocio. Sin SQL ni reglas.
//              Rutas: tabla completa en doc/servidor.md.
//
//   Sesión: POST /sesiones devuelve un token. El cliente lo manda en
//   cada petición en la cabecera  X-Respira-Token: <token>
//   (también vale Authorization: Bearer <token>).
//
// URL en Plesk: https://<tu-dominio>/respira/api/index.php/zonas
//   (también vale index.php?ruta=/zonas, por si el servidor no pasa la ruta)
// Diseño:      doc/servidor.md
// Autor:       Aaron Blasco Blay
// Fecha:       09/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================

require_once __DIR__ . '/../logica/Logica.php';
require_once __DIR__ . '/../logica/Cuentas.php';
require_once __DIR__ . '/../logica/Puntos.php';
require_once __DIR__ . '/../logica/Comunidad.php';
require_once __DIR__ . '/../logica/Flota.php';
require_once __DIR__ . '/../logica/Simulador.php';

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, X-Respira-Token, Authorization, X-HTTP-Method-Override');
header('Cache-Control: no-store');

// --------------------------------------------------------------
// codigo: N, datos: Object --> responder()
// --------------------------------------------------------------
function responder(int $codigo, $datos): void {
    http_response_code($codigo);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($datos, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_PRESERVE_ZERO_FRACTION);
    exit;
}

// --------------------------------------------------------------
// rutaPedida() --> Text   ("/zonas/central/serie")
// --------------------------------------------------------------
function rutaPedida(): string {
    if (isset($_GET['ruta'])) {
        return '/' . trim($_GET['ruta'], '/');
    }
    $uri = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH);
    $pos = strpos($uri, 'index.php');
    $ruta = $pos === false ? $uri : substr($uri, $pos + strlen('index.php'));
    return '/' . trim($ruta, '/');
}

// token de sesión de la petición (o null)
function tokenPedido(): ?string {
    $t = $_SERVER['HTTP_X_RESPIRA_TOKEN'] ?? '';
    if ($t === '') {
        $a = $_SERVER['HTTP_AUTHORIZATION'] ?? ($_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '');
        if (stripos($a, 'Bearer ') === 0) {
            $t = trim(substr($a, 7));
        }
    }
    return $t === '' ? null : $t;
}

function cuerpo(): array {
    $c = json_decode(file_get_contents('php://input') ?: 'null', true);
    return is_array($c) ? $c : [];
}

// --------------------------------------------------------------
// config() --> {pdo, prefijo, url_web, correo}   (config.php solo existe en el servidor)
// --------------------------------------------------------------
function config(): array {
    $dsn = getenv('RESPIRA_DSN');            // tests: sqlite
    if ($dsn) {
        $c = ['pdo' => new PDO($dsn), 'prefijo' => getenv('RESPIRA_PREFIJO') ?: '', 'url_web' => 'http://localhost/web/',
            'simulacion' => getenv('RESPIRA_SIMULACION') === '1', 'bd' => $dsn];
    } else {
        $f = __DIR__ . '/config.php';
        if (!file_exists($f)) {
            responder(500, ['error' => 'Falta api/config.php en el servidor (copia config.example.php)']);
        }
        $c = require $f;
        try {
            $c['pdo'] = new PDO("mysql:host={$c['host']};dbname={$c['bd']};charset=utf8mb4", $c['usuario'], $c['clave'],
                [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);
            $c['pdo']->exec("SET time_zone = '+00:00'");
        } catch (PDOException $e) {
            responder(500, ['error' => 'No se pudo conectar con la base de datos']);
        }
    }
    if (empty($c['url_web'])) {
        // /respira/api/index.php --> https://dominio/respira/web/
        $base = dirname(dirname($_SERVER['SCRIPT_NAME'] ?? '/api/index.php'));
        $https = ($_SERVER['HTTPS'] ?? '') !== '' && ($_SERVER['HTTPS'] ?? '') !== 'off';
        $c['url_web'] = ($https ? 'https' : 'http') . '://' . ($_SERVER['HTTP_HOST'] ?? 'localhost') . rtrim($base, '/') . '/web/';
    }
    $archivo = getenv('RESPIRA_CORREO_ARCHIVO');     // tests: el correo se escribe en un archivo
    $remite = $c['correo_remitente'] ?? ('no-responder@' . preg_replace('/:\d+$/', '', $_SERVER['HTTP_HOST'] ?? 'localhost'));
    $c['correo'] = function ($para, $asunto, $texto) use ($archivo, $remite) {
        if ($archivo) {
            return (bool) file_put_contents($archivo, "$para\n$asunto\n$texto\n", FILE_APPEND);
        }
        return @mail($para, '=?UTF-8?B?' . base64_encode($asunto) . '?=', $texto,
            "From: Respira <$remite>\r\nContent-Type: text/plain; charset=UTF-8");
    };
    return $c;
}

$metodo = $_SERVER['REQUEST_METHOD'] ?? 'GET';
if ($metodo === 'POST' && isset($_SERVER['HTTP_X_HTTP_METHOD_OVERRIDE'])) {
    $metodo = strtoupper($_SERVER['HTTP_X_HTTP_METHOD_OVERRIDE']);   // por si el hosting no deja pasar PUT/DELETE
}
if ($metodo === 'OPTIONS') {
    http_response_code(204);
    exit;
}
$ruta = rutaPedida();
$p = $ruta === '/' ? [] : explode('/', trim($ruta, '/'));
$n = count($p);

try {
    $cfg = config();
    $bd = $cfg['pdo'];
    $pre = $cfg['prefijo'] ?? '';
    $logica = new Logica($bd, $pre);
    $cuentas = new Cuentas($bd, $pre, null, $cfg['correo']);
    $puntos = new Puntos($bd, $pre);
    $comunidad = new Comunidad($bd, $pre);
    $flota = new Flota($bd, $pre);
    // Datos simulados (Simulador.php): por defecto sí; 'simulacion' => false en config.php para quitarlos
    if ($cfg['simulacion'] ?? true) {
        try {
            (new Simulador($bd, $pre, null, sys_get_temp_dir() . '/respira_sim_' . md5((string) ($cfg['bd'] ?? '') . $pre) . '.txt'))->alDia();
        } catch (Throwable $e) {
            error_log('[Respira] Simulador: ' . $e->getMessage());
        }
    }
    $token = tokenPedido();
    $yo = $cuentas->usuarioDeSesion($token);
    $c = in_array($metodo, ['POST', 'PUT', 'DELETE'], true) ? cuerpo() : [];
    $r = $p[0] ?? '';

    // ---------------- público ----------------
    if ($metodo === 'GET' && $ruta === '/estado') {
        $logica->zonas();
        responder(200, ['ok' => true, 'hora' => Logica::fechaISO((int) round(microtime(true) * 1000))]);
    }
    if ($metodo === 'POST' && $ruta === '/mediciones') {
        if (!$c) {
            responder(400, ['error' => 'El cuerpo tiene que ser JSON']);
        }
        if (isset($c['mediciones'])) {
            if (!is_array($c['mediciones'])) {
                responder(400, ['error' => 'mediciones tiene que ser una lista']);
            }
            responder(201, $logica->guardarMediciones($c['mediciones'], $yo));
        }
        $m = $logica->guardarMedicion($c, $yo);
        if ($m === null) {
            responder(409, ['error' => 'Medición repetida (ya la había enviado otro móvil o este mismo)']);
        }
        responder(201, $m);
    }
    if ($metodo === 'GET' && $ruta === '/zonas') {
        responder(200, $logica->zonas());
    }
    if ($metodo === 'GET' && $n === 3 && $r === 'zonas' && $p[2] === 'serie') {
        responder(200, $logica->serie($p[1], $_GET['magnitud'] ?? 'o3', $_GET['periodo'] ?? '24h',
            $_GET['desde'] ?? null, $_GET['hasta'] ?? null));
    }
    if ($metodo === 'GET' && $ruta === '/nodos') {
        responder(200, $logica->nodos());
    }
    if ($metodo === 'GET' && $n === 2 && $r === 'nodos') {
        responder(200, $logica->nodos($p[1])[0]);
    }
    if ($metodo === 'GET' && $ruta === '/ayuntamientos') {
        responder(200, $flota->ayuntamientos());
    }
    if ($metodo === 'GET' && $ruta === '/puntos-reparto') {
        responder(200, $flota->puntosReparto());
    }
    if ($metodo === 'GET' && $ruta === '/regalos') {
        responder(200, $puntos->regalos());
    }
    if ($metodo === 'GET' && $ruta === '/percepciones') {
        responder(200, $comunidad->percepciones($yo));
    }

    // ---------------- cuentas ----------------
    if ($metodo === 'POST' && $ruta === '/sesiones') {
        responder(201, $cuentas->iniciarSesion((string) ($c['email'] ?? ''), (string) ($c['clave'] ?? ''),
            ($c['cliente'] ?? 'app') === 'web' ? 'web' : 'app'));
    }
    if ($metodo === 'DELETE' && $ruta === '/sesiones') {
        if ($token) {
            $cuentas->cerrarSesion($token);
        }
        responder(200, ['ok' => true]);
    }
    if ($metodo === 'POST' && $ruta === '/usuarios') {
        responder(201, $cuentas->registrar((string) ($c['nombre'] ?? ''), (string) ($c['email'] ?? ''),
            (string) ($c['clave'] ?? ''), !empty($c['acepta'])));
    }
    if ($metodo === 'POST' && $ruta === '/recuperaciones') {
        $cuentas->solicitarRecuperacion((string) ($c['email'] ?? ''), $cfg['url_web']);
        responder(202, ['ok' => true, 'mensaje' => 'Si hay una cuenta con ese correo, recibirá un enlace en unos minutos.']);
    }
    if ($n === 2 && $r === 'enlaces') {
        if ($metodo === 'GET') {
            responder(200, $cuentas->verEnlace($p[1]));
        }
        if ($metodo === 'POST') {
            responder(200, ['email' => $cuentas->usarEnlace($p[1], (string) ($c['clave'] ?? ''))]);
        }
    }

    // ---------------- mi cuenta (app) ----------------
    if ($r === 'yo') {
        if (!$yo) {
            throw new NoAutorizadoException('Tu sesión ha caducado. Vuelve a iniciar sesión.');
        }
        if ($metodo === 'GET' && $n === 1) {
            $out = ['usuario' => $cuentas->perfil($yo)];
            if (in_array($yo['rol'], ['vecino', 'trabajador'], true)) {
                $out['cuenta'] = $puntos->cuenta($yo);
                $out['seguimiento'] = $cuentas->seguimiento($yo);
                $out['prestamo'] = $flota->prestamoDe($yo['id']);
            }
            responder(200, $out);
        }
        if ($metodo === 'DELETE' && $n === 1) {
            $cuentas->borrarCuenta($yo);
            responder(200, ['ok' => true]);
        }
        if ($metodo === 'POST' && $ruta === '/yo/sensor') {
            responder(200, $flota->sensorParaLaApp($yo, (string) ($c['codigo'] ?? '')));
        }
        if ($metodo === 'PUT' && $ruta === '/yo/seguimiento') {
            responder(200, $cuentas->guardarSeguimiento($yo, $c));
        }
        if ($metodo === 'GET' && $ruta === '/yo/percepciones') {
            responder(200, $comunidad->mias($yo));
        }
    }
    if ($metodo === 'POST' && $ruta === '/percepciones') {
        responder(201, $comunidad->publicar($yo, (string) ($c['tipo'] ?? ''), $c['nivel'] ?? null, $c['lat'] ?? null,
            $c['lng'] ?? null, (string) ($c['direccion'] ?? '')));
    }
    if ($n === 2 && $r === 'percepciones' && ctype_digit($p[1])) {
        if ($metodo === 'PUT') {
            responder(200, $comunidad->editar($yo, (int) $p[1], (string) ($c['tipo'] ?? ''), $c['nivel'] ?? null));
        }
        if ($metodo === 'DELETE') {
            $comunidad->borrar($yo, (int) $p[1]);
            responder(200, ['ok' => true]);
        }
    }
    if ($metodo === 'POST' && $ruta === '/canjes') {
        responder(201, $puntos->canjear($yo, (string) ($c['regalo'] ?? '')));
    }

    // ---------------- panel municipal (web) ----------------
    if ($r === 'panel') {
        if (!$yo) {
            throw new NoAutorizadoException('Tu sesión ha caducado. Vuelve a iniciar sesión.');
        }
        $s = $p[1] ?? '';
        $id = $p[2] ?? '';
        if ($s === 'nodos') {
            if ($metodo === 'GET' && $n === 2) responder(200, $flota->nodosPanel($yo));
            if ($metodo === 'POST' && $n === 2) responder(201, $flota->altaNodo($yo, $c));
            if ($metodo === 'PUT' && $n === 3) responder(200, $flota->editarNodo($yo, $id, $c));
            if ($metodo === 'DELETE' && $n === 3) {
                $flota->bajaNodo($yo, $id, (string) ($c['motivo'] ?? ''));
                responder(200, ['ok' => true]);
            }
        }
        if ($s === 'prestamos') {
            if ($metodo === 'GET' && $n === 2) responder(200, $flota->prestamos($yo));
            if ($metodo === 'POST' && $n === 2) responder(201, $flota->prestar($yo, (string) ($c['usuario'] ?? ''), (string) ($c['codigo'] ?? ''), $c['punto'] ?? null));
        }
        if ($s === 'devoluciones' && $metodo === 'POST' && $n === 2) {
            responder(200, $flota->devolver($yo, (string) ($c['codigo'] ?? ''), (string) ($c['estado'] ?? 'Correcto')));
        }
        if ($s === 'vecinos' && $metodo === 'GET' && $n === 2) {
            responder(200, $cuentas->vecinos($yo));
        }
        if ($s === 'reglas' && $n === 2) {
            if ($metodo === 'GET') responder(200, $puntos->reglas($yo));
            if ($metodo === 'PUT') responder(200, $puntos->guardarReglas($yo, $c['min'] ?? '', $c['mult'] ?? '', $c['max'] ?? ''));
        }
        if ($s === 'revisiones') {
            if ($metodo === 'GET' && $n === 2) responder(200, $puntos->revisiones($yo));
            if ($metodo === 'POST' && $n === 3 && ctype_digit($id)) {
                responder(200, $puntos->revisar($yo, (int) $id, (string) ($c['decision'] ?? ''), (string) ($c['razon'] ?? '')));
            }
        }
        if ($s === 'regalos') {
            if ($metodo === 'POST' && $n === 2) responder(201, $puntos->crearRegalo($yo, (string) ($c['nombre'] ?? ''), $c['coste'] ?? '', $c['stock'] ?? ''));
            if ($metodo === 'PUT' && $n === 3) responder(200, $puntos->editarRegalo($yo, $id, (string) ($c['nombre'] ?? ''), $c['coste'] ?? '', $c['stock'] ?? ''));
            if ($metodo === 'DELETE' && $n === 3) {
                $puntos->retirarRegalo($yo, $id);
                responder(200, ['ok' => true]);
            }
        }
        if ($s === 'canjes') {
            if ($metodo === 'GET' && $n === 2) responder(200, $puntos->canjes($yo));
            if ($metodo === 'GET' && $n === 3) responder(200, $puntos->comprobarCanje($yo, $id));
            if ($metodo === 'POST' && $n === 4 && $p[3] === 'entrega') responder(200, $puntos->entregarCanje($yo, $id));
        }
        if ($s === 'trabajadores') {
            // el enlace se manda por correo al trabajador y se le enseña a
            // administración (por si el correo no llega, se lo puede dar en mano)
            $enlace = function ($x) use ($cfg) {
                $x['enlace'] = rtrim($cfg['url_web'], '/') . '/#activar/' . $x['token'];
                unset($x['token']);
                $t = $x['trabajador'];
                $x['correo_enviado'] = (bool) call_user_func($cfg['correo'], $t['email'], 'Respira: activa tu cuenta de trabajador municipal',
                    "Hola, {$t['nombre']}:\n\nEl ayuntamiento te ha dado de alta en Respira ({$t['id']}). Para crear tu contraseña, "
                    . "abre este enlace (caduca en 48 horas y solo sirve una vez):\n\n{$x['enlace']}\n\nDespués, entra en la app Respira con este correo.\n\nRespira · Nodia");
                return $x;
            };
            if ($metodo === 'GET' && $n === 2) responder(200, $cuentas->trabajadores($yo));
            if ($metodo === 'POST' && $n === 2) {
                responder(201, $enlace($cuentas->altaTrabajador($yo, (string) ($c['nombre'] ?? ''), (string) ($c['email'] ?? ''),
                    (string) ($c['num_empleado'] ?? ''), (string) ($c['servicio'] ?? ''))));
            }
            if ($metodo === 'POST' && $n === 4 && $p[3] === 'enlace') responder(200, $enlace($cuentas->reenviarActivacion($yo, $id)));
        }
    }
    responder(404, ['error' => "Ruta desconocida: $metodo $ruta"]);
} catch (ErroresDeCamposException $e) {
    responder(400, ['error' => $e->getMessage(), 'errores' => $e->errores]);
} catch (ConflictoException $e) {
    responder(409, ['error' => $e->getMessage(), 'campo' => $e->campo]);
} catch (DatosNoValidosException $e) {
    responder(400, ['error' => $e->getMessage(), 'campo' => $e->campo]);
} catch (NoAutorizadoException $e) {
    responder(401, ['error' => $e->getMessage(), 'quedan' => $e->quedan]);
} catch (ProhibidoException $e) {
    responder(403, ['error' => $e->getMessage()]);
} catch (NoEncontradoException $e) {
    responder(404, ['error' => $e->getMessage()]);
} catch (BloqueadoException $e) {
    responder(423, ['error' => $e->getMessage()]);
} catch (DemasiadasException $e) {
    responder(429, ['error' => $e->getMessage()]);
} catch (Throwable $e) {
    error_log('[Respira] ' . $e->getMessage() . ' en ' . $e->getFile() . ':' . $e->getLine());
    responder(500, ['error' => 'Error del servidor']);
}
