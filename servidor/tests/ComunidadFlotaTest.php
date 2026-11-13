<?php
// ==============================================================
// Fichero:     ComunidadFlotaTest.php
// Descripción: Tests de Comunidad (percepciones) y Flota (sensores,
//              préstamos, devoluciones y el código del sensor en la app).
//              Ejecutar:  php servidor/tests/ComunidadFlotaTest.php
// Autor:       Aaron Blasco Blay
// Fecha:       09/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================

require_once __DIR__ . '/../logica/Comunidad.php';
require_once __DIR__ . '/../logica/Flota.php';
require_once __DIR__ . '/bd_prueba.php';
require_once __DIR__ . '/mini_test.php';

$T = strtotime('2026-11-09 10:00:00 UTC') * 1000;
$LUCIA = ['id' => 'U-48213', 'rol' => 'vecino'];
$MARCOS = ['id' => 'U-11902', 'rol' => 'vecino'];
$ADMIN = ['id' => 'A-0001', 'rol' => 'admin'];
$REPARTO = ['id' => 'A-0003', 'rol' => 'reparto'];
$TECNICO = ['id' => 'A-0002', 'rol' => 'tecnico'];

function entorno(): array {
    global $T;
    $bd = crearBDPrueba();
    $reloj = function () use (&$T) { return $T; };
    crearUsuario($bd, 'U-48213', 'vecino', 'lucia@correo.es', 'Respira2026');
    crearUsuario($bd, 'U-11902', 'vecino', 'marcos@correo.es', 'Marcos2026');
    crearUsuario($bd, 'A-0001', 'admin', 'admin@ayto.es', 'Admin2026!');
    crearUsuario($bd, 'A-0003', 'reparto', 'reparto@ayto.es', 'Reparto2026!');
    crearUsuario($bd, 'T-0001', 'trabajador', 'psanz@ayto.es', 'Trabajo2026!');
    return [$bd, new Comunidad($bd, '', $reloj), new Flota($bd, '', $reloj)];
}

echo "ComunidadFlotaTest\n";

// ---------------- Comunidad ----------------

prueba('percepción: se publica, se ve 24 h y no dice quién la publicó', function () {
    global $T, $LUCIA, $MARCOS;
    [, $c] = entorno();
    $p = $c->publicar($LUCIA, 'ruido', 2, 38.9637, -0.1865, 'C/ Mayor, 43');
    igual([true, 0], [$p['mia'], $p['hace_min']]);
    $vista = $c->percepciones($MARCOS)[0];
    igual(false, $vista['mia'], 'para otro no es suya');
    cierto(!isset($vista['usuario_id']) && !isset($vista['usuario']), 'sin autor');
    igual(1, count($c->percepciones(null)));
    $T += 25 * 3600000;
    igual(0, count($c->percepciones(null)), 'a las 24 h desaparece');
    igual(1, count($c->mias($LUCIA)), 'en «Mis percepciones» sigue');
    $T -= 25 * 3600000;
});

prueba('percepción: máximo 3 por hora y cuenta; datos validados', function () {
    global $T, $LUCIA;
    [, $c] = entorno();
    for ($i = 0; $i < 3; $i++) {
        $c->publicar($LUCIA, 'temp', 1, 38.96, -0.18, 'Pl. Mayor');
    }
    lanza(DemasiadasException::class, function () use ($c, $LUCIA) { $c->publicar($LUCIA, 'temp', 1, 38.96, -0.18, 'Pl. Mayor'); });
    lanza(DatosNoValidosException::class, function () use ($c) { $c->publicar(['id' => 'U-11902', 'rol' => 'vecino'], 'olor', 1, 38.96, -0.18, 'X'); });
    lanza(DatosNoValidosException::class, function () use ($c) { $c->publicar(['id' => 'U-11902', 'rol' => 'vecino'], 'aire', 4, 38.96, -0.18, 'X'); });
    lanza(NoAutorizadoException::class, function () use ($c) { $c->publicar(null, 'aire', 1, 38.96, -0.18, 'X'); });
    $T += 61 * 60000;
    $c->publicar($LUCIA, 'aire', 3, 38.96, -0.18, 'Pl. Mayor');
    $T -= 61 * 60000;
});

prueba('percepción: solo la autora la edita (tipo y nivel) o la borra', function () {
    global $LUCIA, $MARCOS;
    [, $c] = entorno();
    $p = $c->publicar($LUCIA, 'ruido', 2, 38.9637, -0.1865, 'C/ Mayor, 43');
    lanza(ProhibidoException::class, function () use ($c, $MARCOS, $p) { $c->editar($MARCOS, $p['id'], 'aire', 1); });
    lanza(ProhibidoException::class, function () use ($c, $MARCOS, $p) { $c->borrar($MARCOS, $p['id']); });
    $e = $c->editar($LUCIA, $p['id'], 'aire', 3);
    igual(['aire', 3, 'C/ Mayor, 43', 38.9637], [$e['tipo'], $e['nivel'], $e['direccion'], $e['lat']]);
    $c->borrar($LUCIA, $p['id']);
    lanza(NoEncontradoException::class, function () use ($c, $LUCIA, $p) { $c->borrar($LUCIA, $p['id']); });
});

// ---------------- Flota ----------------

prueba('flota: alta de un sensor y validaciones', function () {
    global $TECNICO, $REPARTO;
    [$bd, , $f] = entorno();
    $n = $f->altaNodo($TECNICO, ['codigo' => 'rsp-7z2k', 'tipo' => 'bus', 'magnitudes' => ['o3', 'ruido'], 'asignado_a' => 'Línea 3 · Bus 12']);
    igual(['RSP-7Z2K', 'activo', ['o3', 'ruido'], 'Línea 3 · Bus 12'], [$n['codigo'], $n['estado'], $n['magnitudes'], $n['asignado_a']]);
    igual(['No asignado', 'Sin incidencias'], array_values($bd->query("SELECT asignado_a, motivo FROM nodo WHERE codigo = 'RSP-4F7K'")->fetch(PDO::FETCH_ASSOC)), 'sin NULL');
    $e = lanza(ErroresDeCamposException::class, function () use ($f, $TECNICO) {
        $f->altaNodo($TECNICO, ['codigo' => 'RSP-7Z2K', 'tipo' => 'trab', 'magnitudes' => [], 'asignado_a' => 'x', 'mantenimiento' => true]);
    });
    igual(['codigo', 'magnitudes', 'asignado_a', 'motivo'], array_keys($e->errores));
    lanza(ProhibidoException::class, function () use ($f, $REPARTO) { $f->altaNodo($REPARTO, ['codigo' => 'RSP-AAAA']); });
});

prueba('préstamo: el punto de reparto presta (plazo de 10 días) y la app lo reconoce', function () {
    global $T, $REPARTO, $LUCIA, $MARCOS;
    [, , $f] = entorno();
    $e = lanza(ErroresDeCamposException::class, function () use ($f, $REPARTO) { $f->prestar($REPARTO, 'U-48213', 'RSP-4F7K'); });
    igual(['punto'], array_keys($e->errores), 'sin punto de reparto');
    $p = $f->prestar($REPARTO, 'u-48213', 'rsp-4f7k', 2);
    igual(['RSP-4F7K', 'U-48213', 'Centro Cívico Norte', 'No devuelto', false], [$p['nodo'], $p['usuario'], $p['punto'], $p['estado_devolucion'], $p['fuera_de_plazo']]);
    igual(Base::fechaISO($T + 10 * 86400000), $p['fecha_limite'], 'hora exacta + 10 días');
    igual($p['fecha_limite'], $f->prestamoDe('U-48213')['hasta']);
    igual('prestado', $f->nodosPanel($REPARTO)[0]['estado']);
    igual('RSP-4F7K', $f->sensorParaLaApp($LUCIA, ' rsp-4f7k ')['codigo']);
    igual('RSP-4F7K', $f->prestamoDe('U-48213')['codigo']);
    $e = lanza(ConflictoException::class, function () use ($f, $MARCOS) { $f->sensorParaLaApp($MARCOS, 'RSP-4F7K'); });
    cierto(strpos($e->getMessage(), 'otra persona') !== false, 'de otra persona');
});

prueba('préstamo: mismos errores que el panel (usuario, sensor ocupado o no prestable)', function () {
    global $REPARTO, $ADMIN;
    [, , $f] = entorno();
    $f->altaNodo($ADMIN, ['codigo' => 'RSP-B12A', 'tipo' => 'bus', 'magnitudes' => ['o3'], 'asignado_a' => 'Línea 2']);
    $f->prestar($REPARTO, 'U-48213', 'RSP-4F7K', 1);
    $e = lanza(ErroresDeCamposException::class, function () use ($f, $REPARTO) { $f->prestar($REPARTO, 'U-48213', 'RSP-4F7K', 1); });
    cierto(strpos($e->errores['usuario'], 'ya tiene un sensor') !== false, 'usuario');
    cierto(strpos($e->errores['codigo'], 'ya está prestado a U-48213') !== false, 'código');
    $e = lanza(ErroresDeCamposException::class, function () use ($f, $REPARTO) { $f->prestar($REPARTO, 'U-99999', 'RSP-B12A', 1); });
    cierto(strpos($e->errores['usuario'], 'No existe') !== false, 'no existe');
    cierto(strpos($e->errores['codigo'], 'no se presta') !== false, 'bus');
});

prueba('devolución: defectuoso pasa a mantenimiento; la app avisa de que ya se devolvió', function () {
    global $REPARTO, $LUCIA;
    [, , $f] = entorno();
    $f->prestar($REPARTO, 'U-48213', 'RSP-4F7K', 1);
    lanza(DatosNoValidosException::class, function () use ($f, $REPARTO) { $f->devolver($REPARTO, 'RSP-4F7K', 'Batería baja'); });
    $n = $f->devolver($REPARTO, 'rsp-4f7k', 'Defectuoso');
    igual(['mant', 'No se ha entregado en buenas condiciones', null], [$n['estado'], $n['motivo_mant'], $n['prestamo']]);
    igual('Defectuoso', $f->prestamos($REPARTO)['cerrados'][0]['estado_devolucion']);
    $e = lanza(ConflictoException::class, function () use ($f, $LUCIA) { $f->sensorParaLaApp($LUCIA, 'RSP-4F7K'); });
    cierto(strpos($e->getMessage(), 'ya se devolvió') !== false, 'devuelto');
    lanza(DatosNoValidosException::class, function () use ($f, $REPARTO) { $f->devolver($REPARTO, 'RSP-4F7K', 'Correcto'); });
});

prueba('préstamo: pasado el plazo sale «fuera de plazo» y se puede devolver en otro punto', function () {
    global $T, $REPARTO;
    [, , $f] = entorno();
    $f->prestar($REPARTO, 'U-48213', 'RSP-4F7K', 1);
    $T += 10 * 86400000 + 60000;
    igual(true, $f->prestamos($REPARTO)['abiertos'][0]['fuera_de_plazo']);
    $f->devolver($REPARTO, 'RSP-4F7K', 'Correcto');
    igual([[], 'Correcto'], [$f->prestamos($REPARTO)['abiertos'], $f->prestamos($REPARTO)['cerrados'][0]['estado_devolucion']]);
    $T -= 10 * 86400000 + 60000;
});

prueba('app: mensajes del código (formato, no existe, sin prestar) y trabajadores', function () {
    global $ADMIN;
    [, , $f] = entorno();
    $u = ['id' => 'U-48213', 'rol' => 'vecino'];
    lanza(DatosNoValidosException::class, function () use ($f, $u) { $f->sensorParaLaApp($u, 'ABC'); });
    lanza(NoEncontradoException::class, function () use ($f, $u) { $f->sensorParaLaApp($u, 'RSP-ZZZZ'); });
    $e = lanza(ConflictoException::class, function () use ($f, $u) { $f->sensorParaLaApp($u, 'RSP-4F7K'); });
    cierto(strpos($e->getMessage(), 'con tu ID U-48213') !== false, 'sin prestar');
    $f->altaNodo($ADMIN, ['codigo' => 'RSP-L1MP', 'tipo' => 'trab', 'magnitudes' => ['o3'], 'asignado_a' => 'Limpieza viaria']);
    $t = ['id' => 'T-0001', 'rol' => 'trabajador'];
    igual('RSP-L1MP', $f->sensorParaLaApp($t, 'RSP-L1MP')['codigo']);
    lanza(ConflictoException::class, function () use ($f, $t) { $f->sensorParaLaApp($t, 'RSP-4F7K'); });
});

prueba('baja: solo administración, no si está prestado; desaparece del panel', function () {
    global $ADMIN, $TECNICO, $REPARTO;
    [$bd, , $f] = entorno();
    $f->prestar($REPARTO, 'U-48213', 'RSP-4F7K', 1);
    lanza(ProhibidoException::class, function () use ($f, $TECNICO) { $f->bajaNodo($TECNICO, 'RSP-4F7K', 'Roto'); });
    lanza(ConflictoException::class, function () use ($f, $ADMIN) { $f->bajaNodo($ADMIN, 'RSP-4F7K', 'Roto'); });
    $f->devolver($REPARTO, 'RSP-4F7K', 'Correcto');
    $f->bajaNodo($ADMIN, 'RSP-4F7K', 'Perdido');
    igual(0, count($f->nodosPanel($ADMIN)));
    igual(['baja', 'Perdido'], array_values($bd->query("SELECT estado, motivo FROM nodo WHERE codigo = 'RSP-4F7K'")->fetch(PDO::FETCH_ASSOC)), 'un solo motivo');
});

prueba('panel: «hace cuánto» sale de las mediciones (no se guarda en nodo)', function () {
    global $T, $ADMIN;
    [$bd, , $f] = entorno();
    igual(null, $f->nodosPanel($ADMIN)[0]['ultima_min'], 'sin datos');
    $bd->exec("INSERT INTO medicion (id_nodo, tipo, valor, fecha_medicion, lat, lng, zona_id, origen) VALUES ('RSP-4F7K', 'temp', 20, '"
        . Base::fechaBD($T - 25 * 60000) . "', 38.9657, -0.1827, 'central', 'real'), ('RSP-4F7K', 'o3', 0.04, '"
        . Base::fechaBD($T - 90 * 60000) . "', 38.9657, -0.1827, 'central', 'real')");
    igual(25, $f->nodosPanel($ADMIN)[0]['ultima_min'], 'el más reciente');
});

prueba('editar: no cambia el tipo de uno prestado; mantenimiento con motivo', function () {
    global $ADMIN, $REPARTO;
    [$bd, , $f] = entorno();
    $f->prestar($REPARTO, 'U-48213', 'RSP-4F7K', 1);
    $e = lanza(ErroresDeCamposException::class, function () use ($f, $ADMIN) {
        $f->editarNodo($ADMIN, 'RSP-4F7K', ['tipo' => 'bici', 'magnitudes' => ['o3'], 'asignado_a' => 'Estación 1']);
    });
    igual(['tipo'], array_keys($e->errores));
    $f->devolver($REPARTO, 'RSP-4F7K', 'Correcto');
    $n = $f->editarNodo($ADMIN, 'RSP-4F7K', ['tipo' => 'vol', 'magnitudes' => ['o3', 'temp'], 'mantenimiento' => true, 'motivo' => 'Cambio de pila']);
    igual(['mant', 'Cambio de pila', ['o3', 'temp']], [$n['estado'], $n['motivo_mant'], $n['magnitudes']]);
    $n = $f->editarNodo($ADMIN, 'RSP-4F7K', ['tipo' => 'vol', 'magnitudes' => ['o3', 'temp']]);
    igual(['disponible', null], [$n['estado'], $n['motivo_mant']]);
    igual('Sin incidencias', $bd->query("SELECT motivo FROM nodo WHERE codigo = 'RSP-4F7K'")->fetchColumn());
});

prueba('público: ayuntamientos y puntos de reparto', function () {
    [, , $f] = entorno();
    igual('Gandia', $f->ayuntamientos()[0]['nombre']);
    igual(3, count($f->puntosReparto()));
});

resumen();
