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
    const ESTADOS_DEVOLUCION = ['Correcto', 'Defectuoso'];   // «No devuelto» es el de un préstamo en curso
    const PLAZO_PRESTAMO_MS = 10 * self::DIA_MS;           // 10 días desde la recogida
    const NO_ASIGNADO = 'No asignado';
    const SIN_INCIDENCIAS = 'Sin incidencias';
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
    // nodosPanel() --> [{codigo, tipo, magnitudes, estado, asignado_a,
    //   ultima_min, motivo_mant, prestamo: {usuario, desde} | null}]
    // --------------------------------------------------------------
    public function nodosPanel(?array $quien): array {
        self::exigirRol($quien, self::ROLES_PANEL);
        $ahora = $this->ahora();
        $out = [];
        foreach ($this->filas("SELECT * FROM {$this->p}nodo WHERE estado <> 'baja' ORDER BY codigo") as $n) {
            $p = $this->fila("SELECT usuario_id, inicio, fecha_limite FROM {$this->p}prestamo WHERE nodo = ? AND estado_devolucion = 'No devuelto'", [$n['codigo']]);
            $out[] = ['codigo' => $n['codigo'], 'tipo' => $n['tipo'],
                'magnitudes' => $n['magnitudes'] === '' ? [] : explode(',', $n['magnitudes']),
                'estado' => $n['estado'], 'asignado_a' => $n['asignado_a'] === self::NO_ASIGNADO ? null : $n['asignado_a'],
                'ultima_min' => $this->ultimaMin($n['codigo']),
                'motivo_mant' => $n['estado'] === 'mant' ? $n['motivo'] : null,
                'prestamo' => $p ? ['usuario' => $p['usuario_id'], 'desde' => self::isoDeBD($p['inicio']),
                    'hasta' => self::isoDeBD($p['fecha_limite'])] : null];
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
        return [$codigo, $tipo, implode(',', $mags), $tipo === 'vol' ? self::NO_ASIGNADO : $asig, $mant,
            $mant ? $motivo : self::SIN_INCIDENCIAS];
    }

    public function altaNodo(?array $quien, array $d): array {
        self::exigirRol($quien, ['admin', 'tecnico']);
        [$codigo, $tipo, $mags, $asig, $mant, $motivo] = $this->validarNodo($d, null);
        $estado = $mant ? 'mant' : ($tipo === 'vol' ? 'disponible' : 'activo');
        $this->ejecutar("INSERT INTO {$this->p}nodo (codigo, tipo, magnitudes, estado, asignado_a, motivo)
            VALUES (?, ?, ?, ?, ?, ?)",
            [$codigo, $tipo, $mags, $estado, $asig, $motivo]);
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
        $this->ejecutar("UPDATE {$this->p}nodo SET tipo = ?, magnitudes = ?, asignado_a = ?, estado = ?, motivo = ? WHERE codigo = ?",
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
        $this->ejecutar("UPDATE {$this->p}nodo SET estado = 'baja', motivo = ? WHERE codigo = ?",
            [mb_substr(trim($motivo), 0, 120), $c]);
    }

    // ---------- panel: préstamos ----------

    // --------------------------------------------------------------
    // prestamos() --> {abiertos: [..], cerrados: [..]}
    //   {id, nodo, usuario, punto, inicio, fecha_limite, estado_devolucion, fuera_de_plazo}
    //   abiertos = «No devuelto»; fuera_de_plazo = ya pasó la fecha límite
    // --------------------------------------------------------------
    public function prestamos(?array $quien): array {
        self::exigirRol($quien, self::ROLES_PANEL);
        $sql = "SELECT p.*, r.nombre AS punto FROM {$this->p}prestamo p
            JOIN {$this->p}punto_reparto r ON r.id = p.punto_reparto_id";
        $ahora = $this->ahora();
        $map = function ($p) use ($ahora) {
            return ['id' => (int) $p['id'], 'nodo' => $p['nodo'], 'usuario' => $p['usuario_id'], 'punto' => $p['punto'],
                'inicio' => self::isoDeBD($p['inicio']), 'fecha_limite' => self::isoDeBD($p['fecha_limite']),
                'estado_devolucion' => $p['estado_devolucion'],
                'fuera_de_plazo' => $p['estado_devolucion'] === 'No devuelto' && self::msDeFechaBD($p['fecha_limite']) < $ahora];
        };
        return ['abiertos' => array_map($map, $this->filas("$sql WHERE p.estado_devolucion = 'No devuelto' ORDER BY p.inicio DESC")),
            'cerrados' => array_map($map, $this->filas("$sql WHERE p.estado_devolucion <> 'No devuelto' ORDER BY p.inicio DESC LIMIT 50"))];
    }

    // --------------------------------------------------------------
    // usuarioId (U-XXXXX), codigo, punto (id del punto de reparto) --> prestar() --> préstamo
    // La fecha límite es la hora exacta de la recogida + 10 días.
    // --------------------------------------------------------------
    public function prestar(?array $quien, string $usuarioId, string $codigo, $punto = null): array {
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
        } elseif ($this->fila("SELECT id FROM {$this->p}prestamo WHERE usuario_id = ? AND estado_devolucion = 'No devuelto'", [$u])) {
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
            $quienLoTiene = $this->valor("SELECT usuario_id FROM {$this->p}prestamo WHERE nodo = ? AND estado_devolucion = 'No devuelto'", [$c]);
            $e['codigo'] = "$c ya está prestado" . ($quienLoTiene ? " a $quienLoTiene" : '') . '.';
        } elseif ($n['estado'] === 'mant') {
            $e['codigo'] = "$c está en mantenimiento.";
        }
        if ($punto === null || $punto === '' || !$this->fila("SELECT id FROM {$this->p}punto_reparto WHERE id = ? AND activo = 1", [(int) $punto])) {
            $e['punto'] = 'Elige el punto de reparto donde se recoge.';
        }
        if ($e) {
            throw new ErroresDeCamposException($e);
        }
        $ahora = $this->ahora();
        return $this->transaccion(function () use ($u, $c, $ahora, $punto) {
            $this->ejecutar("INSERT INTO {$this->p}prestamo (nodo, usuario_id, punto_reparto_id, inicio, fecha_limite, estado_devolucion)
                VALUES (?, ?, ?, ?, ?, 'No devuelto')",
                [$c, $u, (int) $punto, self::fechaBD($ahora), self::fechaBD($ahora + self::PLAZO_PRESTAMO_MS)]);
            $this->ejecutar("UPDATE {$this->p}nodo SET estado = 'prestado' WHERE codigo = ?", [$c]);
            return $this->prestamos(['rol' => 'admin'])['abiertos'][0];
        });
    }

    // --------------------------------------------------------------
    // codigo, estado ('Correcto' | 'Defectuoso') --> devolver()
    // Se puede devolver en cualquier punto de reparto, también fuera de plazo.
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
        $p = $this->fila("SELECT * FROM {$this->p}prestamo WHERE nodo = ? AND estado_devolucion = 'No devuelto'", [$c]);
        if (!$p) {
            throw new DatosNoValidosException("$c no figura como prestado.", 'codigo');
        }
        if (!in_array($estado, self::ESTADOS_DEVOLUCION, true)) {
            throw new DatosNoValidosException('Elige cómo llega el sensor.', 'estado');
        }
        $this->transaccion(function () use ($p, $c, $estado) {
            $this->ejecutar("UPDATE {$this->p}prestamo SET estado_devolucion = ? WHERE id = ?", [$estado, $p['id']]);
            if ($estado === 'Defectuoso') {
                $this->ejecutar("UPDATE {$this->p}nodo SET estado = 'mant', motivo = 'No se ha entregado en buenas condiciones' WHERE codigo = ?", [$c]);
            } else {
                $this->ejecutar("UPDATE {$this->p}nodo SET estado = 'disponible', motivo = ? WHERE codigo = ?", [self::SIN_INCIDENCIAS, $c]);
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
            return ['codigo' => $c, 'magnitudes' => $mags, 'desde' => null, 'hasta' => null];
        }
        $p = $this->fila("SELECT * FROM {$this->p}prestamo WHERE nodo = ? AND estado_devolucion = 'No devuelto'", [$c]);
        if ($p && $p['usuario_id'] === $quien['id']) {
            return ['codigo' => $c, 'magnitudes' => $mags, 'desde' => self::isoDeBD($p['inicio']),
                'hasta' => self::isoDeBD($p['fecha_limite'])];
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

    // préstamo en curso de un vecino (para «Mi sensor») --> {codigo, desde, hasta} | null
    // hasta = fecha límite para devolverlo (10 días desde la recogida)
    public function prestamoDe(string $usuarioId): ?array {
        $p = $this->fila("SELECT nodo, inicio, fecha_limite FROM {$this->p}prestamo WHERE usuario_id = ? AND estado_devolucion = 'No devuelto'", [$usuarioId]);
        return $p ? ['codigo' => $p['nodo'], 'desde' => self::isoDeBD($p['inicio']), 'hasta' => self::isoDeBD($p['fecha_limite'])] : null;
    }
}
