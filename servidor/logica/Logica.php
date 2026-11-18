<?php
// ==============================================================
// Fichero:     Logica.php
// Descripción: Lógica de las MEDICIONES: guardar las mediciones
//              que envía la app (validar, descartar repetidas, asignar
//              zona) y calcular lo que muestran la app y la web
//              (medias por zona, series para las gráficas, flota).
//              (Cuentas, puntos, comunidad y flota: en sus propios ficheros.)
// Diseño:      doc/servidor.md
// Autor:       Aaron Blasco Blay
// Fecha:       09/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================

require_once __DIR__ . '/Base.php';
require_once __DIR__ . '/Flota.php';

class Logica extends Base {

    // Magnitudes y rango que puede devolver cada sensor. Fuera de rango
    // el dato se guarda como NO válido (no cuenta en las medias).
    const MAGNITUDES = ['o3', 'temp', 'ruido'];
    const RANGO = [
        'o3'    => [0.0, 5.0],     // SPEC ULPSM-O3: 0 a 5 ppm
        'temp'  => [0.0, 50.0],    // DHT11: 0 a 50 °C
        'ruido' => [30.0, 130.0],  // KY-037 calibrado: dB aproximados
    ];
    const TIPO_BEACON = [11 => 'o3', 12 => 'temp', 13 => 'ruido'];

    // Dos mensajes con el mismo nodo, magnitud y contador en menos de
    // este tiempo son el MISMO anuncio (lo han oído varios móviles).
    const VENTANA_REPETIDA_MS = 10000;      // repetida: mismo sensor, tipo y valor en ±10 s
    const DISTANCIA_ESTIMACION_KM = 1.5;

    // Periodos de las gráficas (los mismos que la web y la app)
    const PERIODOS = [
        '24h' => ['n' => 48, 'paso' => 1800000],      // 30 min
        '7d'  => ['n' => 56, 'paso' => 10800000],     // 3 h
        '30d' => ['n' => 60, 'paso' => 43200000],     // 12 h
    ];

    // --------------------------------------------------------------
    // lat: R, lng: R, poligono: [[lat,lng]] --> dentro() --> B
    // (algoritmo del rayo: x = lng, y = lat)
    // --------------------------------------------------------------
    public static function dentro(float $lat, float $lng, array $poligono): bool {
        $dentro = false;
        $n = count($poligono);
        for ($i = 0, $j = $n - 1; $i < $n; $j = $i++) {
            $yi = $poligono[$i][0]; $xi = $poligono[$i][1];
            $yj = $poligono[$j][0]; $xj = $poligono[$j][1];
            if ((($yi > $lat) !== ($yj > $lat))
                && ($lng < ($xj - $xi) * ($lat - $yi) / ($yj - $yi) + $xi)) {
                $dentro = !$dentro;
            }
        }
        return $dentro;
    }

    // --------------------------------------------------------------
    // lat: R, lng: R --> zonaDe() --> Text | null
    // Un parque o una playa tiene prioridad si también cae en un barrio.
    // --------------------------------------------------------------
    public function zonaDe(?float $lat, ?float $lng): ?string {
        if ($lat === null || $lng === null) {
            return null;
        }
        $barrio = null;
        foreach ($this->leerZonas() as $z) {
            if (self::dentroDeZona($lat, $lng, $z['poligonos'])) {
                if ($z['tipo'] !== 'barrio') {
                    return $z['id'];
                }
                $barrio = $barrio ?? $z['id'];
            }
        }
        return $barrio;
    }

    private static function dentroDeZona(float $lat, float $lng, array $poligonos): bool {
        foreach ($poligonos as $anillos) {
            if (!self::dentro($lat, $lng, $anillos[0])) {
                continue;
            }
            foreach (array_slice($anillos, 1) as $hueco) {
                if (self::dentro($lat, $lng, $hueco)) {
                    continue 2;
                }
            }
            return true;
        }
        return false;
    }

    private $cacheZonas = null;

    private function leerZonas(): array {
        if ($this->cacheZonas === null) {
            $filas = $this->filas("SELECT id, ayuntamiento_id, nombre, tipo, area, etiqueta_lat, etiqueta_lng
                FROM {$this->p}zona ORDER BY nombre");
            foreach ($filas as &$fila) {
                $area = json_decode($fila['area'], true);
                // Las zonas previas guardan solo un anillo [lat, lng].
                $invertir = static fn(array $anillo): array => array_map(static fn($p) => [$p[1], $p[0]], $anillo);
                $geometria = $area['type'] ?? null;
                if ($geometria === null) {
                    $anillo = $invertir($area);
                    if ($anillo[0] !== $anillo[count($anillo) - 1]) {
                        $anillo[] = $anillo[0];
                    }
                    $area = ['type' => 'Polygon', 'coordinates' => [$anillo]];
                }
                $partes = $area['type'] === 'MultiPolygon' ? $area['coordinates'] : [$area['coordinates']];
                $fila['geometria'] = $area;
                $fila['poligonos'] = array_map(static fn($anillos) => array_map($invertir, $anillos), $partes);
                $fila['poligono'] = $fila['poligonos'][0][0];
                unset($fila['area']);
            }
            $this->cacheZonas = $filas;
        }
        return $this->cacheZonas;
    }

    // --------------------------------------------------------------
    // d: {nodo, magnitud | tipo, valor, medida_en?, lat, lng}, quien: usuario con sesión | null
    // (la app manda también contador, precision y rssi: se aceptan y no se guardan)
    // El «portador» (para los puntos) NO lo dice el móvil: es quien tiene
    // la sesión, y solo si de verdad lleva ese sensor (préstamo en curso
    // o sensor de su servicio). Si no, la medición se guarda sin portador.
    //   --> guardarMedicion()
    //   --> {id, nodo, magnitud, valor, zona, valida, motivo, medida_en}
    // Solo se guardan mediciones válidas: con ubicación, dentro de una zona
    // y dentro del rango del sensor. Si no, DatosNoValidosException (400).
    // NoEncontradoException (404) si el sensor no existe; null si es repetida.
    // --------------------------------------------------------------
    public function guardarMedicion(array $d, ?array $quien = null): ?array {
        $ahora = $this->ahora();

        $nodo = strtoupper(trim((string) ($d['nodo'] ?? '')));
        if (!preg_match('/^RSP-[A-Z0-9]{4}$/', $nodo)) {
            throw new DatosNoValidosException('nodo: tiene que ser RSP- y 4 letras o números');
        }
        $magnitud = $d['magnitud'] ?? (isset($d['tipo']) ? (self::TIPO_BEACON[(int) $d['tipo']] ?? null) : null);
        if (!in_array($magnitud, self::MAGNITUDES, true)) {
            throw new DatosNoValidosException('magnitud: tiene que ser o3, temp o ruido (o tipo 11, 12 o 13)');
        }
        if (!isset($d['valor']) || !is_numeric($d['valor']) || !is_finite((float) $d['valor'])) {
            throw new DatosNoValidosException('valor: tiene que ser un número');
        }
        $valor = (float) $d['valor'];
        [$min, $max] = self::RANGO[$magnitud];
        if ($valor < $min || $valor > $max) {
            throw new DatosNoValidosException(sprintf('valor: fuera del rango del sensor (%s a %s)', $min, $max));
        }

        $medidaEn = isset($d['medida_en']) ? $d['medida_en'] : $ahora;
        if (!is_numeric($medidaEn)) {
            throw new DatosNoValidosException('medida_en: tiene que ser la hora en milisegundos (epoch)');
        }
        $medidaEn = (int) $medidaEn;
        if ($medidaEn > $ahora + 300000) {
            throw new DatosNoValidosException('medida_en: está en el futuro (revisa la hora del móvil)');
        }
        if ($medidaEn < $ahora - 7 * self::DIA_MS) {
            throw new DatosNoValidosException('medida_en: tiene más de 7 días');
        }

        $lat = self::numeroOpcional($d, 'lat', -90, 90);
        $lng = self::numeroOpcional($d, 'lng', -180, 180);
        if ($lat === null || $lng === null) {
            throw new DatosNoValidosException('lat y lng: la medición necesita la ubicación del móvil');
        }
        $zona = $this->zonaDe($lat, $lng);
        if ($zona === null) {
            throw new DatosNoValidosException('ubicación: está fuera de los parques y barrios de Respira');
        }

        // La fila del nodo serializa los reenvíos concurrentes de un sensor.
        try {
            $id = $this->transaccion(function () use ($nodo, $magnitud, $valor, $medidaEn, $lat, $lng, $zona, $quien) {
                $bloqueo = $this->bd->getAttribute(PDO::ATTR_DRIVER_NAME) === 'mysql' ? ' FOR UPDATE' : '';
                $n = $this->fila("SELECT estado FROM {$this->p}nodo WHERE codigo = ?" . $bloqueo, [$nodo]);
                if ($n === null) {
                    throw new NoEncontradoException("No existe el sensor $nodo");
                }
                if ($n['estado'] === 'baja') {
                    throw new DatosNoValidosException("El sensor $nodo está dado de baja");
                }
                $repetida = $this->valor("SELECT id FROM {$this->p}medicion
                    WHERE id_nodo = ? AND tipo = ? AND valor = ? AND origen = 'real' AND fecha_medicion BETWEEN ? AND ? LIMIT 1",
                    [$nodo, $magnitud, $valor, self::fechaBD($medidaEn - self::VENTANA_REPETIDA_MS),
                        self::fechaBD($medidaEn + self::VENTANA_REPETIDA_MS)]);
                if ($repetida !== null) {
                    return null;
                }
                $portador = $this->portadorDe($quien, $nodo, $medidaEn);
                $this->ejecutar("INSERT INTO {$this->p}medicion
                    (id_nodo, tipo, valor, fecha_medicion, lat, lng, zona_id, portador_id, origen)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'real')",
                    [$nodo, $magnitud, $valor, self::fechaBD($medidaEn), $lat, $lng, $zona, $portador]);
                $id = (int) $this->bd->lastInsertId();
                $dia = self::diaLocal($medidaEn);
                if ($portador !== null && $dia < self::diaLocal($this->ahora())) {
                    $this->ejecutar("DELETE FROM {$this->p}puntos_dia WHERE usuario_id = ? AND fecha = ?", [$portador, $dia]);
                }
                return $id;
            });
        } catch (PDOException $e) {
            throw new ErrorDeAlmacenamientoException('No se pudo guardar la medición');
        }
        if ($id === null) {
            return null;
        }
        return ['id' => $id, 'nodo' => $nodo, 'magnitud' => $magnitud, 'valor' => $valor, 'zona' => $zona,
            'valida' => true, 'motivo' => null, 'medida_en' => self::fechaISO($medidaEn)];
    }

    // --------------------------------------------------------------
    // La fecha de la medición permite sincronizar lotes de un préstamo aún abierto
    // cuyo plazo venció después de captar los datos.
    private function portadorDe(?array $quien, string $nodo, int $medidaEn): ?string {
        if ($quien === null) {
            return null;
        }
        if ($quien['rol'] === 'vecino') {
            $fecha = self::fechaBD($medidaEn);
            $ok = $this->valor("SELECT id FROM {$this->p}prestamo
                WHERE nodo = ? AND usuario_id = ? AND estado_devolucion = 'No devuelto'
                AND inicio <= ? AND fecha_limite >= ?", [$nodo, $quien['id'], $fecha, $fecha]);
            return $ok !== null ? $quien['id'] : null;
        }
        if ($quien['rol'] === 'trabajador' && (new Flota($this->bd, $this->p, $this->reloj))->sensorDeSuServicio($quien['id'], $nodo)) {
            return $quien['id'];
        }
        return null;
    }

    private static function numeroOpcional(array $d, string $k, float $min, float $max): ?float {
        if (!isset($d[$k]) || $d[$k] === '') {
            return null;
        }
        if (!is_numeric($d[$k]) || $d[$k] < $min || $d[$k] > $max) {
            throw new DatosNoValidosException("$k: tiene que ser un número entre $min y $max");
        }
        return (float) $d[$k];
    }

    // --------------------------------------------------------------
    // lista: [d] --> guardarMediciones() --> {guardadas, repetidas, errores: [{i, error}]}
    // (la app manda varias de golpe cuando recupera la conexión)
    // --------------------------------------------------------------
    public function guardarMediciones(array $lista, ?array $quien = null): array {
        $r = ['guardadas' => 0, 'repetidas' => 0, 'errores' => []];
        foreach (array_values($lista) as $i => $d) {
            try {
                if (!is_array($d)) {
                    throw new DatosNoValidosException('cada medición tiene que ser un objeto JSON');
                }
                $this->guardarMedicion($d, $quien) === null ? $r['repetidas']++ : $r['guardadas']++;
            } catch (DatosNoValidosException | NoEncontradoException $e) {
                $r['errores'][] = ['i' => $i, 'error' => $e->getMessage()];
            }
        }
        return $r;
    }

    // --------------------------------------------------------------
    // Última hora móvil. Para un dato antiguo se indica la media de la última
    // hora que sí tuvo lecturas de esa magnitud y se publica su fecha.
    public function zonas(): array {
        $ahora = $this->ahora();
        $hasta = self::fechaBD($ahora);
        $ultimaHora = self::fechaBD($ahora - self::HORA_MS);
        $ultimas24h = self::fechaBD($ahora - self::DIA_MS);
        $stDia = $this->bd->prepare("SELECT tipo, origen, MAX(fecha_medicion) AS ultima, AVG(valor) AS media
            FROM {$this->p}medicion WHERE zona_id = ? AND fecha_medicion >= ? AND fecha_medicion <= ?
            GROUP BY tipo, origen");
        $stHora = $this->bd->prepare("SELECT AVG(valor) AS media, MIN(valor) AS minimo, COUNT(*) AS n
            FROM {$this->p}medicion WHERE zona_id = ? AND tipo = ? AND origen = ?
            AND fecha_medicion >= ? AND fecha_medicion <= ?");
        $out = [];
        foreach ($this->leerZonas() as $zona) {
            $fila = [
                'id' => $zona['id'], 'ayuntamiento' => $zona['ayuntamiento_id'],
                'nombre' => $zona['nombre'], 'tipo' => $zona['tipo'],
                'poligono' => $zona['poligono'], 'poligonos' => $zona['poligonos'], 'geometria' => $zona['geometria'],
                'etiqueta' => [(float) $zona['etiqueta_lat'], (float) $zona['etiqueta_lng']],
                'hace_min' => null, 'n' => 0, 'simulado' => false,
            ];
            $stDia->execute([$zona['id'], $ultimas24h, $hasta]);
            $porOrigen = [];
            foreach ($stDia->fetchAll(PDO::FETCH_ASSOC) as $dato) {
                $porOrigen[$dato['tipo']][$dato['origen']] = $dato;
            }
            $origenes = [];
            foreach (self::MAGNITUDES as $magnitud) {
                $fila[$magnitud] = $fila['mejor'][$magnitud] = $fila['media_24h'][$magnitud] = null;
                $detalle = ['valor' => null, 'estado' => 'sin_datos', 'momento' => null, 'origen' => null];
                $dato = $porOrigen[$magnitud]['real'] ?? $porOrigen[$magnitud]['simulado'] ?? null;
                if ($dato !== null) {
                    $momento = self::msDeFechaBD($dato['ultima']);
                    // La lectura justo en el corte queda en la última hora.
                    $reciente = $momento >= $ahora - self::HORA_MS;
                    [$desde, $fin] = $reciente
                        ? [$ultimaHora, $hasta]
                        : [self::fechaBD($momento - self::HORA_MS), $dato['ultima']];
                    $stHora->execute([$zona['id'], $magnitud, $dato['origen'], $desde, $fin]);
                    $hora = $stHora->fetch(PDO::FETCH_ASSOC);
                    $stHora->closeCursor();
                    $dec = $magnitud === 'o3' ? 3 : 1;
                    $valor = round((float) $hora['media'], $dec);
                    $fila[$magnitud] = $valor;
                    $fila['mejor'][$magnitud] = $reciente ? round((float) $hora['minimo'], $dec) : null;
                    $fila['media_24h'][$magnitud] = round((float) $dato['media'], $dec);
                    $fila['hace_min'] = min($fila['hace_min'] ?? PHP_INT_MAX, (int) ceil(($ahora - $momento) / 60000));
                    $fila['n'] = max($fila['n'], (int) $hora['n']);
                    $origenes[$dato['origen']] = true;
                    $detalle = [
                        'valor' => $valor, 'estado' => $reciente ? 'reciente' : 'antiguo',
                        'momento' => self::fechaISO($momento), 'origen' => $dato['origen'],
                    ];
                }
                $fila['detalle'][$magnitud] = $detalle;
            }
            $fila['simulado'] = isset($origenes['simulado']) && !isset($origenes['real']);
            $out[] = $fila;
        }

        $observadas = $out;
        foreach ($out as &$zona) {
            foreach (self::MAGNITUDES as $magnitud) {
                if ($zona['detalle'][$magnitud]['estado'] === 'sin_datos') {
                    $aprox = self::estimarZona($zona, $magnitud, $observadas);
                    if ($aprox !== null) {
                        $zona['detalle'][$magnitud] = [
                            'valor' => $aprox, 'estado' => 'estimado', 'momento' => null, 'origen' => 'estimado',
                        ];
                    }
                }
            }
        }
        return $out;
    }

    // Ponderación por distancia inversa. Dos o más zonas vecinas del mismo
    // ayuntamiento a menos de 1,5 km, solo con lecturas recientes del mismo
    // origen. Se prefieren las reales. Una estimación no alimenta a otra.
    private static function estimarZona(array $zona, string $magnitud, array $observadas): ?float {
        [$latitud, $longitud] = $zona['etiqueta'];
        $grupos = ['real' => [], 'simulado' => []];
        foreach ($observadas as $vecina) {
            $dato = $vecina['detalle'][$magnitud];
            if ($vecina['id'] === $zona['id'] || $vecina['ayuntamiento'] !== $zona['ayuntamiento']
                || $dato['estado'] !== 'reciente') {
                continue;
            }
            [$latVecina, $lngVecina] = $vecina['etiqueta'];
            $y = deg2rad($latVecina - $latitud);
            $x = deg2rad($lngVecina - $longitud) * cos(deg2rad(($latitud + $latVecina) / 2));
            $km = 6371 * hypot($x, $y);
            if ($km <= self::DISTANCIA_ESTIMACION_KM) {
                $grupos[$dato['origen']][] = [$dato['valor'], 1 / max(0.01, $km)];
            }
        }
        foreach ($grupos as $vecinas) {
            if (count($vecinas) < 2) {
                continue;
            }
            $total = $peso = 0;
            foreach ($vecinas as [$valor, $factor]) {
                $total += $valor * $factor;
                $peso += $factor;
            }
            return round($total / $peso, $magnitud === 'o3' ? 3 : 1);
        }
        return null;
    }

    private static function fechaCivil(?string $fecha, string $campo): int {
        if ($fecha === null || !preg_match('/^(\d{4})-(\d{2})-(\d{2})$/D', $fecha, $partes)
            || !checkdate((int) $partes[2], (int) $partes[3], (int) $partes[1])) {
            throw new DatosNoValidosException('Indica una fecha válida en formato AAAA-MM-DD.', $campo);
        }
        return self::msDeFechaBD("$fecha 00:00:00");
    }

    private static function finDeSerie(?string $referencia, int $ahora): int {
        if ($referencia === null) {
            return $ahora;
        }
        if (!preg_match('/^(\d{4}-\d{2}-\d{2})T([01]\d|2[0-3]):([0-5]\d):([0-5]\d)(\.\d{3})?Z$/D', $referencia, $partes)) {
            throw new DatosNoValidosException('Usa una fecha UTC válida como referencia.', 'referencia');
        }
        self::fechaCivil($partes[1], 'referencia');
        $fin = self::msDeFechaBD(str_replace('T', ' ', substr($referencia, 0, -1)));
        if ($fin > $ahora + 60000) {
            throw new DatosNoValidosException('La referencia no puede ser futura.', 'referencia');
        }
        return intdiv($fin, 60000) * 60000;
    }

    // 24 h, 7 d o 30 d, o rango de fechas civiles UTC de hasta 90 días
    // inclusive. Una referencia común sincroniza dos ventanas predefinidas.
    public function serie(string $zonaId, string $magnitud, string $periodo, ?string $desde = null, ?string $hasta = null,
                          ?string $referencia = null): array {
        if (!in_array($zonaId, array_column($this->leerZonas(), 'id'), true)) {
            throw new NoEncontradoException("No existe la zona $zonaId");
        }
        if (!in_array($magnitud, self::MAGNITUDES, true)) {
            throw new DatosNoValidosException('magnitud: tiene que ser o3, temp o ruido');
        }
        $ahora = $this->ahora();
        if ($periodo === 'rango') {
            $inicio = self::fechaCivil($desde, 'desde');
            $ultimoDia = self::fechaCivil($hasta, 'hasta');
            if ($inicio > $ahora) {
                throw new DatosNoValidosException('La fecha inicial no puede ser futura.', 'desde');
            }
            if ($ultimoDia > $ahora || $ultimoDia < $inicio) {
                throw new DatosNoValidosException('La fecha final debe ser del mismo día o posterior sin superar hoy.', 'hasta');
            }
            if ($ultimoDia - $inicio >= 90 * self::DIA_MS) {
                throw new DatosNoValidosException('El rango máximo es de 90 días.', 'hasta');
            }
            $fin = min($ultimoDia + self::DIA_MS - 1, $ahora);
            $n = 60;
            $paso = max(1, (int) ceil(($fin - $inicio) / $n));
        } elseif (isset(self::PERIODOS[$periodo])) {
            ['n' => $n, 'paso' => $paso] = self::PERIODOS[$periodo];
            $fin = self::finDeSerie($referencia, $ahora);
            $inicio = $fin - $n * $paso;
        } else {
            throw new DatosNoValidosException('periodo: 24h, 7d, 30d o rango');
        }

        $st = $this->ejecutar("SELECT fecha_medicion AS medida_en, valor, origen FROM {$this->p}medicion
            WHERE zona_id = ? AND tipo = ? AND fecha_medicion > ? AND fecha_medicion <= ?
            ORDER BY fecha_medicion, id", [$zonaId, $magnitud, self::fechaBD($inicio), self::fechaBD(min($fin, $ahora))]);
        // En cada tramo prevalece el dato real y se conserva su origen en la respuesta.
        $acumulados = ['real' => [], 'simulado' => []];
        while ($fila = $st->fetch(PDO::FETCH_ASSOC)) {
            $momento = self::msDeFechaBD($fila['medida_en']);
            $i = min($n - 1, intdiv($momento - $inicio - 1, $paso));
            $valor = (float) $fila['valor'];
            $origen = $fila['origen'];
            $tramo = $acumulados[$origen][$i] ?? ['suma' => 0, 'n' => 0, 'min' => $valor, 'max' => $valor, 'momento' => $momento];
            $tramo['suma'] += $valor;
            $tramo['n']++;
            $tramo['min'] = min($tramo['min'], $valor);
            if ($valor > $tramo['max']) {
                $tramo['max'] = $valor;
                $tramo['momento'] = $momento;
            }
            $acumulados[$origen][$i] = $tramo;
        }

        $dec = $magnitud === 'o3' ? 3 : 1;
        $valores = $maximos = $momentos = $origenes = array_fill(0, $n, null);
        $minimo = $maximo = null;
        $suma = $cantidad = $sinDatos = 0;
        for ($i = 0; $i < $n; $i++) {
            $origen = isset($acumulados['real'][$i]) ? 'real' : 'simulado';
            $tramo = $acumulados[$origen][$i] ?? null;
            if ($tramo === null) {
                $sinDatos += max(0, min($fin, $inicio + ($i + 1) * $paso) - ($inicio + $i * $paso));
                continue;
            }
            $valores[$i] = round($tramo['suma'] / $tramo['n'], $dec);
            $maximos[$i] = round($tramo['max'], $dec);
            $momentos[$i] = $tramo['momento'];
            $origenes[$i] = $origen;
            $minimo = min($minimo ?? $tramo['min'], $tramo['min']);
            $maximo = max($maximo ?? $tramo['max'], $tramo['max']);
            $suma += $tramo['suma'];
            $cantidad += $tramo['n'];
        }
        $eventos = self::eventos($valores, $maximos, $momentos, $magnitud, $inicio, $paso, $origenes);
        $eventos['avisos'] = $this->avisosEmitidos($zonaId, $magnitud, $inicio, $fin);

        return [
            'zona' => $zonaId, 'magnitud' => $magnitud, 'periodo' => $periodo,
            'paso_min' => intdiv($paso, 60000), 'paso_ms' => $paso, 'desde' => self::fechaISO($inicio), 'hasta' => self::fechaISO($fin),
            'valores' => $valores, 'origen' => $origenes, 'aprox' => self::aproximar($valores, $paso, $dec),
            'resumen' => [
                'minimo' => $cantidad ? round($minimo, $dec) : null,
                'media' => $cantidad ? round($suma / $cantidad, $dec) : null,
                'maximo' => $cantidad ? round($maximo, $dec) : null,
                'horas_sin_datos' => $sinDatos / self::HORA_MS,
            ],
            'eventos' => $eventos,
        ];
    }

    private function avisosEmitidos(string $zona, string $magnitud, int $inicio, int $fin): array {
        try {
            $filas = $this->filas("SELECT DISTINCT momento, valor, origen FROM {$this->p}alerta_emitida
                WHERE zona_id = ? AND magnitud = ? AND momento > ? AND momento <= ?
                ORDER BY momento DESC, valor DESC LIMIT 30", [$zona, $magnitud, self::fechaBD($inicio), self::fechaBD($fin)]);
        } catch (PDOException $e) {
            if ($e->getCode() === '42S02' || ($e->errorInfo[1] === 1 && str_contains($e->getMessage(), 'no such table'))) {
                return [];
            }
            throw $e;
        }
        return array_map(static fn($fila) => [
            'momento' => self::isoDeBD($fila['momento']), 'valor' => (float) $fila['valor'],
            'nivel' => self::nivelDe($magnitud, (float) $fila['valor']), 'origen' => $fila['origen'],
        ], $filas);
    }

    // Límites de los niveles (los mismos que la app y la web): Medio y Alto
    const LIMITES = ['o3' => [0.060, 0.090], 'temp' => [25.0, 32.0], 'ruido' => [55.0, 65.0]];
    // Los huecos de más de dos horas permanecen en blanco aunque el tramo sea más largo.
    const HUECO_APROX_MS = 2 * 3600000;

    // --------------------------------------------------------------
    // valores: [R | null], paso: N (ms) --> aproximar() --> [R | null]
    // Rellena por interpolación lineal SOLO los huecos cortos que tienen
    // dato a los dos lados. Lo devuelto es aparte: no cuenta en mín./media/máx.
    // --------------------------------------------------------------
    public static function aproximar(array $valores, int $paso, int $dec): array {
        $n = count($valores);
        $out = array_fill(0, $n, null);
        $maxTramos = intdiv(self::HUECO_APROX_MS, $paso);
        $i = 0;
        while ($i < $n) {
            if ($valores[$i] !== null) {
                $i++;
                continue;
            }
            $j = $i;
            while ($j < $n && $valores[$j] === null) {
                $j++;
            }
            $largo = $j - $i;
            if ($i > 0 && $j < $n && $largo <= $maxTramos) {
                $a = $valores[$i - 1];
                $b = $valores[$j];
                for ($k = $i; $k < $j; $k++) {
                    $out[$k] = round($a + ($b - $a) * ($k - $i + 1) / ($largo + 1), $dec);
                }
            }
            $i = $j;
        }
        return $out;
    }

    public static function nivelDe(string $magnitud, float $valor): string {
        [$medio, $alto] = self::LIMITES[$magnitud];
        return $valor >= $alto ? 'alto' : ($valor >= $medio ? 'medio' : 'bajo');
    }

    // --------------------------------------------------------------
    // eventos() --> {alertas: [{desde, hasta, max, nivel}], picos: [{momento, valor}]}
    //   alertas: ratos seguidos en nivel Alto (y, si no hay, en Medio)
    //   picos: los 3 valores más altos medidos (en tramos no seguidos), con su hora
    // --------------------------------------------------------------
    public static function eventos(array $valores, array $maximos, array $momentos, string $magnitud, int $inicio, int $paso, array $origenes = []): array {
        [$medio, $alto] = self::LIMITES[$magnitud];
        $alertas = [];
        foreach (['alto' => $alto, 'medio' => $medio] as $nivel => $lim) {
            for ($i = 0; $i < count($valores); $i++) {
                if ($valores[$i] === null || $valores[$i] < $lim) {
                    continue;
                }
                $j = $i;
                $mx = $valores[$i];
                while ($j + 1 < count($valores) && $valores[$j + 1] !== null && $valores[$j + 1] >= $lim) {
                    $j++;
                    $mx = max($mx, $valores[$j]);
                }
                $fuentes = array_unique(array_slice($origenes, $i, $j - $i + 1));
                $alertas[] = ['desde' => self::fechaISO($inicio + $i * $paso), 'hasta' => self::fechaISO($inicio + ($j + 1) * $paso),
                    'max' => $mx, 'nivel' => $nivel, 'origen' => count($fuentes) > 1 ? 'mixto' : ($fuentes[0] ?? null)];
                $i = $j;
            }
            if ($alertas) {
                break;      // si hay ratos en Alto, solo esos
            }
        }
        $orden = [];
        foreach ($maximos as $i => $m) {
            if ($m !== null) {
                $orden[$i] = $m;
            }
        }
        arsort($orden);
        $picos = [];
        $usados = [];
        foreach ($orden as $i => $m) {
            if (isset($usados[$i - 1]) || isset($usados[$i + 1])) {
                continue;           // el mismo pico visto en el tramo de al lado
            }
            $usados[$i] = true;
            $picos[] = ['momento' => self::fechaISO($momentos[$i] ?? ($inicio + $i * $paso + intdiv($paso, 2))),
                'valor' => $m, 'nivel' => self::nivelDe($magnitud, $m), 'origen' => $origenes[$i] ?? null];
            if (count($picos) === 3) {
                break;
            }
        }
        return ['alertas' => array_slice($alertas, -10), 'picos' => $picos];
    }

    // --------------------------------------------------------------
    // nodos() --> [{codigo, tipo, magnitudes, estado, asignado_a,
    //              ultima_min | null, ultima: {o3, temp, ruido}}]
    // ultima = último valor válido de cada magnitud (para el panel y «Mi sensor»)
    // --------------------------------------------------------------
    public function nodos(?string $codigo = null): array {
        $ahora = $this->ahora();
        $sql = "SELECT codigo, tipo, magnitudes, estado, asignado_a FROM {$this->p}nodo";
        $st = $this->bd->prepare($sql . ($codigo ? " WHERE codigo = ? AND estado <> 'baja'" : " WHERE estado <> 'baja' ORDER BY codigo"));
        $st->execute($codigo ? [strtoupper($codigo)] : []);
        $filas = $st->fetchAll(PDO::FETCH_ASSOC);
        if ($codigo && !$filas) {
            throw new NoEncontradoException("No existe el sensor $codigo");
        }
        $stUlt = $this->bd->prepare("SELECT valor FROM {$this->p}medicion
            WHERE id_nodo = ? AND tipo = ? ORDER BY fecha_medicion DESC LIMIT 1");
        $out = [];
        foreach ($filas as $f) {
            $ultima = [];
            foreach (self::MAGNITUDES as $m) {
                $stUlt->execute([$f['codigo'], $m]);
                $u = $stUlt->fetch(PDO::FETCH_ASSOC);
                $ultima[$m] = $u ? (float) $u['valor'] : null;
            }
            $out[] = ['codigo' => $f['codigo'], 'tipo' => $f['tipo'],
                'magnitudes' => $f['magnitudes'] === '' ? [] : explode(',', $f['magnitudes']),
                'estado' => $f['estado'], 'asignado_a' => $f['asignado_a'] === 'No asignado' ? null : $f['asignado_a'],
                'ultima_min' => $this->ultimaMin($f['codigo']),
                'ultima' => $ultima];
        }
        return $out;
    }
}
