<?php
// ==============================================================
// Fichero:     Flota.php
// Descripción: Sensores y préstamos:
//              - panel: estado de la flota, alta, edición, mantenimiento
//                y baja de sensores; préstamos y devoluciones en los
//                puntos de reparto.
//              - app: comprobar el código del sensor que va a usar
//                quien tiene la sesión (préstamo en curso a su nombre, o
//                sensor de su servicio si es trabajador municipal).
//              - público: ayuntamientos y puntos de reparto.
// Diseño:      doc/servidor.md
// Autor:       Aaron Blasco Blay
// Fecha:       09/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================

require_once __DIR__ . '/Base.php';

class Flota extends Base {

    const TIPOS = ['vol' => 'Voluntariado', 'trab' => 'Trabajador público', 'bici' => 'Bici o moto compartida', 'bus' => 'Autobús'];
    const MAGNITUDES = ['o3', 'temp', 'ruido'];
    const ESTADOS_DEVOLUCION = ['Correcto', 'Batería baja', 'Dañado'];
    const ROLES_PANEL = ['admin', 'tecnico', 'reparto'];

    // ---------- público ----------

    public function ayuntamientos(): array {
        return array_map(function ($a) {
            return ['id' => $a['id'], 'nombre' => $a['nombre'], 'comarca' => $a['comarca'], 'desde' => $a['desde'],
                'activo' => (bool) $a['activo']];
        }, $this->filas("SELECT * FROM {$this->p}ayuntamiento ORDER BY nombre"));
    }

    public function puntosReparto(): array {
        return array_map(function ($p) {
            return ['id' => (int) $p['id'], 'nombre' => $p['nombre'], 'direccion' => $p['direccion'], 'horario' => $p['horario']];
        }, $this->filas("SELECT * FROM {$this->p}punto_reparto WHERE activo = 1 ORDER BY id"));
    }

    // ---------- panel: flota ----------

    // --------------------------------------------------------------
    // nodosPanel() --> [{codigo, tipo, magnitudes, estado, asignado_a, bateria,
    //   ultima_min, motivo_mant, prestamo: {usuario, desde} | null}]
    // --------------------------------------------------------------
    public function nodosPanel(?array $quien): array {
        self::exigirRol($quien, self::ROLES_PANEL);
        $ahora = $this->ahora();
        $out = [];
        foreach ($this->filas("SELECT * FROM {$this->p}nodo WHERE estado <> 'baja' ORDER BY codigo") as $n) {
            $p = $this->fila("SELECT usuario_id, inicio FROM {$this->p}prestamo WHERE nodo = ? AND fin IS NULL", [$n['codigo']]);
            $out[] = ['codigo' => $n['codigo'], 'tipo' => $n['tipo'],
                'magnitudes' => $n['magnitudes'] === '' ? [] : explode(',', $n['magnitudes']),
                'estado' => $n['estado'], 'asignado_a' => $n['asignado_a'],
                'bateria' => $n['bateria'] === null ? null : (int) $n['bateria'],
                'ultima_min' => $n['ultima_medicion'] ? max(0, intdiv($ahora - self::msDeFechaBD($n['ultima_medicion']), 60000)) : null,
                'motivo_mant' => $n['motivo_mant'],
                'prestamo' => $p ? ['usuario' => $p['usuario_id'], 'desde' => self::isoDeBD($p['inicio'])] : null];
        }
        return $out;
    }

    private function nodoPanel(string $codigo): array {
        foreach ($this->nodosPanel(['rol' => 'admin']) as $n) {
            if ($n['codigo'] === $codigo) {
                return $n;
            }
        }
        throw new NoEncontradoException("No existe el sensor $codigo.");
    }

    // --------------------------------------------------------------
    // Alta y edición. d: {codigo, tipo, magnitudes: [..], asignado_a, mantenimiento: B, motivo}
    // --------------------------------------------------------------
    private function validarNodo(array $d, ?array $actual): array {
        $e = [];
        $codigo = strtoupper(trim((string) ($d['codigo'] ?? '')));
        if ($actual === null) {
            if ($codigo === '') {
                $e['codigo'] = 'Escribe el código del sensor.';
            } elseif (!preg_match('/^RSP-[A-Z0-9]{4}$/', $codigo)) {
                $e['codigo'] = 'Formato no válido: RSP- y 4 letras o números, por ejemplo RSP-7Z2K.';
            } elseif ($this->fila("SELECT codigo FROM {$this->p}nodo WHERE codigo = ?", [$codigo])) {
                $e['codigo'] = "Ya existe un sensor con el código $codigo.";
            }
        }
        $tipo = (string) ($d['tipo'] ?? 'vol');
        if (!isset(self::TIPOS[$tipo])) {
            $e['tipo'] = 'Tipo de portador no válido.';
        } elseif ($actual && $actual['estado'] === 'prestado' && $tipo !== $actual['tipo']) {
            $e['tipo'] = 'Está prestado: registra la devolución antes de cambiar el tipo.';
        }
        $mags = array_values(array_intersect(self::MAGNITUDES, (array) ($d['magnitudes'] ?? [])));
        if (!$mags) {
            $e['magnitudes'] = 'Marca al menos una magnitud.';
        }
        $asig = trim((string) ($d['asignado_a'] ?? ''));
        if ($tipo !== 'vol' && mb_strlen($asig) < 3) {
            $e['asignado_a'] = 'Indica a qué servicio, línea o vehículo va asignado (mínimo 3 caracteres).';
        }
        $mant = !empty($d['mantenimiento']);
        $motivo = trim((string) ($d['motivo'] ?? ''));
        if ($mant && mb_strlen($motivo) < 5) {
            $e['motivo'] = 'Explica el motivo del mantenimiento (mínimo 5 caracteres).';
        }
        if ($e) {
            throw new ErroresDeCamposException($e);
        }
        return [$codigo, $tipo, implode(',', $mags), $tipo === 'vol' ? null : $asig, $mant, $mant ? $motivo : null];
    }

    public function altaNodo(?array $quien, array $d): array {
        self::exigirRol($quien, ['admin', 'tecnico']);
        [$codigo, $tipo, $mags, $asig, $mant, $motivo] = $this->validarNodo($d, null);
        $estado = $mant ? 'mant' : ($tipo === 'vol' ? 'disponible' : 'activo');
        $this->ejecutar("INSERT INTO {$this->p}nodo (codigo, uuid_beacon, tipo, magnitudes, estado, asignado_a, bateria, motivo_mant)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            [$codigo, 'RESPIRA-' . $codigo, $tipo, $mags, $estado, $asig, $tipo === 'bus' ? null : 100, $motivo]);
        return $this->nodoPanel($codigo);
    }

    public function editarNodo(?array $quien, string $codigo, array $d): array {
        self::exigirRol($quien, ['admin', 'tecnico']);
        $c = strtoupper($codigo);
        $n = $this->fila("SELECT * FROM {$this->p}nodo WHERE codigo = ? AND estado <> 'baja'", [$c]);
        if (!$n) {
            throw new NoEncontradoException("No existe el sensor $c.");
        }
        [, $tipo, $mags, $asig, $mant, $motivo] = $this->validarNodo($d, $n);
        if ($mant) {
            if ($n['estado'] === 'prestado') {
                throw new ConflictoException('Está prestado: registra la devolución antes de pasarlo a mantenimiento.', 'estado');
            }
            $estado = 'mant';
        } elseif ($n['estado'] === 'prestado') {
            $estado = 'prestado';
        } else {
            $estado = $tipo === 'vol' ? 'disponible' : 'activo';
        }
        $this->ejecutar("UPDATE {$this->p}nodo SET tipo = ?, magnitudes = ?, asignado_a = ?, estado = ?, motivo_mant = ? WHERE codigo = ?",
            [$tipo, $mags, $asig, $estado, $motivo, $c]);
        return $this->nodoPanel($c);
    }

    // Baja (solo administración). Las mediciones se conservan.
    public function bajaNodo(?array $quien, string $codigo, string $motivo): void {
        self::exigirRol($quien, ['admin']);
        $c = strtoupper($codigo);
        $n = $this->fila("SELECT * FROM {$this->p}nodo WHERE codigo = ? AND estado <> 'baja'", [$c]);
        if (!$n) {
            throw new NoEncontradoException("No existe el sensor $c.");
        }
        if ($n['estado'] === 'prestado') {
            throw new ConflictoException('Está prestado: primero registra la devolución.');
        }
        if (trim($motivo) === '') {
            throw new DatosNoValidosException('Elige un motivo.', 'motivo');
        }
        $this->ejecutar("UPDATE {$this->p}nodo SET estado = 'baja', baja = ?, motivo_baja = ? WHERE codigo = ?",
            [self::fechaBD($this->ahora()), mb_substr(trim($motivo), 0, 120), $c]);
    }

    // ---------- panel: préstamos ----------

    // --------------------------------------------------------------
    // prestamos() --> {abiertos: [..], cerrados: [..]}
    //   {id, nodo, usuario, punto, inicio, fin, estado_devolucion}
    // --------------------------------------------------------------
    public function prestamos(?array $quien): array {
        self::exigirRol($quien, self::ROLES_PANEL);
        $sql = "SELECT p.*, r.nombre AS punto FROM {$this->p}prestamo p
            LEFT JOIN {$this->p}punto_reparto r ON r.id = p.punto_reparto_id";
        $map = function ($p) {
            return ['id' => (int) $p['id'], 'nodo' => $p['nodo'], 'usuario' => $p['usuario_id'], 'punto' => $p['punto'],
                'inicio' => self::isoDeBD($p['inicio']), 'fin' => self::isoDeBD($p['fin']),
                'estado_devolucion' => $p['estado_devolucion']];
        };
        return ['abiertos' => array_map($map, $this->filas("$sql WHERE p.fin IS NULL ORDER BY p.inicio DESC")),
            'cerrados' => array_map($map, $this->filas("$sql WHERE p.fin IS NOT NULL ORDER BY p.fin DESC LIMIT 50"))];
    }

    // --------------------------------------------------------------
    // usuarioId (U-XXXXX), codigo --> prestar() --> préstamo
    // --------------------------------------------------------------
    public function prestar(?array $quien, string $usuarioId, string $codigo): array {
        self::exigirRol($quien, self::ROLES_PANEL);
        $u = strtoupper(trim($usuarioId));
        $c = strtoupper(trim($codigo));
        $e = [];
        if ($u === '') {
            $e['usuario'] = 'Escribe el ID que aparece en la app del voluntario.';
        } elseif (!preg_match('/^U-\d{5}$/', $u)) {
            $e['usuario'] = 'Formato no válido: U- y 5 cifras, por ejemplo U-48213.';
        } elseif (!$this->fila("SELECT id FROM {$this->p}usuario WHERE id = ? AND rol = 'vecino'", [$u])) {
            $e['usuario'] = "No existe ningún usuario $u. Pídele que lo compruebe en la app, en Mi cuenta.";
        } elseif ($this->fila("SELECT id FROM {$this->p}prestamo WHERE usuario_id = ? AND fin IS NULL", [$u])) {
            $e['usuario'] = "$u ya tiene un sensor prestado. Tiene que devolverlo antes de llevarse otro.";
        }
        $n = $c === '' ? null : $this->fila("SELECT * FROM {$this->p}nodo WHERE codigo = ? AND estado <> 'baja'", [$c]);
        if ($c === '') {
            $e['codigo'] = 'Escribe el código del sensor.';
        } elseif (!preg_match('/^RSP-[A-Z0-9]{4}$/', $c)) {
            $e['codigo'] = 'Formato no válido: RSP- y 4 letras o números.';
        } elseif (!$n) {
            $e['codigo'] = "No existe ningún sensor $c.";
        } elseif ($n['tipo'] !== 'vol') {
            $e['codigo'] = "$c va asignado a " . mb_strtolower(self::TIPOS[$n['tipo']]) . ': no se presta.';
        } elseif ($n['estado'] === 'prestado') {
            $quienLoTiene = $this->valor("SELECT usuario_id FROM {$this->p}prestamo WHERE nodo = ? AND fin IS NULL", [$c]);
            $e['codigo'] = "$c ya está prestado" . ($quienLoTiene ? " a $quienLoTiene" : '') . '.';
        } elseif ($n['estado'] === 'mant') {
            $e['codigo'] = "$c está en mantenimiento.";
        }
        if ($e) {
            throw new ErroresDeCamposException($e);
        }
        $ahora = $this->ahora();
        return $this->transaccion(function () use ($quien, $u, $c, $ahora) {
            $this->ejecutar("INSERT INTO {$this->p}prestamo (nodo, usuario_id, punto_reparto_id, inicio, prestado_por) VALUES (?, ?, ?, ?, ?)",
                [$c, $u, $quien['punto_reparto_id'] ?? null, self::fechaBD($ahora), $quien['id']]);
            $this->ejecutar("UPDATE {$this->p}nodo SET estado = 'prestado' WHERE codigo = ?", [$c]);
            return $this->prestamos($quien)['abiertos'][0];
        });
    }

    // --------------------------------------------------------------
    // codigo, estado ('Correcto' | 'Batería baja' | 'Dañado') --> devolver()
    // --------------------------------------------------------------
    public function devolver(?array $quien, string $codigo, string $estado): array {
        self::exigirRol($quien, self::ROLES_PANEL);
        $c = strtoupper(trim($codigo));
        if ($c === '') {
            throw new DatosNoValidosException('Escribe el código del sensor.', 'codigo');
        }
        if (!preg_match('/^RSP-[A-Z0-9]{4}$/', $c)) {
            throw new DatosNoValidosException('Formato no válido: RSP- y 4 letras o números.', 'codigo');
        }
        if (!$this->fila("SELECT codigo FROM {$this->p}nodo WHERE codigo = ?", [$c])) {
            throw new DatosNoValidosException("No existe ningún sensor $c.", 'codigo');
        }
        $p = $this->fila("SELECT * FROM {$this->p}prestamo WHERE nodo = ? AND fin IS NULL", [$c]);
        if (!$p) {
            throw new DatosNoValidosException("$c no figura como prestado.", 'codigo');
        }
        if (!in_array($estado, self::ESTADOS_DEVOLUCION, true)) {
            throw new DatosNoValidosException('Elige cómo llega el sensor.', 'estado');
        }
        $ahora = $this->ahora();
        $this->transaccion(function () use ($quien, $p, $c, $estado, $ahora) {
            $this->ejecutar("UPDATE {$this->p}prestamo SET fin = ?, estado_devolucion = ?, recibido_por = ? WHERE id = ?",
                [self::fechaBD($ahora), $estado, $quien['id'], $p['id']]);
            if ($estado === 'Dañado') {
                $this->ejecutar("UPDATE {$this->p}nodo SET estado = 'mant', motivo_mant = 'Dañado al devolverlo' WHERE codigo = ?", [$c]);
            } else {
                $this->ejecutar("UPDATE {$this->p}nodo SET estado = 'disponible' WHERE codigo = ?", [$c]);
            }
        });
        return $this->nodoPanel($c);
    }

    // ---------- app: el sensor que voy a llevar ----------

    // --------------------------------------------------------------
    // quien, codigo --> sensorParaLaApp() --> {codigo, magnitudes, desde}
    // Los mensajes son los que enseña la app debajo del campo del código.
    // --------------------------------------------------------------
    public function sensorParaLaApp(?array $quien, string $codigo): array {
        self::exigirRol($quien, ['vecino', 'trabajador']);
        $c = strtoupper(trim($codigo));
        if ($c === '') {
            throw new DatosNoValidosException('Escribe el código que lleva impreso el sensor.', 'codigo');
        }
        if (!preg_match('/^RSP-[A-Z0-9]{4}$/', $c)) {
            throw new DatosNoValidosException('Formato no válido: es RSP- seguido de 4 letras o números. Ejemplo: RSP-4F7K.', 'codigo');
        }
        $n = $this->fila("SELECT * FROM {$this->p}nodo WHERE codigo = ? AND estado <> 'baja'", [$c]);
        if (!$n) {
            throw new NoEncontradoException("No existe ningún sensor con el código $c. Revisa que lo has copiado bien.");
        }
        $mags = $n['magnitudes'] === '' ? [] : explode(',', $n['magnitudes']);
        if ($quien['rol'] === 'trabajador') {
            if ($n['tipo'] !== 'trab' || $n['estado'] !== 'activo') {
                throw new ConflictoException("El sensor $c no está asignado a un servicio municipal. Pide a tu servicio el código correcto.", 'codigo');
            }
            return ['codigo' => $c, 'magnitudes' => $mags, 'desde' => null];
        }
        $p = $this->fila("SELECT * FROM {$this->p}prestamo WHERE nodo = ? AND fin IS NULL", [$c]);
        if ($p && $p['usuario_id'] === $quien['id']) {
            return ['codigo' => $c, 'magnitudes' => $mags, 'desde' => self::isoDeBD($p['inicio'])];
        }
        if ($p) {
            throw new ConflictoException("El sensor $c está prestado a otra persona. Comprueba el código o pregunta en el punto de reparto.", 'codigo');
        }
        $ult = $this->fila("SELECT usuario_id FROM {$this->p}prestamo WHERE nodo = ? ORDER BY inicio DESC LIMIT 1", [$c]);
        if ($ult && $ult['usuario_id'] === $quien['id']) {
            throw new ConflictoException("El sensor $c ya se devolvió. Pide uno nuevo en el punto de reparto.", 'codigo');
        }
        throw new ConflictoException("El sensor $c no está prestado a nadie. Pide en el punto de reparto que lo registren con tu ID {$quien['id']}.", 'codigo');
    }

    // préstamo en curso de un vecino (para «Mi sensor») --> {codigo, desde} | null
    public function prestamoDe(string $usuarioId): ?array {
        $p = $this->fila("SELECT nodo, inicio FROM {$this->p}prestamo WHERE usuario_id = ? AND fin IS NULL", [$usuarioId]);
        return $p ? ['codigo' => $p['nodo'], 'desde' => self::isoDeBD($p['inicio'])] : null;
    }
}
