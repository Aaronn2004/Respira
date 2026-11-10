<?php
// ==============================================================
// Fichero:     CuentasTest.php
// Descripción: Tests de Cuentas: registro, inicio de sesión y bloqueo,
//              sesiones, enlaces de un solo uso, trabajadores y borrar
//              la cuenta. BD SQLite en memoria y reloj fijo.
//              Ejecutar:  php servidor/tests/CuentasTest.php
// Autor:       Aaron Blasco Blay
// Fecha:       09/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================

require_once __DIR__ . '/../logica/Cuentas.php';
require_once __DIR__ . '/bd_prueba.php';
require_once __DIR__ . '/mini_test.php';

$T = strtotime('2026-11-09 10:00:00 UTC') * 1000;
$correos = [];
function nuevas(): array {
    global $T, $correos;
    $correos = [];
    $bd = crearBDPrueba();
    $reloj = function () use (&$T) { return $T; };
    $c = new Cuentas($bd, '', $reloj, function ($para, $asunto, $texto) use (&$correos) {
        $correos[] = [$para, $asunto, $texto];
        return true;
    });
    crearUsuario($bd, 'A-0001', 'admin', 'admin@ayto.es', 'Admin2026!');
    crearUsuario($bd, 'U-48213', 'vecino', 'lucia@correo.es', 'Respira2026', ['nombre' => 'lucia']);
    return [$bd, $c];
}

echo "CuentasTest\n";

prueba('registro: crea un vecino U-XXXXX, con hash y la sesión abierta', function () {
    [$bd, $c] = nuevas();
    $r = $c->registrar('marcos', 'Marcos@Correo.es', 'Marcos2026', true);
    cierto((bool) preg_match('/^U-\d{5}$/', $r['usuario']['id']), 'id');
    igual('vecino', $r['usuario']['rol']);
    igual('marcos@correo.es', $r['usuario']['email'], 'email en minúsculas');
    $hash = $bd->query("SELECT clave_hash FROM usuario WHERE email = 'marcos@correo.es'")->fetchColumn();
    cierto($hash !== 'Marcos2026' && password_verify('Marcos2026', $hash), 'hash');
    igual($r['usuario']['id'], $c->usuarioDeSesion($r['token'])['id'], 'sesión');
});

prueba('registro: errores por campo (repetidos, formato, términos)', function () {
    [, $c] = nuevas();
    $e = lanza(ErroresDeCamposException::class, function () use ($c) { $c->registrar('Lucia', 'lucia@correo.es', 'corta', false); });
    igual(['nombre', 'email', 'clave', 'acepta'], array_keys($e->errores));
    cierto(strpos($e->errores['nombre'], 'ya existe') !== false, 'nombre repetido');
    $e = lanza(ErroresDeCamposException::class, function () use ($c) { $c->registrar('con espacio', 'malo', '', true); });
    igual(['nombre', 'email', 'clave'], array_keys($e->errores));
});

prueba('login: correo en mayúsculas vale y devuelve token y perfil', function () {
    [, $c] = nuevas();
    $r = $c->iniciarSesion(' LUCIA@correo.es ', 'Respira2026', 'app');
    igual('U-48213', $r['usuario']['id']);
    igual(64, strlen($r['token']));
});

prueba('login: avisa de los intentos que quedan y bloquea 15 min al 5º fallo', function () {
    global $T;
    [, $c] = nuevas();
    for ($i = 1; $i <= 4; $i++) {
        $e = lanza(NoAutorizadoException::class, function () use ($c) { $c->iniciarSesion('lucia@correo.es', 'mal', 'app'); });
        igual(5 - $i, $e->quedan, "intento $i");
    }
    lanza(BloqueadoException::class, function () use ($c) { $c->iniciarSesion('lucia@correo.es', 'mal', 'app'); });
    // bloqueada aunque ahora ponga bien la contraseña
    lanza(BloqueadoException::class, function () use ($c) { $c->iniciarSesion('lucia@correo.es', 'Respira2026', 'app'); });
    $T += 16 * 60000;
    igual('U-48213', $c->iniciarSesion('lucia@correo.es', 'Respira2026', 'app')['usuario']['id'], 'tras 15 min');
    $T -= 16 * 60000;
});

prueba('login: un correo que no existe no dice si existe', function () {
    [, $c] = nuevas();
    $e = lanza(NoAutorizadoException::class, function () use ($c) { $c->iniciarSesion('nadie@correo.es', 'x', 'app'); });
    igual('Correo o contraseña incorrectos.', $e->getMessage());
});

prueba('login: la web solo deja entrar al personal; la app, a vecinos y trabajadores', function () {
    [$bd, $c] = nuevas();
    crearUsuario($bd, 'T-0001', 'trabajador', 'psanz@ayto.es', 'Trabajo2026!');
    lanza(ProhibidoException::class, function () use ($c) { $c->iniciarSesion('lucia@correo.es', 'Respira2026', 'web'); });
    $e = lanza(ProhibidoException::class, function () use ($c) { $c->iniciarSesion('psanz@ayto.es', 'Trabajo2026!', 'web'); });
    cierto(strpos($e->getMessage(), 'trabajador municipal') !== false, 'mensaje trabajador');
    lanza(ProhibidoException::class, function () use ($c) { $c->iniciarSesion('admin@ayto.es', 'Admin2026!', 'app'); });
    igual('admin', $c->iniciarSesion('admin@ayto.es', 'Admin2026!', 'web')['usuario']['rol']);
    igual('trabajador', $c->iniciarSesion('psanz@ayto.es', 'Trabajo2026!', 'app')['usuario']['rol']);
});

prueba('sesión: el token se guarda con hash, caduca y se puede cerrar', function () {
    global $T;
    [$bd, $c] = nuevas();
    $r = $c->iniciarSesion('admin@ayto.es', 'Admin2026!', 'web');
    igual(0, (int) $bd->query("SELECT COUNT(*) FROM sesion WHERE token_hash = " . $bd->quote($r['token']))->fetchColumn(), 'no en claro');
    $T += 13 * 3600000;
    igual(null, $c->usuarioDeSesion($r['token']), 'web caduca a las 12 h');
    $T -= 13 * 3600000;
    $c->cerrarSesion($r['token']);
    igual(null, $c->usuarioDeSesion($r['token']), 'cerrada');
    igual(null, $c->usuarioDeSesion('inventado'));
});

prueba('trabajador: alta pendiente con enlace de 48 h que solo sirve una vez', function () {
    global $T;
    [$bd, $c] = nuevas();
    $admin = $c->usuarioPorId('A-0001');
    $r = $c->altaTrabajador($admin, 'Pablo  Sanz Gil', 'PSanz@ayto.es', '104233', 'Limpieza viaria');
    igual('T-0001', $r['trabajador']['id']);
    igual('pendiente', $r['trabajador']['estado']);
    $v = $c->verEnlace($r['token']);
    igual(['valido', 'activar', 'Pablo Sanz Gil'], [$v['estado'], $v['tipo'], $v['nombre']]);
    $e = lanza(DatosNoValidosException::class, function () use ($c, $r) { $c->usarEnlace($r['token'], 'psanz12345'); });
    cierto(strpos($e->getMessage(), 'nombre de usuario') !== false, 'no puede llevar su usuario');
    lanza(DatosNoValidosException::class, function () use ($c, $r) { $c->usarEnlace($r['token'], 'solotexto'); });
    igual('psanz@ayto.es', $c->usarEnlace($r['token'], 'Trabajo2026!'));
    igual('activo', $c->trabajadores($admin)[0]['estado']);
    igual('no_valido', $c->verEnlace($r['token'])['estado'], 'ya usado');
    igual('trabajador', $c->iniciarSesion('psanz@ayto.es', 'Trabajo2026!', 'app')['usuario']['rol']);
});

prueba('trabajador: enlace caducado y reenvío (el anterior deja de servir)', function () {
    global $T;
    [$bd, $c] = nuevas();
    $admin = $c->usuarioPorId('A-0001');
    $r = $c->altaTrabajador($admin, 'Elena Mora Vidal', 'emora@ayto.es', '104587', 'Parques y jardines');
    $T += 49 * 3600000;
    igual('caducado', $c->verEnlace($r['token'])['estado']);
    igual('caducado', $c->trabajadores($admin)[0]['estado']);
    lanza(ConflictoException::class, function () use ($c, $r) { $c->usarEnlace($r['token'], 'Elena2026xyz'); });
    $r2 = $c->reenviarActivacion($admin, 'T-0001');
    igual('no_valido', $c->verEnlace($r['token'])['estado'], 'el viejo ya no vale');
    igual('valido', $c->verEnlace($r2['token'])['estado']);
    $T -= 49 * 3600000;
});

prueba('trabajador: validaciones y solo administración', function () {
    [$bd, $c] = nuevas();
    $admin = $c->usuarioPorId('A-0001');
    $e = lanza(ErroresDeCamposException::class, function () use ($c, $admin) { $c->altaTrabajador($admin, 'Pablo', 'p@gmail.com', '12', 'X'); });
    igual(['nombre', 'email', 'num_empleado', 'servicio'], array_keys($e->errores));
    crearUsuario($bd, 'A-0002', 'tecnico', 'tecnico@ayto.es', 'Tecnico2026!');
    lanza(ProhibidoException::class, function () use ($c) { $c->altaTrabajador($c->usuarioPorId('A-0002'), 'A B', 'a@ayto.es', '111111', 'Movilidad'); });
    lanza(NoAutorizadoException::class, function () use ($c) { $c->trabajadores(null); });
});

prueba('recuperar: manda el enlace por correo, cambia la contraseña y cierra las sesiones', function () {
    global $correos;
    [$bd, $c] = nuevas();
    $s = $c->iniciarSesion('lucia@correo.es', 'Respira2026', 'app');
    $c->solicitarRecuperacion('lucia@correo.es', 'https://x.es/respira/web/');
    igual(1, count($correos));
    preg_match('/#activar\/([0-9a-f]{64})/', $correos[0][2], $m);
    igual('recuperar', $c->verEnlace($m[1])['tipo']);
    $c->usarEnlace($m[1], 'Nueva2026');
    igual(null, $c->usuarioDeSesion($s['token']), 'sesión cerrada');
    igual('U-48213', $c->iniciarSesion('lucia@correo.es', 'Nueva2026', 'app')['usuario']['id']);
    $c->solicitarRecuperacion('nadie@correo.es', 'https://x.es/');
    igual(1, count($correos), 'a un correo que no existe no se manda nada (y no se avisa)');
});

prueba('borrar cuenta: no con un sensor prestado; después borra sus datos', function () {
    [$bd, $c] = nuevas();
    $u = $c->usuarioPorId('U-48213');
    $bd->exec("INSERT INTO prestamo (nodo, usuario_id, inicio) VALUES ('RSP-4F7K', 'U-48213', '2026-11-09 08:00:00.000')");
    lanza(ConflictoException::class, function () use ($c, $u) { $c->borrarCuenta($u); });
    $bd->exec("UPDATE prestamo SET fin = '2026-11-09 09:00:00.000'");
    $bd->exec("INSERT INTO percepcion (usuario_id, tipo, nivel, lat, lng, direccion, creada) VALUES ('U-48213', 'ruido', 2, 38.96, -0.18, 'C/ Mayor', '2026-11-09 09:00:00.000')");
    $c->borrarCuenta($u);
    igual(null, $c->usuarioPorId('U-48213'));
    igual(0, (int) $bd->query('SELECT COUNT(*) FROM percepcion')->fetchColumn(), 'percepciones');
    igual(null, $bd->query('SELECT usuario_id FROM prestamo')->fetchColumn() ?: null, 'préstamo anonimizado');
    lanza(ProhibidoException::class, function () use ($c) { $c->borrarCuenta($c->usuarioPorId('A-0001')); });
});

resumen();
