<?php
// ==============================================================
// Fichero:     mini_test.php
// Descripción: Mini marco de tests (sin dependencias): prueba(),
//              igual(), cierto(), lanza() y el resumen final.
// Autor:       Aaron Blasco Blay
// Fecha:       09/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================
// # tipos: Base.php (Valor, Registro, Ausente, Error y capacidades lógicas).

$fallos = 0;
$total = 0;
// --------------------------------------------------------------
// # effects: actualiza los contadores de pruebas y escribe el resultado.
// nombre: Text, f: Operacion --> prueba()
// --------------------------------------------------------------
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
// --------------------------------------------------------------
// esperado: Valor, real: Valor, que: Text --> igual() --> 0 | Error
// --------------------------------------------------------------
function igual($esperado, $real, string $que = ''): void {
    if ($esperado !== $real) {
        throw new Exception("$que: esperaba " . var_export($esperado, true) . ' y salió ' . var_export($real, true));
    }
}
// --------------------------------------------------------------
// v: Valor, que: Text --> cierto() --> 0 | Error
// --------------------------------------------------------------
function cierto($v, string $que = ''): void {
    if ($v !== true) {
        throw new Exception("$que: no se cumple");
    }
}
// --------------------------------------------------------------
// # effects: ejecuta el caso de prueba recibido.
// clase: Text, f: Operacion --> lanza() --> Error
// --------------------------------------------------------------
function lanza(string $clase, callable $f): Throwable {
    try {
        $f();
    } catch (Throwable $e) {
        if ($e instanceof $clase) {
            return $e;
        }
        throw new Exception("esperaba $clase y salió " . get_class($e) . ': ' . $e->getMessage());
    }
    throw new Exception("esperaba $clase y no lanzó nada");
}
// --------------------------------------------------------------
// # effects: actualiza los contadores de pruebas y escribe el resultado.
// resumen()
// --------------------------------------------------------------
function resumen(): void {
    global $fallos, $total;
    echo "\n" . ($total - $fallos) . " de $total tests correctos\n";
    exit($fallos ? 1 : 0);
}
