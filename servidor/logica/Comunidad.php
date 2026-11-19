<?php
// ==============================================================
// Fichero:     Comunidad.php
// Descripción: Percepciones de los vecinos (capa «Comunidad» del mapa):
//              lo que notan sobre el ruido, el calor y el aire.
//              - Se publican desde la app con una cuenta.
//              - Se ven 24 h. Máximo 3 por hora y cuenta.
//              - Sin nombres: el mapa no sabe quién la publicó.
//              - Solo quien la publicó puede cambiar el tipo y el nivel
//                (no el lugar ni la hora) o borrarla.
//              No son mediciones: no cuentan en las medias de las zonas.
// Diseño:      doc/servidor.md
// Autor:       Aaron Blasco Blay
// Fecha:       09/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================
// # tipos: Base.php (Valor, Registro, Ausente, Error y capacidades lógicas).

require_once __DIR__ . '/Base.php';

// Base <--- Comunidad
class Comunidad extends Base {

    const TIPOS = ['ruido', 'temp', 'aire'];
    const MAX_POR_HORA = 3;
    const VISIBLE_MS = 24 * Base::HORA_MS;
    private const EJEMPLOS = [
        [39.002841, -0.166719, 'Parc del Clot de la Mota', 'temp'],
        [39.003615, -0.160804, 'Platja Nord de Gandia', 'ruido'],
        [38.988863, -0.160567, 'Parc de l’Oller', 'aire'],
        [39.008903, -0.172225, 'Plaça del Castell', 'ruido'],
        [38.991451, -0.151895, 'Platja de Venècia', 'aire'],
    ];

    // --------------------------------------------------------------
    // # effects: consulta el reloj inyectado.
    // - p: Registro, quien: Registro | Ausente
    //     -->
    //         json() <--
    //     <--
    //     Registro
    // --------------------------------------------------------------
    private function json(array $p, ?array $quien): array {
        $creada = self::msDeFechaBD($p['creada']);
        return ['id' => (int) $p['id'], 'tipo' => $p['tipo'], 'nivel' => (int) $p['nivel'],
            'lat' => (float) $p['lat'], 'lng' => (float) $p['lng'], 'direccion' => $p['direccion'],
            'creada' => self::fechaISO($creada), 'hace_min' => max(0, intdiv($this->ahora() - $creada, 60000)),
            'mia' => $quien !== null && $quien['id'] === $p['usuario_id'], 'simulado' => false];
    }

    // --------------------------------------------------------------
    // # ('mia' = la ha publicado quien pregunta; nunca se dice quién es el autor)
    // # effects: consulta la base de datos; consulta el reloj inyectado.
    // + quien: Registro | Ausente, simulacion: B
    //     -->
    //         percepciones() <--
    //     <--
    //     [Registro] | Error
    // --------------------------------------------------------------
    public function percepciones(?array $quien, bool $simulacion = false): array {
        $ahora = $this->ahora();
        $desde = self::fechaBD($ahora - self::VISIBLE_MS);
        $out = [];
        foreach ($this->filas("SELECT * FROM {$this->p}percepcion WHERE creada > ? ORDER BY creada DESC", [$desde]) as $p) {
            $out[] = $this->json($p, $quien);
        }
        if ($simulacion) {
            $paso = 3 * self::HORA_MS;
            for ($t = (intdiv($ahora - self::VISIBLE_MS, $paso) + 1) * $paso; $t <= $ahora; $t += $paso) {
                $periodo = intdiv($t, $paso);
                [$lat, $lng, $direccion, $tipo] = self::EJEMPLOS[$periodo % count(self::EJEMPLOS)];
                $out[] = ['id' => -$periodo, 'tipo' => $tipo, 'nivel' => 1 + $periodo % 3,
                    'lat' => $lat, 'lng' => $lng, 'direccion' => $direccion, 'creada' => self::fechaISO($t),
                    'hace_min' => intdiv($ahora - $t, 60000), 'mia' => false, 'simulado' => true];
            }
            usort($out, static fn($a, $b) => strcmp($b['creada'], $a['creada']));
        }
        return $out;
    }

    // Las mías (también las que ya no se ven en el mapa), de los últimos 30 días
    // --------------------------------------------------------------
    // # effects: consulta la base de datos; consulta el reloj inyectado.
    // + quien: Registro | Ausente
    //     -->
    //         mias() <--
    //     <--
    //     [Registro] | Error
    // --------------------------------------------------------------
    public function mias(?array $quien): array {
        self::exigirRol($quien, ['vecino', 'trabajador']);
        $desde = self::fechaBD($this->ahora() - 30 * self::DIA_MS);
        return array_map(function ($p) use ($quien) { return $this->json($p, $quien); },
            $this->filas("SELECT * FROM {$this->p}percepcion WHERE usuario_id = ? AND creada >= ? ORDER BY creada DESC",
                [$quien['id'], $desde]));
    }

    // --------------------------------------------------------------
    // - tipo: Text, nivel: Valor
    //     -->
    //         validar() --x
    //     <--
    //     0 | Error
    // --------------------------------------------------------------
    private static function validar(string $tipo, $nivel): void {
        if (!in_array($tipo, self::TIPOS, true)) {
            throw new DatosNoValidosException('tipo: ruido, temp o aire', 'tipo');
        }
        if (filter_var($nivel, FILTER_VALIDATE_INT) === false || (int) $nivel < 1 || (int) $nivel > 3) {
            throw new DatosNoValidosException('nivel: 1 (bajo), 2 (medio) o 3 (alto)', 'nivel');
        }
    }

    // --------------------------------------------------------------
    // # Cuenta todos los envíos de la hora, aunque luego se borre la percepción.
    // # effects: consulta y modifica la base de datos; consulta el reloj inyectado.
    // + quien: Registro | Ausente, tipo: Text, nivel: Valor, lat: Valor, lng: Valor, direccion: Text
    //     -->
    //         publicar() -->
    //     <--
    //     Registro | Error
    // --------------------------------------------------------------
    public function publicar(?array $quien, string $tipo, $nivel, $lat, $lng, string $direccion): array {
        self::exigirRol($quien, ['vecino', 'trabajador']);
        self::validar($tipo, $nivel);
        if (!is_numeric($lat) || !is_numeric($lng) || $lat < -90 || $lat > 90 || $lng < -180 || $lng > 180) {
            throw new DatosNoValidosException('Falta el lugar (lat y lng).', 'lugar');
        }
        $dir = trim($direccion);
        if ($dir === '' || mb_strlen($dir) > 120) {
            throw new DatosNoValidosException('Escribe la dirección o el lugar (máximo 120 caracteres).', 'direccion');
        }
        return $this->transaccion(function () use ($quien, $tipo, $nivel, $lat, $lng, $dir) {
            $usuario = $quien['id'];
            $bloqueo = $this->bd->getAttribute(PDO::ATTR_DRIVER_NAME) === 'sqlite' ? '' : ' FOR UPDATE';
            $this->fila("SELECT id FROM {$this->p}usuario WHERE id = ?$bloqueo", [$usuario]);
            $ahora = $this->ahora();
            $desde = self::fechaBD($ahora - self::HORA_MS);
            $this->ejecutar("DELETE FROM {$this->p}percepcion_envio WHERE usuario_id = ? AND creada <= ?", [$usuario, $desde]);
            $enLaHora = (int) $this->valor("SELECT COUNT(*) FROM {$this->p}percepcion_envio WHERE usuario_id = ?", [$usuario]);
            if ($enLaHora >= self::MAX_POR_HORA) {
                throw new DemasiadasException('Ya has publicado ' . self::MAX_POR_HORA . ' en la última hora. Prueba un poco más tarde.');
            }
            $creada = self::fechaBD($ahora);
            $this->ejecutar("INSERT INTO {$this->p}percepcion (usuario_id, tipo, nivel, lat, lng, direccion, creada) VALUES (?, ?, ?, ?, ?, ?, ?)",
                [$usuario, $tipo, (int) $nivel, round((float) $lat, 6), round((float) $lng, 6), $dir, $creada]);
            $id = (int) $this->bd->lastInsertId();
            $this->ejecutar("INSERT INTO {$this->p}percepcion_envio (percepcion_id, usuario_id, creada) VALUES (?, ?, ?)",
                [$id, $usuario, $creada]);
            return $this->json($this->fila("SELECT * FROM {$this->p}percepcion WHERE id = ?", [$id]), $quien);
        });
    }

    // --------------------------------------------------------------
    // # Comprueba la autoría antes de permitir cambios en la percepción.
    // # effects: consulta la base de datos.
    // - quien: Registro | Ausente, id: Z
    //     -->
    //         propia() <--
    //     <--
    //     Registro | Error
    // --------------------------------------------------------------
    private function propia(?array $quien, int $id): array {
        self::exigirRol($quien, ['vecino', 'trabajador']);
        $p = $this->fila("SELECT * FROM {$this->p}percepcion WHERE id = ?", [$id]);
        if (!$p) {
            throw new NoEncontradoException('Esta percepción ya no existe.');
        }
        if ($p['usuario_id'] !== $quien['id']) {
            throw new ProhibidoException('Solo quien la publicó puede editarla o borrarla.');
        }
        return $p;
    }

    // Cambia tipo y nivel; conserva lugar, hora y autor
    // --------------------------------------------------------------
    // # effects: consulta y modifica la base de datos.
    // + quien: Registro | Ausente, id: Z, tipo: Text, nivel: Valor
    //     -->
    //         editar() -->
    //     <--
    //     Registro | Error
    // --------------------------------------------------------------
    public function editar(?array $quien, int $id, string $tipo, $nivel): array {
        $this->propia($quien, $id);
        self::validar($tipo, $nivel);
        $this->ejecutar("UPDATE {$this->p}percepcion SET tipo = ?, nivel = ? WHERE id = ?",
            [$tipo, (int) $nivel, $id]);
        return $this->json($this->fila("SELECT * FROM {$this->p}percepcion WHERE id = ?", [$id]), $quien);
    }

    // --------------------------------------------------------------
    // # effects: consulta y modifica la base de datos.
    // + quien: Registro | Ausente, id: Z
    //     -->
    //         borrar() -->
    //     <--
    //     0 | Error
    // --------------------------------------------------------------
    public function borrar(?array $quien, int $id): void {
        $this->propia($quien, $id);
        $this->ejecutar("DELETE FROM {$this->p}percepcion WHERE id = ?", [$id]);
    }
}
