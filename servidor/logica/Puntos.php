<?php
// ==============================================================
// Fichero:     Puntos.php
// Descripción: Puntos del voluntariado, regalos y canjes.
//
//   - Los puntos los calcula el SERVIDOR con las mediciones que llevan
//     el ID de la persona como portador (no los manda el móvil).
//   - Los puntos de cada día NO se guardan: se calculan cada vez con las
//     mediciones (así nunca se descuadran). Solo se guardan los días
//     sospechosos que revisa administración (tabla «revision»); se
//     detectan la primera vez que alguien pide los datos después de que
//     acabe el día (no hace falta una tarea programada).
//   - Saldo = puntos de los días acabados no anulados − canjes no caducados.
//     Un canje caducado sin recoger devuelve los puntos y la unidad.
//
//   Minutos válidos (entre dos datos seguidos del mismo portador):
//     - más de 15 min sin datos           --> no cuentan
//     - más de 25 km/h (va en un vehículo) --> no cuentan
//     - parado (menos de 20 m) más de 15 min seguidos --> no cuentan
//   Puntos = min(máx, ⌊min/N⌋ + ⌊min_parque/N⌋ · (mult − 1))
// Diseño:      doc/servidor.md
// Autor:       Aaron Blasco Blay
// Fecha:       09/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================

require_once __DIR__ . '/Base.php';

class Puntos extends Base {

    const HUECO_MAX_MIN = 15;
    const VELOCIDAD_MAX_KMH = 25;
    const PARADO_M = 20;
    const PARADO_MAX_MIN = 15;
    const DIAS_ATRAS = 60;      // como mucho se recalculan 60 días
    const CANJE_DIAS = 7;       // días para recoger un regalo

    // ==============================================================
    // Reglas
    // ==============================================================

    // dia 'YYYY-MM-DD' --> reglasDe() --> {min, mult, max}
    public function reglasDe(string $dia): array {
        $r = $this->fila("SELECT min_por_punto, mult_parque, max_dia FROM {$this->p}reglas_puntos
            WHERE desde <= ? ORDER BY desde DESC, id DESC LIMIT 1", [$dia]);
        if (!$r) {
            return ['min' => 5, 'mult' => 2, 'max' => 100];
        }
        return ['min' => (int) $r['min_por_punto'], 'mult' => (int) $r['mult_parque'], 'max' => (int) $r['max_dia']];
    }

    // --------------------------------------------------------------
    // reglas() --> {min, mult, max, desde_manana: {min, mult, max} | null}
    // --------------------------------------------------------------
    public function reglas(?array $quien): array {
        self::exigirRol($quien, ['admin']);
        $hoy = self::diaLocal($this->ahora());
        $r = $this->reglasDe($hoy);
        $m = $this->reglasDe(self::sumarDias($hoy, 1));
        $r['desde_manana'] = $m == array_intersect_key($r, $m) ? null : $m;
        return $r;
    }

    // --------------------------------------------------------------
    // Cambiar las reglas: se aplican desde mañana (los días ya empezados no cambian)
    // --------------------------------------------------------------
    public function guardarReglas(?array $quien, $min, $mult, $max): array {
        self::exigirRol($quien, ['admin']);
        $e = [];
        $chk = function ($k, $v, $a, $b, $msg) use (&$e) {
            if (!preg_match('/^\d+$/', trim((string) $v))) {
                $e[$k] = 'Escribe un número entero.';
            } elseif ((int) $v < $a || (int) $v > $b) {
                $e[$k] = $msg;
            }
        };
        $chk('min', $min, 1, 60, 'Debe estar entre 1 y 60 minutos.');
        $chk('mult', $mult, 1, 5, 'Debe estar entre 1 y 5.');
        $chk('max', $max, 10, 500, 'Debe estar entre 10 y 500 puntos.');
        if ($e) {
            throw new ErroresDeCamposException($e);
        }
        $manana = self::sumarDias(self::diaLocal($this->ahora()), 1);
        $this->ejecutar("DELETE FROM {$this->p}reglas_puntos WHERE desde = ?", [$manana]);
        $this->ejecutar("INSERT INTO {$this->p}reglas_puntos (min_por_punto, mult_parque, max_dia, desde) VALUES (?, ?, ?, ?)",
            [(int) $min, (int) $mult, (int) $max, $manana]);
        return $this->reglas($quien);
    }

    public static function puntosDe(int $min, int $minParque, array $r): int {
        $p = intdiv($min, $r['min']) + intdiv($minParque, $r['min']) * ($r['mult'] - 1);
        return max(0, min($r['max'], $p));
    }

    // distancia en metros entre dos puntos (haversine)
    public static function metros(float $lat1, float $lng1, float $lat2, float $lng2): float {
        $r = 6371000.0;
        $dLat = deg2rad($lat2 - $lat1);
        $dLng = deg2rad($lng2 - $lng1);
        $a = sin($dLat / 2) ** 2 + cos(deg2rad($lat1)) * cos(deg2rad($lat2)) * sin($dLng / 2) ** 2;
        return 2 * $r * asin(min(1.0, sqrt($a)));
    }

    // ==============================================================
    // Cálculo de un día
    // ==============================================================

    // --------------------------------------------------------------
    // usuarioId, dia, hastaMs? --> calcularDia() -->
    //   {minutos, minutos_parque, descartados, puntos, motivo | null}
    // motivo != null: el día se marca para revisar
    // --------------------------------------------------------------
    public function calcularDia(string $usuarioId, string $dia, ?int $hastaMs = null): array {
        $ini = self::inicioDia($dia);
        $fin = $hastaMs ?? self::inicioDia(self::sumarDias($dia, 1));
        // Un punto por anuncio (las 3 magnitudes de un mismo instante llegan juntas)
        $filas = $this->filas("SELECT m.fecha_medicion AS medida_en, m.lat, m.lng, z.tipo AS tipo_zona
            FROM {$this->p}medicion m JOIN {$this->p}zona z ON z.id = m.zona_id
            WHERE m.portador_id = ? AND m.fecha_medicion >= ? AND m.fecha_medicion < ?
            ORDER BY m.fecha_medicion", [$usuarioId, self::fechaBD($ini), self::fechaBD($fin)]);
        $puntos = [];
        foreach ($filas as $f) {
            $t = self::msDeFechaBD($f['medida_en']);
            $ult = end($puntos);
            if ($ult && $t - $ult['t'] < 1000) {
                if ($ult['lat'] === null && $f['lat'] !== null) {
                    $puntos[key($puntos)]['lat'] = (float) $f['lat'];
                    $puntos[key($puntos)]['lng'] = (float) $f['lng'];
                }
                continue;
            }
            $puntos[] = ['t' => $t, 'lat' => $f['lat'] === null ? null : (float) $f['lat'],
                'lng' => $f['lng'] === null ? null : (float) $f['lng'], 'parque' => $f['tipo_zona'] === 'parque'];
        }
        $valido = 0.0; $parque = 0.0; $desc = 0.0; $paradoTotal = 0.0;
        // rato «parado»: desde ancla, sin alejarse más de PARADO_M
        $ancla = null; $rato = 0.0; $ratoParque = 0.0; $descartando = false;
        $reiniciar = function () use (&$ancla, &$rato, &$ratoParque, &$descartando) {
            $ancla = null; $rato = 0.0; $ratoParque = 0.0; $descartando = false;
        };
        for ($i = 1; $i < count($puntos); $i++) {
            $a = $puntos[$i - 1]; $b = $puntos[$i];
            $dt = ($b['t'] - $a['t']) / 60000.0;
            if ($dt > self::HUECO_MAX_MIN) {           // sin datos demasiado tiempo
                $desc += $dt; $reiniciar();
                continue;
            }
            if ($a['lat'] !== null && $b['lat'] !== null) {
                $d = self::metros($a['lat'], $a['lng'], $b['lat'], $b['lng']);
                if ($dt > 0 && ($d / 1000) / ($dt / 60) > self::VELOCIDAD_MAX_KMH) {   // en vehículo
                    $desc += $dt; $reiniciar();
                    continue;
                }
                $ancla = $ancla ?? $a;
                if (self::metros($ancla['lat'], $ancla['lng'], $b['lat'], $b['lng']) < self::PARADO_M) {
                    $rato += $dt;
                    if ($descartando) {
                        $desc += $dt; $paradoTotal += $dt;
                        continue;
                    }
                    if ($rato > self::PARADO_MAX_MIN) {   // todo el rato parado deja de contar
                        $valido -= ($rato - $dt);
                        $parque -= $ratoParque;
                        $desc += $rato; $paradoTotal += $rato;
                        $descartando = true;
                        continue;
                    }
                    $valido += $dt;
                    if ($b['parque']) {
                        $parque += $dt; $ratoParque += $dt;
                    }
                    continue;
                }
                $reiniciar();
                $ancla = $b;
            }
            $valido += $dt;
            if ($b['parque']) {
                $parque += $dt;
            }
        }
        $min = (int) floor(max(0, $valido));
        $minParque = (int) floor(max(0, min($parque, $valido)));
        $r = $this->reglasDe($dia);
        $pts = self::puntosDe($min, $minParque, $r);
        $motivo = null;
        if ($paradoTotal >= 60) {
            $motivo = 'Sensor ' . ($paradoTotal >= 120 ? round($paradoTotal / 60) . ' h' : round($paradoTotal) . ' min')
                . ' sin moverse en el mismo punto (descontado)';
        } elseif ($pts >= $r['max']) {
            $motivo = 'Llega al máximo diario';
        } elseif ($pts >= 0.9 * $r['max']) {
            $motivo = 'Llega al 90 % del máximo diario';
        }
        return ['minutos' => $min, 'minutos_parque' => $minParque, 'descartados' => (int) round($desc),
            'puntos' => $pts, 'motivo' => $motivo];
    }

    // --------------------------------------------------------------
    // usuarioId --> dias() --> ['YYYY-MM-DD' => calcularDia(), ...]
    // Días ya acabados en los que llevó un sensor (del más reciente al más antiguo)
    // --------------------------------------------------------------
    private $cacheDias = [];

    public function dias(string $usuarioId): array {
        $hoy = self::diaLocal($this->ahora());
        if (isset($this->cacheDias[$usuarioId . '|' . $hoy])) {
            return $this->cacheDias[$usuarioId . '|' . $hoy];
        }
        $dias = [];
        // una fila por hora con datos (no una por medición) y se pasa a día en hora de España
        foreach ($this->filas("SELECT DISTINCT SUBSTR(fecha_medicion, 1, 13) AS h FROM {$this->p}medicion
            WHERE portador_id = ? AND fecha_medicion < ?", [$usuarioId, self::fechaBD(self::inicioDia($hoy))]) as $f) {
            $dias[self::diaLocal(self::msDeFechaBD($f['h'] . ':00:00'))] = true;
        }
        krsort($dias);
        $out = [];
        foreach (array_keys($dias) as $d) {
            $c = $this->calcularDia($usuarioId, (string) $d);
            if ($c['minutos'] > 0 || $c['descartados'] > 0) {
                $out[(string) $d] = $c;
            }
        }
        return $this->cacheDias[$usuarioId . '|' . $hoy] = $out;
    }

    // --------------------------------------------------------------
    // Marca para revisar los días acabados sospechosos que aún no estén en
    // «revision» (solo los de los últimos 60 días). usuarioId null = todos.
    // --------------------------------------------------------------
    public function cerrarDias(?string $usuarioId = null): void {
        $hoy = self::diaLocal($this->ahora());
        $desde = self::sumarDias($hoy, -self::DIAS_ATRAS);
        $usuarios = $usuarioId ? [$usuarioId] : array_column($this->filas("SELECT DISTINCT portador_id FROM {$this->p}medicion
            WHERE portador_id IS NOT NULL AND fecha_medicion >= ?", [self::fechaBD(self::inicioDia($desde))]), 'portador_id');
        foreach ($usuarios as $u) {
            foreach ($this->dias($u) as $dia => $c) {
                if ((string) $dia < $desde || !$c['motivo']) {
                    continue;
                }
                if (!$this->fila("SELECT id FROM {$this->p}revision WHERE usuario_id = ? AND fecha = ?", [$u, $dia])) {
                    $this->ejecutar("INSERT INTO {$this->p}revision (usuario_id, fecha, motivo, estado, razon) VALUES (?, ?, ?, 'pend', 'Sin revisar')",
                        [$u, $dia, $c['motivo']]);
                }
            }
        }
    }

    // usuarioId --> revisionesDe() --> ['YYYY-MM-DD' => {estado, razon}]
    private function revisionesDe(string $usuarioId): array {
        $out = [];
        foreach ($this->filas("SELECT fecha, estado, razon FROM {$this->p}revision WHERE usuario_id = ?", [$usuarioId]) as $r) {
            $out[$r['fecha']] = $r;
        }
        return $out;
    }

    // Canjes no recogidos a tiempo: caducan, y la unidad vuelve al stock
    public function caducarCanjes(): void {
        $hoy = self::diaLocal($this->ahora());
        $this->transaccion(function () use ($hoy) {
            foreach ($this->filas("SELECT codigo, regalo_id FROM {$this->p}canje WHERE estado = 'pendiente' AND caduca < ?", [$hoy]) as $c) {
                $this->ejecutar("UPDATE {$this->p}canje SET estado = 'caducado' WHERE codigo = ?", [$c['codigo']]);
                $this->ejecutar("UPDATE {$this->p}regalo SET stock = stock + 1 WHERE id = ?", [$c['regalo_id']]);
            }
        });
    }

    public function saldo(string $usuarioId): int {
        $rev = $this->revisionesDe($usuarioId);
        $mas = 0;
        foreach ($this->dias($usuarioId) as $dia => $c) {
            if (($rev[$dia]['estado'] ?? '') !== 'anulado') {
                $mas += $c['puntos'];
            }
        }
        $menos = (int) $this->valor("SELECT COALESCE(SUM(coste), 0) FROM {$this->p}canje WHERE usuario_id = ? AND estado <> 'caducado'", [$usuarioId]);
        return $mas - $menos;
    }

    // --------------------------------------------------------------
    // cuenta(usuario) --> {puntos, hoy, historial, canjes, reglas}
    //   historial: días con sensor y canjes, del más reciente al más antiguo
    // --------------------------------------------------------------
    public function cuenta(array $u): array {
        $this->cerrarDias($u['id']);
        $this->caducarCanjes();
        $ahora = $this->ahora();
        $hoy = self::diaLocal($ahora);
        $h = $this->calcularDia($u['id'], $hoy, $ahora);
        $mov = [];
        $rev = $this->revisionesDe($u['id']);
        foreach (array_slice($this->dias($u['id']), 0, 60, true) as $dia => $c) {
            $estado = $rev[$dia]['estado'] ?? 'auto';
            $mov[] = ['tipo' => 'dia', 'fecha' => (string) $dia, 'minutos' => $c['minutos'],
                'minutos_parque' => $c['minutos_parque'], 'descartados' => $c['descartados'],
                'puntos' => $estado === 'anulado' ? 0 : $c['puntos'], 'revision' => $estado,
                'razon' => $estado === 'anulado' ? $rev[$dia]['razon'] : null, 'canje' => null];
        }
        $canjes = [];
        foreach ($this->filas("SELECT c.*, r.nombre FROM {$this->p}canje c JOIN {$this->p}regalo r ON r.id = c.regalo_id
            WHERE c.usuario_id = ? ORDER BY c.creado DESC", [$u['id']]) as $c) {
            $canjes[] = ['codigo' => $c['codigo'], 'regalo' => $c['nombre'], 'fecha' => self::diaLocal(self::msDeFechaBD($c['creado'])),
                'caduca' => $c['caduca'], 'estado' => $c['estado'], 'coste' => (int) $c['coste']];
            $mov[] = ['tipo' => 'canje', 'fecha' => self::diaLocal(self::msDeFechaBD($c['creado'])), 'minutos' => 0,
                'minutos_parque' => 0, 'descartados' => 0, 'puntos' => $c['estado'] === 'caducado' ? 0 : -(int) $c['coste'],
                'revision' => null, 'razon' => $c['estado'] === 'caducado' ? 'Caducado sin recoger: puntos devueltos' : null,
                'canje' => $c['nombre']];
        }
        usort($mov, function ($a, $b) { return strcmp($b['fecha'], $a['fecha']); });
        return ['puntos' => $this->saldo($u['id']),
            'hoy' => ['minutos' => $h['minutos'], 'minutos_parque' => $h['minutos_parque'],
                'descartados' => $h['descartados'], 'puntos' => $h['puntos']],
            'historial' => $mov, 'canjes' => $canjes, 'reglas' => $this->reglasDe($hoy)];
    }

    // ==============================================================
    // Regalos y canjes
    // ==============================================================

    private static function regaloJSON(array $r): array {
        return ['id' => $r['id'], 'nombre' => $r['nombre'], 'coste' => (int) $r['coste'], 'stock' => (int) $r['stock']];
    }

    // catálogo que se ve en la app (y en el panel)
    public function regalos(): array {
        $this->caducarCanjes();
        return array_map([self::class, 'regaloJSON'],
            $this->filas("SELECT * FROM {$this->p}regalo WHERE activo = 1 ORDER BY coste, nombre"));
    }

    // --------------------------------------------------------------
    // usuario, regaloId --> canjear() --> {codigo, regalo, fecha, caduca, estado, coste}
    // --------------------------------------------------------------
    public function canjear(?array $u, string $regaloId): array {
        self::exigirRol($u, ['vecino', 'trabajador']);
        $this->caducarCanjes();
        $ahora = $this->ahora();
        return $this->transaccion(function () use ($u, $regaloId, $ahora) {
            $g = $this->fila("SELECT * FROM {$this->p}regalo WHERE id = ? AND activo = 1", [$regaloId]);
            if (!$g) {
                throw new NoEncontradoException('Ese regalo ya no está en el catálogo.');
            }
            if ((int) $g['stock'] <= 0) {
                throw new ConflictoException('Este regalo está agotado.');
            }
            $saldo = $this->saldo($u['id']);
            if ($saldo < (int) $g['coste']) {
                throw new ConflictoException('Te faltan ' . ((int) $g['coste'] - $saldo) . ' puntos para este regalo.');
            }
            do {
                $codigo = self::codigoAleatorio('CJ-');
            } while ($this->fila("SELECT codigo FROM {$this->p}canje WHERE codigo = ?", [$codigo]));
            $caduca = self::sumarDias(self::diaLocal($ahora), self::CANJE_DIAS);
            $this->ejecutar("UPDATE {$this->p}regalo SET stock = stock - 1 WHERE id = ?", [$regaloId]);
            $this->ejecutar("INSERT INTO {$this->p}canje (codigo, usuario_id, regalo_id, coste, creado, caduca, estado)
                VALUES (?, ?, ?, ?, ?, ?, 'pendiente')", [$codigo, $u['id'], $regaloId, (int) $g['coste'], self::fechaBD($ahora), $caduca]);
            return ['codigo' => $codigo, 'regalo' => $g['nombre'], 'fecha' => self::diaLocal($ahora), 'caduca' => $caduca,
                'estado' => 'pendiente', 'coste' => (int) $g['coste']];
        });
    }

    // ---------- panel: catálogo (solo administración) ----------

    private function validarRegalo(string $nombre, $coste, $stock, ?string $id): array {
        $n = trim($nombre);
        $c = trim((string) $coste);
        $s = trim((string) $stock);
        $e = [];
        if (mb_strlen($n) < 3) {
            $e['nombre'] = 'Escribe un nombre de al menos 3 caracteres.';
        } elseif ($this->fila("SELECT id FROM {$this->p}regalo WHERE LOWER(nombre) = ? AND activo = 1 AND id <> ?", [mb_strtolower($n), $id ?? ''])) {
            $e['nombre'] = 'Ya hay un regalo con ese nombre.';
        }
        if (!preg_match('/^\d+$/', $c) || (int) $c < 1 || (int) $c > 5000) {
            $e['coste'] = 'Entre 1 y 5000 puntos.';
        }
        if (!preg_match('/^\d+$/', $s) || (int) $s > 999) {
            $e['stock'] = 'Entre 0 y 999 unidades.';
        }
        if ($e) {
            throw new ErroresDeCamposException($e);
        }
        return [$n, (int) $c, (int) $s];
    }

    public function crearRegalo(?array $quien, string $nombre, $coste, $stock): array {
        self::exigirRol($quien, ['admin']);
        [$n, $c, $s] = $this->validarRegalo($nombre, $coste, $stock, null);
        do {
            $id = 'g' . random_int(100000, 999999);
        } while ($this->fila("SELECT id FROM {$this->p}regalo WHERE id = ?", [$id]));
        $this->ejecutar("INSERT INTO {$this->p}regalo (id, nombre, coste, stock, activo) VALUES (?, ?, ?, ?, 1)", [$id, $n, $c, $s]);
        return self::regaloJSON($this->fila("SELECT * FROM {$this->p}regalo WHERE id = ?", [$id]));
    }

    public function editarRegalo(?array $quien, string $id, string $nombre, $coste, $stock): array {
        self::exigirRol($quien, ['admin']);
        if (!$this->fila("SELECT id FROM {$this->p}regalo WHERE id = ? AND activo = 1", [$id])) {
            throw new NoEncontradoException('No existe ese regalo.');
        }
        [$n, $c, $s] = $this->validarRegalo($nombre, $coste, $stock, $id);
        $this->ejecutar("UPDATE {$this->p}regalo SET nombre = ?, coste = ?, stock = ? WHERE id = ?", [$n, $c, $s, $id]);
        return self::regaloJSON($this->fila("SELECT * FROM {$this->p}regalo WHERE id = ?", [$id]));
    }

    // Retirar: deja de verse en la app; los canjes pendientes se siguen entregando
    public function retirarRegalo(?array $quien, string $id): void {
        self::exigirRol($quien, ['admin']);
        $this->ejecutar("UPDATE {$this->p}regalo SET activo = 0 WHERE id = ?", [$id]);
    }

    // ---------- panel: canjes ----------

    private function canjePanel(array $c): array {
        return ['codigo' => $c['codigo'], 'usuario' => $c['usuario_id'], 'regalo_id' => $c['regalo_id'],
            'regalo' => $c['nombre'], 'caduca' => $c['caduca'], 'estado' => $c['estado'],
            'creado' => self::isoDeBD($c['creado'])];
    }

    public function canjes(?array $quien): array {
        self::exigirRol($quien, ['admin']);
        $this->caducarCanjes();
        return array_map([$this, 'canjePanel'], $this->filas("SELECT c.*, r.nombre FROM {$this->p}canje c
            JOIN {$this->p}regalo r ON r.id = c.regalo_id ORDER BY c.creado DESC LIMIT 200"));
    }

    // --------------------------------------------------------------
    // codigo --> comprobarCanje() --> canje (pendiente y en plazo)
    // --------------------------------------------------------------
    public function comprobarCanje(?array $quien, string $codigo): array {
        self::exigirRol($quien, ['admin', 'reparto']);
        $this->caducarCanjes();
        $c = strtoupper(trim($codigo));
        if (!preg_match('/^CJ-[A-Z0-9]{4}$/', $c)) {
            throw new DatosNoValidosException('Formato no válido: CJ- y 4 letras o números.', 'codigo');
        }
        $x = $this->fila("SELECT c.*, r.nombre FROM {$this->p}canje c JOIN {$this->p}regalo r ON r.id = c.regalo_id WHERE c.codigo = ?", [$c]);
        if (!$x) {
            throw new NoEncontradoException("No existe ningún canje $c.");
        }
        if ($x['estado'] === 'entregado') {
            throw new ConflictoException("El canje $c ya se entregó.", 'codigo');
        }
        if ($x['estado'] === 'caducado') {
            $f = date('d/m', strtotime($x['caduca']));
            throw new ConflictoException("El canje $c caducó el $f: los puntos ya se devolvieron al usuario.", 'codigo');
        }
        return $this->canjePanel($x);
    }

    public function entregarCanje(?array $quien, string $codigo): array {
        $x = $this->comprobarCanje($quien, $codigo);
        $this->ejecutar("UPDATE {$this->p}canje SET estado = 'entregado' WHERE codigo = ?", [$x['codigo']]);
        $x['estado'] = 'entregado';
        $x['stock'] = (int) $this->valor("SELECT stock FROM {$this->p}regalo WHERE id = ?", [$x['regalo_id']]);
        return $x;
    }

    // ==============================================================
    // Revisión de días sospechosos (administración)
    // ==============================================================

    public function revisiones(?array $quien): array {
        self::exigirRol($quien, ['admin']);
        $this->cerrarDias(null);
        $out = [];
        foreach ($this->filas("SELECT * FROM {$this->p}revision
            ORDER BY CASE WHEN estado = 'pend' THEN 0 ELSE 1 END, fecha DESC LIMIT 100") as $r) {
            $c = $this->calcularDia($r['usuario_id'], $r['fecha']);
            $out[] = ['id' => (int) $r['id'], 'usuario' => $r['usuario_id'], 'fecha' => $r['fecha'],
                'minutos' => $c['minutos'], 'puntos' => $c['puntos'], 'motivo' => $r['motivo'],
                'estado' => $r['estado'], 'razon' => $r['estado'] === 'anulado' ? $r['razon'] : null];
        }
        return $out;
    }

    // decision: 'aprobar' | 'anular' (con razón de 10+ caracteres)
    public function revisar(?array $quien, int $id, string $decision, string $razon = ''): array {
        self::exigirRol($quien, ['admin']);
        $r = $this->fila("SELECT * FROM {$this->p}revision WHERE id = ?", [$id]);
        if (!$r) {
            throw new NoEncontradoException('No existe ese día.');
        }
        if ($r['estado'] !== 'pend') {
            throw new ConflictoException('Este día ya está revisado.');
        }
        if ($decision === 'anular') {
            $t = trim($razon);
            if (mb_strlen($t) < 10) {
                throw new DatosNoValidosException('Explica el motivo con al menos 10 caracteres: el voluntario lo verá.', 'razon');
            }
            $this->ejecutar("UPDATE {$this->p}revision SET estado = 'anulado', razon = ? WHERE id = ?", [mb_substr($t, 0, 200), $id]);
        } elseif ($decision === 'aprobar') {
            $this->ejecutar("UPDATE {$this->p}revision SET estado = 'ok', razon = 'Aprobado' WHERE id = ?", [$id]);
        } else {
            throw new DatosNoValidosException('decision: aprobar o anular');
        }
        foreach ($this->revisiones($quien) as $x) {
            if ($x['id'] === $id) {
                return $x;
            }
        }
        throw new NoEncontradoException('No existe ese día.');
    }
}
