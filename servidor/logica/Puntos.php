<?php
// ==============================================================
// Fichero:     Puntos.php
// Descripción: Puntos del voluntariado, regalos y canjes.
//
//   - Los puntos los calcula el SERVIDOR con las mediciones que llevan
//     el ID de la persona como portador (no los manda el móvil).
//   - Las reglas están aquí, en el código (REGLAS): 1 punto cada 5 min
//     válidos, los minutos en parques valen x2 y como mucho 100 al día.
//   - Los puntos de cada día acabado se guardan en «puntos_dia» (una fila
//     por usuario y día). Se calculan la primera vez que la persona pide
//     su cuenta después de que acabe el día (no hace falta una tarea
//     programada). Hoy no se guarda: se calcula en directo. Si llegan
//     mediciones atrasadas de un día guardado, Logica borra su fila y se
//     vuelve a calcular.
//   - Saldo = suma de «puntos_dia» − canjes no caducados.
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
// # tipos: Base.php (Valor, Registro, Ausente, Error y capacidades lógicas).
// ReglasPuntos = { min: N, mult: N, max: N }.

require_once __DIR__ . '/Base.php';

// Base <--- Puntos
class Puntos extends Base {

    const HUECO_MAX_MIN = 15;
    const VELOCIDAD_MAX_KMH = 25;
    const PARADO_M = 20;
    const PARADO_MAX_MIN = 15;
    const CANJE_DIAS = 7;       // días para recoger un regalo

    // ==============================================================
    // Reglas (fijas, en el código)
    // ==============================================================

    const REGLAS = ['min' => 5, 'mult' => 2, 'max' => 100];   // min por punto, x parques, máximo al día

    // --------------------------------------------------------------
    // + quien: Registro | Ausente
    //     -->
    //         reglas() --x
    //     <--
    //     ReglasPuntos | Error
    // --------------------------------------------------------------
    public function reglas(?array $quien): array {
        self::exigirRol($quien, ['admin']);
        return self::REGLAS;
    }

    // --------------------------------------------------------------
    // + min: Z, minParque: N, r: Registro
    //     -->
    //         puntosDe() --x
    //     <--
    //     N
    // --------------------------------------------------------------
    public static function puntosDe(int $min, int $minParque, array $r = self::REGLAS): int {
        $p = intdiv($min, $r['min']) + intdiv($minParque, $r['min']) * ($r['mult'] - 1);
        return max(0, min($r['max'], $p));
    }

    // distancia en metros entre dos puntos (haversine)
    // --------------------------------------------------------------
    // + lat1: R, lng1: R, lat2: R, lng2: R
    //     -->
    //         metros() --x
    //     <--
    //     R
    // --------------------------------------------------------------
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
    // # Estadísticas del recorrido: inicio y fin = primera y última medición del día;
    // # distancia = lo andado en los tramos válidos (sin huecos ni vehículo).
    // # effects: consulta la base de datos.
    // + usuarioId: Text, dia: Date, hastaMs: Instante | Ausente
    //     -->
    //         calcularDia() <--
    //     <--
    //     Registro | Error
    // --------------------------------------------------------------
    public function calcularDia(string $usuarioId, string $dia, ?int $hastaMs = null): array {
        $ini = self::inicioDia($dia);
        $fin = $hastaMs ?? self::inicioDia(self::sumarDias($dia, 1));
        // Un punto por anuncio (las 3 magnitudes de un mismo instante llegan juntas)
        $filas = $this->filas("SELECT m.fecha_medicion AS medida_en, m.lat, m.lng, z.tipo AS tipo_zona, z.nombre AS nombre_zona
            FROM {$this->p}medicion m JOIN {$this->p}zona z ON z.id = m.zona_id
            WHERE m.portador_id = ? AND m.fecha_medicion >= ? AND m.fecha_medicion < ?
            ORDER BY m.fecha_medicion", [$usuarioId, self::fechaBD($ini), self::fechaBD($fin)]);
        $puntos = [];
        $parques = [];
        foreach ($filas as $f) {
            if ($f['tipo_zona'] === 'parque') {
                $parques[$f['nombre_zona']] = true;
            }
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
        $valido = 0.0; $parque = 0.0; $desc = 0.0; $dist = 0.0;
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
                if (!$descartando) {
                    $dist += $d;          // tramo andado (si está parado, apenas suma)
                }
                if (self::metros($ancla['lat'], $ancla['lng'], $b['lat'], $b['lng']) < self::PARADO_M) {
                    $rato += $dt;
                    if ($descartando) {
                        $desc += $dt;
                        continue;
                    }
                    if ($rato > self::PARADO_MAX_MIN) {   // todo el rato parado deja de contar
                        $valido -= ($rato - $dt);
                        $parque -= $ratoParque;
                        $desc += $rato;
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
        $pts = self::puntosDe($min, $minParque);
        $iniMs = $puntos ? $puntos[0]['t'] : null;
        $finMs = $puntos ? end($puntos)['t'] : null;
        return ['minutos' => $min, 'minutos_parque' => $minParque, 'descartados' => (int) round($desc),
            'puntos' => $pts,
            'inicio' => $iniMs === null ? null : self::fechaISO($iniMs), 'fin' => $finMs === null ? null : self::fechaISO($finMs),
            'duracion_min' => $iniMs === null ? 0 : (int) round(($finMs - $iniMs) / 60000),
            'distancia_m' => (int) round($dist), 'lecturas' => count($puntos), 'parques' => array_keys($parques)];
    }

    // --------------------------------------------------------------
    // - c: Registro
    //     -->
    //         recorridoJSON() --x
    //     <--
    //     Registro
    // --------------------------------------------------------------
    private static function recorridoJSON(array $c): array {
        return ['inicio' => $c['inicio'], 'fin' => $c['fin'], 'duracion_min' => $c['duracion_min'],
            'distancia_m' => $c['distancia_m'], 'lecturas' => $c['lecturas'], 'parques' => $c['parques']];
    }

    // --------------------------------------------------------------
    // # Guarda en «puntos_dia» los días ya acabados en los que llevó un
    // # sensor y que aún no estén guardados.
    // # effects: consulta y modifica la base de datos; consulta el reloj inyectado.
    // + usuarioId: Text
    //     -->
    //         cerrarDias() -->
    //     <--
    //     0 | Error
    // --------------------------------------------------------------
    public function cerrarDias(string $usuarioId): void {
        $hoy = self::diaLocal($this->ahora());
        $hechos = array_flip(array_column($this->filas("SELECT fecha FROM {$this->p}puntos_dia WHERE usuario_id = ?",
            [$usuarioId]), 'fecha'));
        $dias = [];
        // una fila por hora con datos (no una por medición) y se pasa a día en hora de España
        foreach ($this->filas("SELECT DISTINCT SUBSTR(fecha_medicion, 1, 13) AS h FROM {$this->p}medicion
            WHERE portador_id = ? AND fecha_medicion < ?", [$usuarioId, self::fechaBD(self::inicioDia($hoy))]) as $f) {
            $dias[self::diaLocal(self::msDeFechaBD($f['h'] . ':00:00'))] = true;
        }
        foreach (array_keys($dias) as $d) {
            if (isset($hechos[(string) $d])) {
                continue;
            }
            $c = $this->calcularDia($usuarioId, (string) $d);
            try {
                $this->ejecutar("INSERT INTO {$this->p}puntos_dia (usuario_id, fecha, minutos, minutos_parque, descartados, puntos)
                    VALUES (?, ?, ?, ?, ?, ?)", [$usuarioId, (string) $d, $c['minutos'], $c['minutos_parque'], $c['descartados'], $c['puntos']]);
            } catch (PDOException $e) {
                // otra petición a la vez ya lo guardó (misma clave): no pasa nada
                if ($e->getCode() !== '23000') {
                    throw $e;
                }
            }
        }
    }

    // --------------------------------------------------------------
    // # Solo los días con algo que enseñar (minutos válidos o descartados).
    // # effects: consulta y modifica la base de datos.
    // + usuarioId: Text, max: N
    //     -->
    //         dias() -->
    //     <--
    //     [Registro] | Error
    // --------------------------------------------------------------
    public function dias(string $usuarioId, int $max = 60): array {
        $this->cerrarDias($usuarioId);
        return $this->filas("SELECT fecha, minutos, minutos_parque, descartados, puntos FROM {$this->p}puntos_dia
            WHERE usuario_id = ? AND (minutos > 0 OR descartados > 0) ORDER BY fecha DESC LIMIT " . (int) $max, [$usuarioId]);
    }

    // Canjes no recogidos a tiempo: caducan, y la unidad vuelve al stock
    // --------------------------------------------------------------
    // # Los canjes vencidos devuelven puntos y una unidad al stock.
    // # effects: consulta y modifica la base de datos; consulta el reloj inyectado.
    // +       caducarCanjes() -->
    //     <--
    //     0 | Error
    // --------------------------------------------------------------
    public function caducarCanjes(): void {
        $hoy = self::diaLocal($this->ahora());
        $this->transaccion(function () use ($hoy) {
            foreach ($this->filas("SELECT codigo, regalo_id FROM {$this->p}canje WHERE estado = 'pendiente' AND caduca < ?", [$hoy]) as $c) {
                $this->ejecutar("UPDATE {$this->p}canje SET estado = 'caducado' WHERE codigo = ?", [$c['codigo']]);
                $this->ejecutar("UPDATE {$this->p}regalo SET stock = stock + 1 WHERE id = ?", [$c['regalo_id']]);
            }
        });
    }

    // --------------------------------------------------------------
    // # effects: consulta y modifica la base de datos.
    // + usuarioId: Text
    //     -->
    //         saldo() -->
    //     <--
    //     Z | Error
    // --------------------------------------------------------------
    public function saldo(string $usuarioId): int {
        $this->cerrarDias($usuarioId);
        $mas = (int) $this->valor("SELECT COALESCE(SUM(puntos), 0) FROM {$this->p}puntos_dia WHERE usuario_id = ?", [$usuarioId]);
        $menos = (int) $this->valor("SELECT COALESCE(SUM(coste), 0) FROM {$this->p}canje WHERE usuario_id = ? AND estado <> 'caducado'", [$usuarioId]);
        return $mas - $menos;
    }

    // --------------------------------------------------------------
    // # effects: consulta y modifica la base de datos; consulta el reloj inyectado.
    // + u: Registro
    //     -->
    //         cuenta() -->
    //     <--
    //     Registro | Error
    // --------------------------------------------------------------
    public function cuenta(array $u): array {
        $this->caducarCanjes();
        $ahora = $this->ahora();
        $hoy = self::diaLocal($ahora);
        $h = $this->calcularDia($u['id'], $hoy, $ahora);
        $mov = [];
        foreach ($this->dias($u['id']) as $d) {
            // el recorrido (distancia, horas, parques) no se guarda: sale de las mediciones de ese día
            $mov[] = ['tipo' => 'dia', 'fecha' => $d['fecha'], 'minutos' => (int) $d['minutos'],
                'minutos_parque' => (int) $d['minutos_parque'], 'descartados' => (int) $d['descartados'],
                'puntos' => (int) $d['puntos'], 'razon' => null, 'canje' => null,
                'recorrido' => self::recorridoJSON($this->calcularDia($u['id'], $d['fecha']))];
        }
        $canjes = [];
        foreach ($this->filas("SELECT c.*, r.nombre FROM {$this->p}canje c JOIN {$this->p}regalo r ON r.id = c.regalo_id
            WHERE c.usuario_id = ? ORDER BY c.creado DESC", [$u['id']]) as $c) {
            $canjes[] = ['codigo' => $c['codigo'], 'regalo' => $c['nombre'], 'fecha' => self::diaLocal(self::msDeFechaBD($c['creado'])),
                'caduca' => $c['caduca'], 'estado' => $c['estado'], 'coste' => (int) $c['coste']];
            $mov[] = ['tipo' => 'canje', 'fecha' => self::diaLocal(self::msDeFechaBD($c['creado'])), 'minutos' => 0,
                'minutos_parque' => 0, 'descartados' => 0, 'puntos' => $c['estado'] === 'caducado' ? 0 : -(int) $c['coste'],
                'razon' => $c['estado'] === 'caducado' ? 'Caducado sin recoger: puntos devueltos' : null,
                'canje' => $c['nombre']];
        }
        usort($mov, function ($a, $b) { return strcmp($b['fecha'], $a['fecha']); });
        return ['puntos' => $this->saldo($u['id']),
            'hoy' => ['minutos' => $h['minutos'], 'minutos_parque' => $h['minutos_parque'],
                'descartados' => $h['descartados'], 'puntos' => $h['puntos'], 'recorrido' => self::recorridoJSON($h)],
            'historial' => $mov, 'canjes' => $canjes, 'reglas' => self::REGLAS];
    }

    // ==============================================================
    // Regalos y canjes
    // ==============================================================

    // --------------------------------------------------------------
    // - r: Registro
    //     -->
    //         regaloJSON() --x
    //     <--
    //     Registro
    // --------------------------------------------------------------
    private static function regaloJSON(array $r): array {
        return ['id' => $r['id'], 'nombre' => $r['nombre'], 'coste' => (int) $r['coste'], 'stock' => (int) $r['stock']];
    }

    // catálogo que se ve en la app (y en el panel)
    // --------------------------------------------------------------
    // # effects: consulta y modifica la base de datos.
    // +       regalos() -->
    //     <--
    //     [Registro] | Error
    // --------------------------------------------------------------
    public function regalos(): array {
        $this->caducarCanjes();
        return array_map([self::class, 'regaloJSON'],
            $this->filas("SELECT * FROM {$this->p}regalo WHERE activo = 1 ORDER BY coste, nombre"));
    }

    // --------------------------------------------------------------
    // # Reserva stock y puntos en la misma transacción.
    // # effects: consulta y modifica la base de datos; consulta el reloj inyectado.
    // + u: Registro | Ausente, regaloId: Text
    //     -->
    //         canjear() -->
    //     <--
    //     Registro | Error
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

    // --------------------------------------------------------------
    // # effects: consulta la base de datos.
    // - nombre: Text, coste: Valor, stock: Valor, id: Text | Ausente
    //     -->
    //         validarRegalo() <--
    //     <--
    //     (nombre: Text, coste: N, stock: N) | Error
    // --------------------------------------------------------------
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

    // --------------------------------------------------------------
    // # effects: consulta y modifica la base de datos.
    // + quien: Registro | Ausente, nombre: Text, coste: Valor, stock: Valor
    //     -->
    //         crearRegalo() -->
    //     <--
    //     Registro | Error
    // --------------------------------------------------------------
    public function crearRegalo(?array $quien, string $nombre, $coste, $stock): array {
        self::exigirRol($quien, ['admin']);
        [$n, $c, $s] = $this->validarRegalo($nombre, $coste, $stock, null);
        do {
            $id = 'g' . random_int(100000, 999999);
        } while ($this->fila("SELECT id FROM {$this->p}regalo WHERE id = ?", [$id]));
        $this->ejecutar("INSERT INTO {$this->p}regalo (id, nombre, coste, stock, activo) VALUES (?, ?, ?, ?, 1)", [$id, $n, $c, $s]);
        return self::regaloJSON($this->fila("SELECT * FROM {$this->p}regalo WHERE id = ?", [$id]));
    }

    // --------------------------------------------------------------
    // # effects: consulta y modifica la base de datos.
    // + quien: Registro | Ausente, id: Text, nombre: Text, coste: Valor, stock: Valor
    //     -->
    //         editarRegalo() -->
    //     <--
    //     Registro | Error
    // --------------------------------------------------------------
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
    // --------------------------------------------------------------
    // # effects: consulta y modifica la base de datos.
    // + quien: Registro | Ausente, id: Text
    //     -->
    //         retirarRegalo() -->
    //     <--
    //     0 | Error
    // --------------------------------------------------------------
    public function retirarRegalo(?array $quien, string $id): void {
        self::exigirRol($quien, ['admin']);
        $this->ejecutar("UPDATE {$this->p}regalo SET activo = 0 WHERE id = ?", [$id]);
    }

    // ---------- panel: canjes ----------

    // --------------------------------------------------------------
    // - c: Registro
    //     -->
    //         canjePanel() --x
    //     <--
    //     Registro
    // --------------------------------------------------------------
    private function canjePanel(array $c): array {
        return ['codigo' => $c['codigo'], 'usuario' => $c['usuario_id'], 'regalo_id' => $c['regalo_id'],
            'regalo' => $c['nombre'], 'caduca' => $c['caduca'], 'estado' => $c['estado'],
            'creado' => self::isoDeBD($c['creado'])];
    }

    // --------------------------------------------------------------
    // # effects: consulta y modifica la base de datos.
    // + quien: Registro | Ausente
    //     -->
    //         canjes() -->
    //     <--
    //     [Registro] | Error
    // --------------------------------------------------------------
    public function canjes(?array $quien): array {
        self::exigirRol($quien, ['admin']);
        $this->caducarCanjes();
        return array_map([$this, 'canjePanel'], $this->filas("SELECT c.*, r.nombre FROM {$this->p}canje c
            JOIN {$this->p}regalo r ON r.id = c.regalo_id ORDER BY c.creado DESC LIMIT 200"));
    }

    // --------------------------------------------------------------
    // # effects: consulta y modifica la base de datos.
    // + quien: Registro | Ausente, codigo: Text
    //     -->
    //         comprobarCanje() -->
    //     <--
    //     Registro | Error
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

    // --------------------------------------------------------------
    // # effects: consulta y modifica la base de datos.
    // + quien: Registro | Ausente, codigo: Text
    //     -->
    //         entregarCanje() -->
    //     <--
    //     Registro | Error
    // --------------------------------------------------------------
    public function entregarCanje(?array $quien, string $codigo): array {
        $x = $this->comprobarCanje($quien, $codigo);
        $this->ejecutar("UPDATE {$this->p}canje SET estado = 'entregado' WHERE codigo = ?", [$x['codigo']]);
        $x['estado'] = 'entregado';
        $x['stock'] = (int) $this->valor("SELECT stock FROM {$this->p}regalo WHERE id = ?", [$x['regalo_id']]);
        return $x;
    }
}
