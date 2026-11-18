<?php
// ==============================================================
// Fichero:     Simulador.php
// Descripción: Mediciones SIMULADAS para enseñar Respira con todas las
//              zonas con datos mientras solo hay un sensor real.
//
//   - Se guardan en la misma tabla «medicion» con origen = 'simulado'
//     (se borran con: DELETE FROM medicion WHERE origen = 'simulado').
//   - Si una zona tiene datos REALES, el mapa y las gráficas usan los
//     reales (Logica::zonas y ::serie).
//   - No hace falta una tarea programada: la API llama a alDia() en
//     cada petición y rellena, cada 10 min, desde el último dato
//     simulado hasta ahora. El historial (30 días) se rellena hacia
//     atrás de 5 en 5 días por petición (para no pasarse de tiempo en
//     Plesk); cuando está completo, solo pasa como mucho cada 5 min.
//   - Los valores siguen el ritmo de un día de noviembre en Gandia:
//     ozono más alto por la tarde (con días de episodio), temperatura
//     con su ciclo diario y ruido con las horas punta y algún pico.
//     Son deterministas (mismo instante = mismo valor).
//   - Para que se vean los estados del mapa: «oeste» no tiene datos
//     (Sin datos) y «estacion» va 3 h por detrás (Antiguo). Hay huecos
//     cortos (se aproximan) y, de noche en los parques, alguno largo (gris).
//   Se apaga con 'simulacion' => false en api/config.php.
// Diseño:      doc/servidor.md
// Autor:       Aaron Blasco Blay
// Fecha:       10/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================

require_once __DIR__ . '/Base.php';

class Simulador extends Base {

    const PASO_MS = 600000;                 // un dato cada 10 min por zona y magnitud
    const DIAS = 30;
    const CADA_MS = 300000;                 // con el historial completo, como mucho una pasada cada 5 min
    const VERSION_MAPA = 2;
    const ATRAS_DIAS = 5;                   // días de historial que se rellenan por petición
    const SIN_DATOS = ['oeste', 'marenys'];
    const RETRASO = ['estacion' => 3 * 3600000, 'ermita' => 3 * 3600000];
    // sensores (de la demostración) a los que se apuntan los datos simulados;
    // si no existe ninguno, se crea RSP-SIM0
    const NODOS = ['RSP-L1MP', 'RSP-B12A', 'RSP-V08E', 'RSP-M7NT', 'RSP-SIM0'];
    const NODOS_RUIDO = ['RSP-L1MP', 'RSP-B12A'];

    private $marca;      // fichero con la hora de la última pasada (para no consultar la BD en cada petición)

    public function __construct(PDO $bd, string $prefijo = '', ?callable $reloj = null, ?string $marca = null) {
        parent::__construct($bd, $prefijo, $reloj);
        $this->marca = $marca;
    }

    // ---------- números «aleatorios» deterministas ----------

    // texto --> [0, 1)
    public static function azar(string $semilla): float {
        return (crc32($semilla) % 1000003) / 1000003.0;
    }

    // campana centrada en c (horas), anchura w
    private static function campana(float $h, float $c, float $w): float {
        $d = min(abs($h - $c), 24 - abs($h - $c));
        return exp(-($d * $d) / (2 * $w * $w));
    }

    // --------------------------------------------------------------
    // zona: {id, tipo}, magnitud, ms --> medida() --> R
    // --------------------------------------------------------------
    public static function medida(array $z, string $mag, int $ms): float {
        $l = new DateTime('@' . intdiv($ms, 1000));
        $l->setTimezone(new DateTimeZone(self::ZONA_HORARIA));
        $h = (int) $l->format('G') + (int) $l->format('i') / 60;
        $dia = $l->format('Y-m-d');
        $parque = $z['tipo'] === 'parque';
        $id = $z['id'];
        $ruido01 = self::azar("$id|$mag|$ms") - 0.5;
        if ($mag === 'o3') {
            // fondo + subida por la tarde (sol); algunos días, episodio de ozono
            $base = ($parque ? 0.044 : 0.038) + (self::azar("$id|o3") - 0.5) * 0.008;
            $episodio = self::azar("o3|$dia") < 0.22 ? 0.030 + self::azar("o3|$dia|$id") * 0.012 : 0.0;
            $v = $base - 0.016 + (0.034 + $episodio) * self::campana($h, 15.5, 2.8) + $ruido01 * 0.006;
            return round(max(0.004, $v), 3);
        }
        if ($mag === 'temp') {
            // mín. ~7 h, máx. ~15 h; días más templados o frescos; algún día de calor
            $dia01 = self::azar("temp|$dia");
            $calor = $dia01 > 0.92 ? 7.0 : 0.0;
            $v = 16.0 + ($dia01 - 0.5) * 5 + $calor + 5.2 * cos(($h - 15) / 24 * 2 * M_PI)
                + ($parque ? -0.8 : 0.0) + ($id === 'centro' ? 0.7 : 0.0) + $ruido01 * 0.6;
            return round($v, 1);
        }
        // ruido: horas punta, noche tranquila y algún pico (obras, tráfico, fiestas)
        $base = $parque ? 46.0 : ($id === 'centro' ? 59.0 : 54.0);
        $v = $base + 8 * self::campana($h, 8.5, 1.2) + 7 * self::campana($h, 19.5, 1.5)
            + 4 * self::campana($h, 13.5, 2.0) - 9 * self::campana($h, 3.5, 2.2) + $ruido01 * 5;
        $horaClave = $dia . '|' . (int) $h;
        if ($h >= 8 && $h <= 22 && self::azar("pico|$id|$horaClave") < 0.04) {
            $v += 14 + self::azar("pico2|$id|$horaClave") * 8;
        }
        return round(max(32.0, $v), 1);
    }

    // ¿Hueco sin datos en ese instante? (cortos de día; largos de noche en parques)
    public static function hueco(array $z, int $ms): bool {
        $l = new DateTime('@' . intdiv($ms, 1000));
        $l->setTimezone(new DateTimeZone(self::ZONA_HORARIA));
        $h = (int) $l->format('G') + (int) $l->format('i') / 60;
        $dia = $l->format('Y-m-d');
        $id = $z['id'];
        // hueco corto (40-100 min) algunos días, a una hora al azar
        if (self::azar("hc|$id|$dia") < 0.35) {
            $ini = 7 + self::azar("hc2|$id|$dia") * 14;
            $dur = (40 + self::azar("hc3|$id|$dia") * 60) / 60;
            if ($h >= $ini && $h < $ini + $dur) {
                return true;
            }
        }
        // parques: alguna noche nadie pasa (2-5 h)
        if ($z['tipo'] === 'parque' && self::azar("hl|$id|$dia") < 0.18) {
            $ini = 1 + self::azar("hl2|$id|$dia") * 1.5;
            $dur = 2 + self::azar("hl3|$id|$dia") * 3;
            if ($h >= $ini && $h < $ini + $dur) {
                return true;
            }
        }
        return false;
    }

    // --------------------------------------------------------------
    // alDia() --> N (filas nuevas). Rellena hasta ahora.
    // --------------------------------------------------------------
    public function alDia(bool $forzar = false): int {
        if ($this->marca === null) {
            return $this->rellenar($this->ahora())[0];
        }
        $fichero = fopen($this->marca, 'c+');
        if ($fichero === false) {
            throw new ErrorDeAlmacenamientoException('No se pudo abrir la marca de la simulación.');
        }
        try {
            if (!flock($fichero, LOCK_EX | LOCK_NB)) {
                return 0;
            }
            $ahora = $this->ahora();
            $marca = explode('|', (string) stream_get_contents($fichero));
            if (!$forzar && count($marca) === 3 && (int) $marca[2] === self::VERSION_MAPA
                && $marca[1] === '1' && $ahora - (int) $marca[0] < self::CADA_MS) {
                return 0;
            }
            [$total, $completo] = $this->rellenar($ahora);
            rewind($fichero);
            ftruncate($fichero, 0);
            fwrite($fichero, $ahora . '|' . ($completo ? '1' : '0') . '|' . self::VERSION_MAPA);
            return $total;
        } finally {
            fclose($fichero);
        }
    }

    private function rellenar(int $ahora): array {
        @set_time_limit(120);
        $completo = true;
        $nodos = array_values(array_filter(self::NODOS, function ($c) {
            return (bool) $this->fila("SELECT codigo FROM {$this->p}nodo WHERE codigo = ?", [$c]);
        }));
        if (!$nodos) {
            $this->ejecutar("INSERT INTO {$this->p}nodo (codigo, ayuntamiento_id, tipo, magnitudes, estado, asignado_a, incidencia)
                VALUES ('RSP-SIM0', (SELECT MIN(id) FROM {$this->p}ayuntamiento), 'trabajador', 'o3,temp,ruido', 'activo', 'Datos simulados', 'Sin incidencias')");
            $nodos = ['RSP-SIM0'];
        }
        $ruido = array_values(array_intersect($nodos, self::NODOS_RUIDO)) ?: [$nodos[0]];
        $total = 0;
        $zonas = $this->filas("SELECT id, tipo, etiqueta_lat, etiqueta_lng FROM {$this->p}zona");
        foreach ($zonas as $z) {
            if (in_array($z['id'], self::SIN_DATOS, true)) {
                continue;
            }
            $hasta = $ahora - (self::RETRASO[$z['id']] ?? 0);
            $tope = $ahora - self::DIAS * self::DIA_MS;
            $ult = $this->valor("SELECT MAX(fecha_medicion) FROM {$this->p}medicion WHERE zona_id = ? AND tipo = 'o3' AND origen = 'simulado'", [$z['id']]);
            $pri = $this->valor("SELECT MIN(fecha_medicion) FROM {$this->p}medicion WHERE zona_id = ? AND tipo = 'o3' AND origen = 'simulado'", [$z['id']]);
            // tramos que faltan: [desde, hasta] hacia delante y, si falta historial, hacia atrás
            $tramos = [];
            if (!$ult) {
                $tramos[] = [max($tope, $hasta - self::ATRAS_DIAS * self::DIA_MS), $hasta];
            } else {
                $tramos[] = [self::msDeFechaBD($ult) + 1, $hasta];
                $priMs = self::msDeFechaBD($pri);
                if ($priMs - self::PASO_MS > $tope) {
                    $tramos[] = [max($tope, $priMs - self::ATRAS_DIAS * self::DIA_MS), $priMs - 1];
                }
            }
            $filas = [];
            foreach ($tramos as [$desde, $fin]) {
                for ($t = (int) (ceil($desde / self::PASO_MS) * self::PASO_MS); $t <= $fin; $t += self::PASO_MS) {
                    if (self::hueco($z, $t)) {
                        continue;
                    }
                    $k = intdiv($t, self::PASO_MS);
                    foreach (['o3', 'temp', 'ruido'] as $m) {
                        $lista = $m === 'ruido' ? $ruido : $nodos;
                        $nodo = $lista[$k % count($lista)];
                        $lat = (float) $z['etiqueta_lat'] + (self::azar("la|$k|{$z['id']}") - 0.5) * 0.0006;
                        $lng = (float) $z['etiqueta_lng'] + (self::azar("ln|$k|{$z['id']}") - 0.5) * 0.0006;
                        $f = self::fechaBD($t);
                        $filas[] = [$nodo, $m, self::medida($z, $m, $t), $f, round($lat, 6), round($lng, 6), $z['id']];
                    }
                }
            }
            if ((count($tramos) > 1 && $tramos[1][0] > $tope) || (!$ult && $tramos[0][0] > $tope)) {
                $completo = false;      // aún falta historial: la próxima petición sigue
            }
            $total += $this->insertar($filas);
        }
        return [$total, $completo];
    }

    // Inserta en bloques de 300 filas (rápido también en Plesk)
    private function insertar(array $filas): int {
        foreach (array_chunk($filas, 300) as $bloque) {
            $sql = "INSERT INTO {$this->p}medicion (id_nodo, tipo, valor, fecha_medicion, lat, lng, zona_id, origen)
                VALUES " . implode(',', array_fill(0, count($bloque), "(?, ?, ?, ?, ?, ?, ?, 'simulado')"));
            $this->ejecutar($sql, array_merge(...$bloque));
        }
        return count($filas);
    }
}
