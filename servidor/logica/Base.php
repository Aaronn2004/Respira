<?php
// ==============================================================
// Fichero:     Base.php
// Descripción: Lo común a toda la lógica del negocio: errores del
//              dominio (cada uno se traduce a un código HTTP en la API),
//              conexión, reloj, fechas y pequeñas ayudas de SQL.
//              Solo SQL sencillo: funciona igual en MariaDB (Plesk) y en
//              SQLite (tests).
// Diseño:      doc/servidor.md
// Autor:       Aaron Blasco Blay
// Fecha:       09/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================

// ---- Errores del dominio (la API los traduce a HTTP) ----

// 400: un dato no vale. campo = qué campo del formulario (para marcarlo)
class DatosNoValidosException extends Exception {
    public $campo;
    public function __construct(string $mensaje, ?string $campo = null) {
        parent::__construct($mensaje);
        $this->campo = $campo;
    }
}

// 400 con varios campos a la vez: {campo: mensaje}
class ErroresDeCamposException extends Exception {
    public $errores;
    public function __construct(array $errores) {
        parent::__construct('Revisa los campos marcados');
        $this->errores = $errores;
    }
}

class NoAutorizadoException extends Exception {        // 401: sin sesión o credenciales malas
    public $quedan;
    public function __construct(string $mensaje, ?int $quedan = null) {
        parent::__construct($mensaje);
        $this->quedan = $quedan;
    }
}
class ProhibidoException extends Exception { }            // 403: con sesión, pero sin permiso
class NoEncontradoException extends Exception { }         // 404
class ConflictoException extends DatosNoValidosException { }  // 409: choca con el estado actual
class BloqueadoException extends Exception { }            // 423: cuenta bloqueada un rato
class DemasiadasException extends Exception { }           // 429: límite por hora
class ErrorDeAlmacenamientoException extends Exception { } // 500

abstract class Base {

    const HORA_MS = 3600000;
    const DIA_MS = 86400000;
    // Los días (puntos, «hoy») se cuentan en hora de España
    const ZONA_HORARIA = 'Europe/Madrid';

    protected $bd;
    protected $p;     // prefijo de las tablas ('' normalmente)
    protected $reloj; // () --> ms. Se cambia en los tests

    // --------------------------------------------------------------
    // bd: PDO, prefijo: Text, reloj: (() --> N) | null --> Base()
    // --------------------------------------------------------------
    public function __construct(PDO $bd, string $prefijo = '', ?callable $reloj = null) {
        $this->bd = $bd;
        $this->bd->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $this->p = $prefijo;
        $this->reloj = $reloj ?? function () { return (int) round(microtime(true) * 1000); };
    }

    protected function ahora(): int {
        return (int) call_user_func($this->reloj);
    }

    // ---------- fechas ----------

    // ms --> 'YYYY-MM-DD HH:MM:SS.mmm' (UTC)
    public static function fechaBD(int $ms): string {
        return gmdate('Y-m-d H:i:s', intdiv($ms, 1000)) . sprintf('.%03d', $ms % 1000);
    }

    // 'YYYY-MM-DD HH:MM:SS[.mmm]' (UTC) --> ms
    public static function msDeFechaBD(string $f): int {
        $seg = strtotime(substr($f, 0, 19) . ' UTC');
        $ms = strlen($f) > 20 ? (int) str_pad(substr($f, 20, 3), 3, '0') : 0;
        return $seg * 1000 + $ms;
    }

    // ms --> '2026-11-06T10:15:00.000Z'
    public static function fechaISO(int $ms): string {
        return gmdate('Y-m-d\TH:i:s', intdiv($ms, 1000)) . sprintf('.%03dZ', $ms % 1000);
    }

    // 'YYYY-MM-DD HH:MM:SS' (UTC, de la BD) | null --> ISO | null
    public static function isoDeBD(?string $f): ?string {
        return $f === null ? null : self::fechaISO(self::msDeFechaBD($f));
    }

    // ms --> 'YYYY-MM-DD' del día en España
    public static function diaLocal(int $ms): string {
        $d = new DateTime('@' . intdiv($ms, 1000));
        $d->setTimezone(new DateTimeZone(self::ZONA_HORARIA));
        return $d->format('Y-m-d');
    }

    // 'YYYY-MM-DD' (España) --> ms UTC de las 00:00 de ese día
    public static function inicioDia(string $dia): int {
        $d = new DateTime($dia . ' 00:00:00', new DateTimeZone(self::ZONA_HORARIA));
        return $d->getTimestamp() * 1000;
    }

    // 'YYYY-MM-DD' + n días --> 'YYYY-MM-DD'
    public static function sumarDias(string $dia, int $n): string {
        $d = new DateTime($dia . ' 12:00:00', new DateTimeZone(self::ZONA_HORARIA));
        $d->modify(($n >= 0 ? '+' : '') . $n . ' day');
        return $d->format('Y-m-d');
    }

    // ---------- SQL ----------

    // nombre de tabla con el prefijo
    protected function t(string $tabla): string {
        return $this->p . $tabla;
    }

    protected function ejecutar(string $sql, array $params = []): PDOStatement {
        $st = $this->bd->prepare($sql);
        $st->execute($params);
        return $st;
    }

    protected function fila(string $sql, array $params = []): ?array {
        $f = $this->ejecutar($sql, $params)->fetch(PDO::FETCH_ASSOC);
        return $f === false ? null : $f;
    }

    protected function filas(string $sql, array $params = []): array {
        return $this->ejecutar($sql, $params)->fetchAll(PDO::FETCH_ASSOC);
    }

    protected function valor(string $sql, array $params = []) {
        $v = $this->ejecutar($sql, $params)->fetchColumn();
        return $v === false ? null : $v;
    }

    // Ejecuta fn() en una transacción (si ya hay una abierta, se une a ella)
    protected function transaccion(callable $fn) {
        if ($this->bd->inTransaction()) {
            return $fn();
        }
        $this->bd->beginTransaction();
        try {
            $r = $fn();
            $this->bd->commit();
            return $r;
        } catch (Throwable $e) {
            $this->bd->rollBack();
            throw $e;
        }
    }

    // Código aleatorio legible: prefijo + n caracteres sin 0/O ni 1/I
    protected static function codigoAleatorio(string $prefijo, int $n = 4): string {
        $abc = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
        $s = $prefijo;
        for ($i = 0; $i < $n; $i++) {
            $s .= $abc[random_int(0, strlen($abc) - 1)];
        }
        return $s;
    }

    // Comprueba el rol de quien hace la petición (sesión ya validada)
    // --------------------------------------------------------------
    // codigo --> ultimaMin() --> minutos desde el último dato del sensor | null
    // (no se guarda en nodo: sale de medicion; con el índice
    //  (nodo, magnitud, medida_en) son tres lecturas directas)
    // --------------------------------------------------------------
    protected function ultimaMin(string $codigo): ?int {
        $ultima = null;
        foreach (['o3', 'temp', 'ruido'] as $m) {
            $f = $this->valor("SELECT MAX(fecha_medicion) FROM {$this->p}medicion WHERE id_nodo = ? AND tipo = ?", [$codigo, $m]);
            if ($f) {
                $ultima = max($ultima ?? 0, self::msDeFechaBD($f));
            }
        }
        return $ultima === null ? null : max(0, intdiv($this->ahora() - $ultima, 60000));
    }

    protected static function exigirRol(?array $usuario, array $roles): void {
        if ($usuario === null) {
            throw new NoAutorizadoException('Inicia sesión para continuar.');
        }
        if (!in_array($usuario['rol'], $roles, true)) {
            throw new ProhibidoException('Tu cuenta no tiene permiso para hacer esto.');
        }
    }
}
