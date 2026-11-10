<?php
// ==============================================================
// Fichero:     bd_prueba.php
// Descripción: Crea una BD SQLite con las MISMAS tablas que esquema.sql
//              (se traducen solas: SQLite no entiende ENUM, SET, ENGINE,
//              COMMENT ni AUTO_INCREMENT) y carga los datos base
//              (zonas, nodo real, regalos, reglas, puntos de reparto).
//              Los datos de demostración no se cargan: cada test crea
//              lo que necesita.
// Autor:       Aaron Blasco Blay
// Fecha:       09/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================

// --------------------------------------------------------------
// sql: Text (CREATE TABLE de MariaDB) --> aSQLite() --> Text
// --------------------------------------------------------------
function aSQLite(string $sql): string {
    $lineas = [];
    $pk = null;
    foreach (explode("\n", $sql) as $l) {
        $t = trim($l);
        if ($t === '' || strpos($t, '--') === 0) {
            continue;
        }
        if (preg_match('/^(UNIQUE )?KEY /', $t)) {
            continue;                                   // los índices no hacen falta en los tests
        }
        if (preg_match('/^CONSTRAINT \w+ CHECK/', $t)) {
            continue;                                   // REGEXP y JSON_VALID no existen en SQLite
        }
        if (preg_match('/^PRIMARY KEY \((\w+)\),?$/', $t, $m) && $pk === $m[1]) {
            continue;                                   // ya va en la columna AUTOINCREMENT
        }
        $t = preg_replace("/ COMMENT '(?:[^']|'')*'/", '', $t);
        $t = preg_replace('/ENUM\([^)]*\)|SET\([^)]*\)/', 'TEXT', $t);
        $t = preg_replace('/\bUNSIGNED\b/', '', $t);
        if (preg_match('/^(\w+)\s+\w+.*AUTO_INCREMENT/', $t, $m)) {
            $pk = $m[1];
            $t = "$m[1] INTEGER PRIMARY KEY AUTOINCREMENT,";
        }
        $t = preg_replace('/\) ENGINE=.*;$/', ');', $t);
        $lineas[] = $t;
    }
    $s = implode("\n", $lineas);
    return preg_replace('/,\s*\n\)/', "\n)", $s);       // coma sobrante antes del paréntesis final
}

// --------------------------------------------------------------
// dsn: Text --> crearBDPrueba() --> PDO
// --------------------------------------------------------------
function crearBDPrueba(string $dsn = 'sqlite::memory:'): PDO {
    $bd = new PDO($dsn);
    $bd->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $sql = file_get_contents(__DIR__ . '/../bd/esquema.sql');
    $ini = strpos($sql, '-- @@TABLAS:INICIO');
    $tablas = substr($sql, $ini, strpos($sql, '-- @@TABLAS:FIN') - $ini);
    preg_match_all('/CREATE TABLE (\w+) \(.*?\) ENGINE=[^;]*;/s', $tablas, $m);
    foreach (array_reverse($m[1]) as $t) {
        $bd->exec("DROP TABLE IF EXISTS $t");
    }
    foreach ($m[0] as $create) {
        $bd->exec(aSQLite($create));
    }
    $ini = strpos($sql, '-- @@BASE:INICIO');
    $base = substr($sql, $ini, strpos($sql, '-- @@BASE:FIN') - $ini);
    preg_match_all('/^INSERT INTO .*?;\s*$/ms', $base, $m);
    foreach ($m[0] as $insert) {
        $bd->exec($insert);
    }
    return $bd;
}

// --------------------------------------------------------------
// Ayuda para los tests: crea una cuenta (clave null = pendiente de activar)
// bd, id, rol, email, clave, extra: {columna: valor} --> crearUsuario()
// --------------------------------------------------------------
function crearUsuario(PDO $bd, string $id, string $rol, string $email, ?string $clave, array $extra = []): void {
    $c = array_merge(['id' => $id, 'rol' => $rol, 'nombre' => explode('@', $email)[0], 'email' => $email,
        'clave_hash' => $clave === null ? null : password_hash($clave, PASSWORD_DEFAULT),
        'estado' => $clave === null ? 'pendiente' : 'activo', 'alta' => '2026-11-01 10:00:00.000'], $extra);
    $st = $bd->prepare('INSERT INTO usuario (' . implode(', ', array_keys($c)) . ') VALUES ('
        . implode(', ', array_fill(0, count($c), '?')) . ')');
    $st->execute(array_values($c));
}
