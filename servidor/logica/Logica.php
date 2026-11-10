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
    const VENTANA_REPETIDA_MS = 120000;
    // Una zona muestra la media de la hora anterior a su último dato;
    // si el último dato tiene más de 24 h, la zona queda «Sin datos».

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
    // Si el punto cae en un parque, cuenta para el parque (aunque esté
    // dentro de un barrio); si no, para el barrio.
    // --------------------------------------------------------------
    public function zonaDe(?float $lat, ?float $lng): ?string {
        if ($lat === null || $lng === null) {
            return null;
        }
        $barrio = null;
        foreach ($this->leerZonas() as $z) {
            if (self::dentro($lat, $lng, $z['poligono'])) {
                if ($z['tipo'] === 'parque') {
                    return $z['id'];
                }
                $barrio = $barrio ?? $z['id'];
            }
        }
        return $barrio;
    }

    private $cacheZonas = null;

    private function leerZonas(): array {
        if ($this->cacheZonas === null) {
            $filas = $this->bd->query("SELECT id, nombre, tipo, municipio, poligono, etiqueta_lat, etiqueta_lng
                FROM {$this->p}zona ORDER BY orden, id")->fetchAll(PDO::FETCH_ASSOC);
            foreach ($filas as &$f) {
                $f['poligono'] = json_decode($f['poligono'], true);
            }
            $this->cacheZonas = $filas;
        }
        return $this->cacheZonas;
    }

    // --------------------------------------------------------------
    // d: {nodo, magnitud | tipo, valor, contador, medida_en?, lat?, lng?,
    //     precision?, rssi?}, quien: usuario con sesión | null
    // El «portador» (para los puntos) NO lo dice el móvil: es quien tiene
    // la sesión, y solo si de verdad lleva ese sensor (préstamo en curso
    // o sensor de su servicio). Si no, la medición se guarda sin portador.
    //   --> guardarMedicion()
    //   --> {id, nodo, magnitud, valor, zona, valida, motivo, medida_en}
    // Lanza DatosNoValidosException (400), NoEncontradoException (404),
    // y devuelve null si es repetida.
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
        if (!isset($d['contador']) || filter_var($d['contador'], FILTER_VALIDATE_INT) === false
            || $d['contador'] < 0 || $d['contador'] > 255) {
            throw new DatosNoValidosException('contador: tiene que ser un entero de 0 a 255');
        }
        $contador = (int) $d['contador'];

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
        if (($lat === null) !== ($lng === null)) {
            throw new DatosNoValidosException('lat y lng van juntas: o las dos o ninguna');
        }
        $precision = self::numeroOpcional($d, 'precision', 0, 65535);
        $rssi = self::numeroOpcional($d, 'rssi', -127, 20);

        $st = $this->bd->prepare("SELECT estado, magnitudes FROM {$this->p}nodo WHERE codigo = ?");
        $st->execute([$nodo]);
        $n = $st->fetch(PDO::FETCH_ASSOC);
        if (!$n) {
            throw new NoEncontradoException("No existe el sensor $nodo");
        }
        if ($n['estado'] === 'baja') {
            throw new DatosNoValidosException("El sensor $nodo está dado de baja");
        }
        $portador = $this->portadorDe($quien, $nodo);

        // ¿Repetida? mismo nodo, magnitud y contador en menos de 2 minutos
        $st = $this->bd->prepare("SELECT COUNT(*) FROM {$this->p}medicion
            WHERE nodo = ? AND magnitud = ? AND contador = ? AND medida_en BETWEEN ? AND ?");
        $st->execute([$nodo, $magnitud, $contador,
            self::fechaBD($medidaEn - self::VENTANA_REPETIDA_MS), self::fechaBD($medidaEn + self::VENTANA_REPETIDA_MS)]);
        if ((int) $st->fetchColumn() > 0) {
            return null;
        }

        [$min, $max] = self::RANGO[$magnitud];
        $valida = $valor >= $min && $valor <= $max;
        $motivo = $valida ? null : sprintf('fuera del rango del sensor (%s a %s)', $min, $max);
        $zona = $this->zonaDe($lat, $lng);

        try {
            $st = $this->bd->prepare("INSERT INTO {$this->p}medicion
                (nodo, magnitud, valor, contador, medida_en, recibida_en, lat, lng, precision_m, zona_id,
                 portador_id, rssi, origen, valida, motivo_descarte)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'real', ?, ?)");
            $st->execute([$nodo, $magnitud, $valor, $contador, self::fechaBD($medidaEn), self::fechaBD($ahora),
                $lat, $lng, $precision === null ? null : (int) $precision, $zona, $portador,
                $rssi === null ? null : (int) $rssi, $valida ? 1 : 0, $motivo]);
            $id = (int) $this->bd->lastInsertId();
            $st = $this->bd->prepare("UPDATE {$this->p}nodo SET ultima_medicion = ?
                WHERE codigo = ? AND (ultima_medicion IS NULL OR ultima_medicion < ?)");
            $st->execute([self::fechaBD($medidaEn), $nodo, self::fechaBD($medidaEn)]);
        } catch (PDOException $e) {
            throw new ErrorDeAlmacenamientoException('No se pudo guardar la medición');
        }
        return ['id' => $id, 'nodo' => $nodo, 'magnitud' => $magnitud, 'valor' => $valor, 'zona' => $zona,
            'valida' => $valida, 'motivo' => $motivo, 'medida_en' => self::fechaISO($medidaEn)];
    }

    // --------------------------------------------------------------
    // quien: usuario | null, nodo: Text --> portadorDe() --> Text | null
    //   vecino: tiene ese sensor prestado ahora
    //   trabajador: el sensor es de un servicio (tipo trab) y está activo
    // --------------------------------------------------------------
    private function portadorDe(?array $quien, string $nodo): ?string {
        if ($quien === null) {
            return null;
        }
        if ($quien['rol'] === 'vecino') {
            $ok = $this->valor("SELECT COUNT(*) FROM {$this->p}prestamo WHERE nodo = ? AND usuario_id = ? AND fin IS NULL",
                [$nodo, $quien['id']]);
            return (int) $ok > 0 ? $quien['id'] : null;
        }
        if ($quien['rol'] === 'trabajador') {
            $ok = $this->valor("SELECT COUNT(*) FROM {$this->p}nodo WHERE codigo = ? AND tipo = 'trab' AND estado = 'activo'", [$nodo]);
            return (int) $ok > 0 ? $quien['id'] : null;
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
    // zonas() --> [{id, nombre, tipo, municipio, poligono, etiqueta,
    //              hace_min | null, n, o3, temp, ruido}]
    // Valores = media de la hora anterior al último dato de la zona.
    // --------------------------------------------------------------
    public function zonas(): array {
        $ahora = $this->ahora();
        $out = [];
        $stUlt = $this->bd->prepare("SELECT MAX(medida_en) FROM {$this->p}medicion
            WHERE zona_id = ? AND valida = 1 AND origen = ? AND medida_en >= ?");
        $stMed = $this->bd->prepare("SELECT magnitud, AVG(valor) AS media, COUNT(*) AS n FROM {$this->p}medicion
            WHERE zona_id = ? AND valida = 1 AND origen = ? AND medida_en BETWEEN ? AND ? GROUP BY magnitud");
        foreach ($this->leerZonas() as $z) {
            $fila = ['id' => $z['id'], 'nombre' => $z['nombre'], 'tipo' => $z['tipo'], 'municipio' => $z['municipio'],
                'poligono' => $z['poligono'], 'etiqueta' => [(float) $z['etiqueta_lat'], (float) $z['etiqueta_lng']],
                'hace_min' => null, 'n' => 0, 'o3' => null, 'temp' => null, 'ruido' => null,
                'media_24h' => ['o3' => null, 'temp' => null, 'ruido' => null], 'simulado' => false];
            // Si la zona tiene datos REALES en las últimas 24 h, se usan solo esos;
            // si no, los simulados (Simulador.php)
            foreach (['real', 'simulado'] as $origen) {
                $stUlt->execute([$z['id'], $origen, self::fechaBD($ahora - self::DIA_MS)]);
                $ult = $stUlt->fetchColumn();
                if (!$ult) {
                    continue;
                }
                $ultMs = self::msDeFechaBD($ult);
                $fila['hace_min'] = max(0, intdiv($ahora - $ultMs, 60000));
                $fila['simulado'] = $origen === 'simulado';
                $stMed->execute([$z['id'], $origen, self::fechaBD($ultMs - self::HORA_MS), self::fechaBD($ultMs)]);
                foreach ($stMed->fetchAll(PDO::FETCH_ASSOC) as $m) {
                    $fila[$m['magnitud']] = round((float) $m['media'], $m['magnitud'] === 'o3' ? 3 : 1);
                    $fila['n'] = max($fila['n'], (int) $m['n']);
                }
                // promedio de las últimas 24 h (para la ficha de la zona)
                $stMed->execute([$z['id'], $origen, self::fechaBD($ahora - self::DIA_MS), self::fechaBD($ahora)]);
                foreach ($stMed->fetchAll(PDO::FETCH_ASSOC) as $m) {
                    $fila['media_24h'][$m['magnitud']] = round((float) $m['media'], $m['magnitud'] === 'o3' ? 3 : 1);
                }
                break;
            }
            $out[] = $fila;
        }
        return $out;
    }

    // --------------------------------------------------------------
    // zonaId: Text, magnitud: Text, periodo: Text, desde?: Text, hasta?: Text
    //   --> serie() --> {zona, magnitud, periodo, paso_min, desde, hasta, valores: [R | null]}
    // periodo '24h' | '7d' | '30d', o 'rango' con desde/hasta (AAAA-MM-DD, 60 tramos)
    // --------------------------------------------------------------
    public function serie(string $zonaId, string $magnitud, string $periodo, ?string $desde = null, ?string $hasta = null): array {
        $existe = false;
        foreach ($this->leerZonas() as $z) {
            $existe = $existe || $z['id'] === $zonaId;
        }
        if (!$existe) {
            throw new NoEncontradoException("No existe la zona $zonaId");
        }
        if (!in_array($magnitud, self::MAGNITUDES, true)) {
            throw new DatosNoValidosException('magnitud: tiene que ser o3, temp o ruido');
        }
        $ahora = $this->ahora();
        if ($periodo === 'rango') {
            $d = $desde ? strtotime($desde . ' 00:00:00 UTC') : false;
            $h = $hasta ? strtotime($hasta . ' 23:59:59 UTC') : false;
            if ($d === false || $h === false || $h <= $d) {
                throw new DatosNoValidosException('desde y hasta: fechas AAAA-MM-DD, con hasta posterior a desde');
            }
            if ($h - $d > 90 * 86400) {
                throw new DatosNoValidosException('el rango máximo es de 90 días');
            }
            $n = 60;
            $fin = min($h * 1000 + 999, $ahora);
            $paso = (int) ceil(($fin - $d * 1000) / $n);
        } elseif (isset(self::PERIODOS[$periodo])) {
            $n = self::PERIODOS[$periodo]['n'];
            $paso = self::PERIODOS[$periodo]['paso'];
            $fin = $ahora;
        } else {
            throw new DatosNoValidosException('periodo: 24h, 7d, 30d o rango');
        }
        $inicio = $fin - $n * $paso;
        $st = $this->bd->prepare("SELECT medida_en, valor, origen FROM {$this->p}medicion
            WHERE zona_id = ? AND magnitud = ? AND valida = 1 AND medida_en > ? AND medida_en <= ?");
        $st->execute([$zonaId, $magnitud, self::fechaBD($inicio), self::fechaBD($fin)]);
        // por tramo, real y simulado por separado: si hay real, manda el real
        $suma = ['real' => array_fill(0, $n, 0.0), 'simulado' => array_fill(0, $n, 0.0)];
        $cuenta = ['real' => array_fill(0, $n, 0), 'simulado' => array_fill(0, $n, 0)];
        $max = ['real' => array_fill(0, $n, null), 'simulado' => array_fill(0, $n, null)];
        $cuando = ['real' => array_fill(0, $n, null), 'simulado' => array_fill(0, $n, null)];
        foreach ($st->fetchAll(PDO::FETCH_ASSOC) as $f) {
            $o = $f['origen'] === 'real' ? 'real' : 'simulado';
            $i = min($n - 1, intdiv(self::msDeFechaBD($f['medida_en']) - $inicio - 1, $paso));
            $v = (float) $f['valor'];
            $suma[$o][$i] += $v;
            $cuenta[$o][$i]++;
            if ($max[$o][$i] === null || $v > $max[$o][$i]) {
                $max[$o][$i] = $v;
                $cuando[$o][$i] = self::msDeFechaBD($f['medida_en']);
            }
        }
        $dec = $magnitud === 'o3' ? 3 : 1;
        $valores = [];
        $maximos = [];
        $momentos = [];
        for ($i = 0; $i < $n; $i++) {
            $o = $cuenta['real'][$i] ? 'real' : 'simulado';
            $valores[] = $cuenta[$o][$i] ? round($suma[$o][$i] / $cuenta[$o][$i], $dec) : null;
            $maximos[] = $cuenta[$o][$i] ? round($max[$o][$i], $dec) : null;
            $momentos[] = $cuando[$o][$i];
        }
        return ['zona' => $zonaId, 'magnitud' => $magnitud, 'periodo' => $periodo, 'paso_min' => intdiv($paso, 60000),
            'desde' => self::fechaISO($inicio), 'hasta' => self::fechaISO($fin), 'valores' => $valores,
            'aprox' => self::aproximar($valores, $paso, $dec),
            'eventos' => self::eventos($valores, $maximos, $momentos, $magnitud, $inicio, $paso)];
    }

    // Límites de los niveles (los mismos que la app y la web): Medio y Alto
    const LIMITES = ['o3' => [0.060, 0.090], 'temp' => [25.0, 32.0], 'ruido' => [55.0, 65.0]];
    // Un hueco se aproxima (línea discontinua) si dura como mucho esto (o un tramo, si el tramo es mayor)
    const HUECO_APROX_MS = 2 * 3600000;

    // --------------------------------------------------------------
    // valores: [R | null], paso: N (ms) --> aproximar() --> [R | null]
    // Rellena por interpolación lineal SOLO los huecos cortos que tienen
    // dato a los dos lados. Lo devuelto es aparte: no cuenta en mín./media/máx.
    // --------------------------------------------------------------
    public static function aproximar(array $valores, int $paso, int $dec): array {
        $n = count($valores);
        $out = array_fill(0, $n, null);
        $maxTramos = max(1, intdiv(self::HUECO_APROX_MS, $paso));
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

    // --------------------------------------------------------------
    // eventos() --> {alertas: [{desde, hasta, max, nivel}], picos: [{momento, valor}]}
    //   alertas: ratos seguidos en nivel Alto (y, si no hay, en Medio)
    //   picos: los 3 valores más altos medidos (en tramos no seguidos), con su hora
    // --------------------------------------------------------------
    public static function eventos(array $valores, array $maximos, array $momentos, string $magnitud, int $inicio, int $paso): array {
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
                $alertas[] = ['desde' => self::fechaISO($inicio + $i * $paso), 'hasta' => self::fechaISO($inicio + ($j + 1) * $paso),
                    'max' => $mx, 'nivel' => $nivel];
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
            $picos[] = ['momento' => self::fechaISO($momentos[$i] ?? ($inicio + $i * $paso + intdiv($paso, 2))), 'valor' => $m];
            if (count($picos) === 3) {
                break;
            }
        }
        return ['alertas' => array_slice($alertas, -10), 'picos' => $picos];
    }

    // --------------------------------------------------------------
    // nodos() --> [{codigo, tipo, magnitudes, estado, asignado_a, bateria,
    //              ultima_min | null, ultima: {o3, temp, ruido}}]
    // ultima = último valor válido de cada magnitud (para el panel y «Mi sensor»)
    // --------------------------------------------------------------
    public function nodos(?string $codigo = null): array {
        $ahora = $this->ahora();
        $sql = "SELECT codigo, tipo, magnitudes, estado, asignado_a, bateria, ultima_medicion FROM {$this->p}nodo";
        $st = $this->bd->prepare($sql . ($codigo ? " WHERE codigo = ? AND estado <> 'baja'" : " WHERE estado <> 'baja' ORDER BY codigo"));
        $st->execute($codigo ? [strtoupper($codigo)] : []);
        $filas = $st->fetchAll(PDO::FETCH_ASSOC);
        if ($codigo && !$filas) {
            throw new NoEncontradoException("No existe el sensor $codigo");
        }
        $stUlt = $this->bd->prepare("SELECT valor, medida_en FROM {$this->p}medicion
            WHERE nodo = ? AND magnitud = ? AND valida = 1 ORDER BY medida_en DESC LIMIT 1");
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
                'estado' => $f['estado'], 'asignado_a' => $f['asignado_a'],
                'bateria' => $f['bateria'] === null ? null : (int) $f['bateria'],
                'ultima_min' => $f['ultima_medicion'] ? max(0, intdiv($ahora - self::msDeFechaBD($f['ultima_medicion']), 60000)) : null,
                'ultima' => $ultima];
        }
        return $out;
    }
}
