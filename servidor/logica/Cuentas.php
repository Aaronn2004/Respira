<?php
// ==============================================================
// Fichero:     Cuentas.php
// Descripción: Cuentas y seguridad: registro de vecinos (app), inicio y
//              cierre de sesión (app y web), enlaces de un solo uso
//              (recuperar la contraseña) y alta de trabajadores
//              municipales desde administración (que pone también su
//              contraseña: la cuenta queda activa al momento).
//
//   - Las contraseñas se guardan con password_hash (bcrypt).
//   - Los tokens de sesión y de los enlaces son 32 bytes aleatorios; en
//     la BD solo se guarda su sha256 (si alguien lee la BD, no le sirven).
// Diseño:      doc/servidor.md
// Autor:       Aaron Blasco Blay
// Fecha:       09/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================
// # tipos: Base.php (Valor, Registro, Ausente, Error y capacidades lógicas).

require_once __DIR__ . '/Base.php';
require_once __DIR__ . '/Logica.php';   // MAGNITUDES y RANGO (alertas)

// Base <--- Cuentas
class Cuentas extends Base {

    const SESION_APP_MS = 90 * Base::DIA_MS;   // la app no pide la contraseña cada día
    const SESION_WEB_MS = 12 * Base::HORA_MS;  // el panel caduca en una jornada
    const ROLES_PANEL = ['admin', 'tecnico', 'reparto'];
    const ROLES_APP = ['vecino', 'trabajador'];
    const SERVICIOS = ['Limpieza viaria', 'Parques y jardines', 'Mantenimiento urbano', 'Policía local',
        'Movilidad', 'Otro servicio municipal'];

    private $correo;   // (para, asunto, texto) --> B   (se cambia en los tests)

    // --------------------------------------------------------------
    // # effects: inicializa el estado y la conexión.
    // + bd: Conexion, prefijo: Text, reloj: Reloj | Ausente, correo: Correo | Ausente
    //     -->
    //         __construct() -->
    // --------------------------------------------------------------
    public function __construct(PDO $bd, string $prefijo = '', ?callable $reloj = null, ?callable $correo = null) {
        parent::__construct($bd, $prefijo, $reloj);
        $this->correo = $correo ?? function ($para, $asunto, $texto) { return false; };
    }

    // ---------- utilidades ----------

    // --------------------------------------------------------------
    // + token: Text
    //     -->
    //         hashToken() --x
    //     <--
    //     Text
    // --------------------------------------------------------------
    public static function hashToken(string $token): string {
        return hash('sha256', $token);
    }

    // --------------------------------------------------------------
    // # effects: obtiene aleatoriedad del sistema.
    // -       tokenNuevo() --x
    //     <--
    //     Text
    // --------------------------------------------------------------
    private static function tokenNuevo(): string {
        return bin2hex(random_bytes(32));
    }

    // --------------------------------------------------------------
    // + m: Text
    //     -->
    //         esEmail() --x
    //     <--
    //     B
    // --------------------------------------------------------------
    public static function esEmail(string $m): bool {
        return (bool) preg_match('/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/', $m);
    }

    // --------------------------------------------------------------
    // # effects: consulta la base de datos.
    // + u: Registro
    //     -->
    //         perfil() <--
    //     <--
    //     Registro | Error
    // --------------------------------------------------------------
    public function perfil(array $u): array {
        $servicio = $u['rol'] === 'trabajador'
            ? $this->valor("SELECT servicio FROM {$this->p}empleado WHERE usuario_id = ?", [$u['id']]) : null;
        return ['id' => $u['id'], 'rol' => $u['rol'], 'nombre' => $u['nombre'], 'email' => $u['email'],
            'servicio' => $servicio, 'punto' => null];
    }

    // --------------------------------------------------------------
    // # effects: consulta la base de datos.
    // + id: Text
    //     -->
    //         usuarioPorId() <--
    //     <--
    //     Registro | Ausente | Error
    // --------------------------------------------------------------
    public function usuarioPorId(string $id): ?array {
        return $this->fila("SELECT * FROM {$this->p}usuario WHERE id = ?", [$id]);
    }

    // --------------------------------------------------------------
    // # personal del ayuntamiento: 10, con letras y números, sin su usuario
    // # vecinos: al menos 8 caracteres; personal municipal: al menos 10 con letras y números.
    // + rol: Text, clave: Text, email: Text
    //     -->
    //         errorClave() --x
    //     <--
    //     Text | Ausente
    // --------------------------------------------------------------
    public static function errorClave(string $rol, string $clave, string $email): ?string {
        if ($clave === '') {
            return 'Escribe una contraseña.';
        }
        if (strlen($clave) > 72 || strpos($clave, chr(0)) !== false) {
            return 'La contraseña supera el tamaño admitido o contiene un carácter no válido.';
        }
        if ($rol === 'vecino') {
            return mb_strlen($clave) < 8 ? 'La contraseña debe tener al menos 8 caracteres.' : null;
        }
        if (mb_strlen($clave) < 10) {
            return 'Tiene que tener al menos 10 caracteres (has escrito ' . mb_strlen($clave) . ').';
        }
        if (!preg_match('/[a-z]/i', $clave) || !preg_match('/\d/', $clave)) {
            return 'Tiene que llevar letras y números.';
        }
        $usuario = strtolower(explode('@', $email)[0]);
        if ($usuario !== '' && strpos(strtolower($clave), $usuario) !== false) {
            return 'No puede contener tu nombre de usuario.';
        }
        return null;
    }

    // --------------------------------------------------------------
    // # Reintenta solo colisiones de identificador; los duplicados de campos se devuelven al formulario.
    // # effects: consulta y modifica la base de datos.
    // - insertar: Operacion, duplicados: Operacion
    //     -->
    //         altaSinColision() -->
    //     <--
    //     Registro | Error
    // --------------------------------------------------------------
    private function altaSinColision(callable $insertar, callable $duplicados): array {
        for ($intento = 0; $intento < 5; $intento++) {
            try {
                return $this->transaccion($insertar);
            } catch (PDOException $e) {
                if ($e->getCode() !== '23000') {
                    throw $e;
                }
                $campos = $duplicados();
                if ($campos) {
                    throw new ErroresDeCamposException($campos);
                }
                if (stripos($e->getMessage(), 'duplicate') === false && stripos($e->getMessage(), 'unique') === false) {
                    throw $e;
                }
            }
        }
        throw new ConflictoException('No se pudo reservar un identificador. Vuelve a intentarlo.');
    }

    // --------------------------------------------------------------
    // # Registro desde la app (vecinos). Deja la sesión abierta.
    // # Lanza ErroresDeCamposException {nombre|email|clave|acepta: mensaje}
    // # effects: consulta y modifica la base de datos; consulta el reloj inyectado.
    // + nombre: Text, email: Text, clave: Text, acepta: B
    //     -->
    //         registrar() -->
    //     <--
    //     Registro | Error
    // --------------------------------------------------------------
    public function registrar(string $nombre, string $email, string $clave, bool $acepta): array {
        $n = trim($nombre);
        $m = strtolower(trim($email));
        $e = [];
        if ($n === '') {
            $e['nombre'] = 'Elige un nombre de usuario.';
        } elseif (!preg_match('/^[A-Za-z0-9._]{3,20}$/', $n)) {
            $e['nombre'] = 'Usa de 3 a 20 letras, números, puntos o guiones bajos, sin espacios.';
        } elseif ($this->fila("SELECT id FROM {$this->p}usuario WHERE rol = 'vecino' AND LOWER(nombre) = ?", [strtolower($n)])) {
            $e['nombre'] = 'Ese nombre de usuario ya existe. Prueba con otro.';
        }
        if ($m === '') {
            $e['email'] = 'Escribe tu correo.';
        } elseif (strlen($m) > 120) {
            $e['email'] = 'El correo debe tener como máximo 120 caracteres.';
        } elseif (!self::esEmail($m)) {
            $e['email'] = 'El correo no tiene un formato válido. Ejemplo: nombre@correo.es';
        } elseif ($this->fila("SELECT id FROM {$this->p}usuario WHERE email = ?", [$m])) {
            $e['email'] = 'Ya hay una cuenta con este correo. Inicia sesión.';
        }
        $ec = self::errorClave('vecino', $clave, $m);
        if ($ec) {
            $e['clave'] = $clave === '' ? 'Elige una contraseña.' : $ec;
        }
        if (!$acepta) {
            $e['acepta'] = 'Tienes que aceptar los términos para crear la cuenta.';
        }
        if ($e) {
            throw new ErroresDeCamposException($e);
        }
        $ahora = $this->ahora();
        $hash = password_hash($clave, PASSWORD_DEFAULT);
        return $this->altaSinColision(function () use ($n, $m, $hash, $ahora) {
            do {
                $id = 'U-' . random_int(10000, 99999);
            } while ($this->usuarioPorId($id));
            // sin aceptar las condiciones no se llega aquí: «alta» es también el momento en que las aceptó
            $this->ejecutar("INSERT INTO {$this->p}usuario (id, rol, nombre, email, clave_hash, alta)
                VALUES (?, 'vecino', ?, ?, ?, ?)",
                [$id, $n, $m, $hash, self::fechaBD($ahora)]);
            $u = $this->usuarioPorId($id);
            return ['token' => $this->abrirSesion($u, 'app'), 'usuario' => $this->perfil($u)];
        }, function () use ($n, $m) {
            $e = [];
            if ($this->fila("SELECT id FROM {$this->p}usuario WHERE rol = 'vecino' AND LOWER(nombre) = ?", [strtolower($n)])) {
                $e['nombre'] = 'Ese nombre de usuario ya existe.';
            }
            if ($this->fila("SELECT id FROM {$this->p}usuario WHERE email = ?", [$m])) {
                $e['email'] = 'Ya hay una cuenta con ese correo.';
            }
            return $e;
        });
    }

    // --------------------------------------------------------------
    // # effects: consulta y modifica la base de datos; consulta el reloj inyectado.
    // - u: Registro, cliente: Text
    //     -->
    //         abrirSesion() -->
    //     <--
    //     Text | Error
    // --------------------------------------------------------------
    private function abrirSesion(array $u, string $cliente): string {
        $token = self::tokenNuevo();
        $ahora = $this->ahora();
        $dura = $cliente === 'web' ? self::SESION_WEB_MS : self::SESION_APP_MS;
        $this->ejecutar("INSERT INTO {$this->p}token (token_hash, usuario_id, tipo, caduca) VALUES (?, ?, ?, ?)",
            [self::hashToken($token), $u['id'], $cliente, self::fechaBD($ahora + $dura)]);
        // limpieza de sesiones caducadas (de paso). Los enlaces caducados se
        // quedan hasta que se pide otro, para poder decir «ha caducado».
        $this->ejecutar("DELETE FROM {$this->p}token WHERE tipo IN ('app', 'web') AND caduca < ?", [self::fechaBD($ahora)]);
        return $token;
    }

    // --------------------------------------------------------------
    // # 401 correo o contraseña mal
    // # 403 cuenta que no corresponde a ese cliente
    // # effects: consulta y modifica la base de datos; consulta el reloj inyectado.
    // + email: Text, clave: Text, cliente: Text
    //     -->
    //         iniciarSesion() -->
    //     <--
    //     Registro | Error
    // --------------------------------------------------------------
    public function iniciarSesion(string $email, string $clave, string $cliente): array {
        $m = strtolower(trim($email));
        $mensaje = 'Correo o contraseña incorrectos.';
        if (strlen($m) > 120 || !self::esEmail($m)) {
            throw new NoAutorizadoException($mensaje);
        }
        $u = $this->fila("SELECT * FROM {$this->p}usuario WHERE email = ?", [$m]);
        if (!$u || !password_verify($clave, $u['clave_hash'])) {
            throw new NoAutorizadoException($mensaje);
        }
        $this->validarCliente($u, $cliente);
        return ['token' => $this->abrirSesion($u, $cliente), 'usuario' => $this->perfil($u)];
    }

    // --------------------------------------------------------------
    // - u: Registro, cliente: Text
    //     -->
    //         validarCliente() --x
    //     <--
    //     0 | Error
    // --------------------------------------------------------------
    private function validarCliente(array $u, string $cliente): void {
        if ($cliente === 'web' && !in_array($u['rol'], self::ROLES_PANEL, true)) {
            throw new ProhibidoException($u['rol'] === 'trabajador'
                ? 'Tu cuenta es de trabajador municipal. No tiene acceso al panel de gestión: entra en la app Respira con este correo.'
                : 'Esta cuenta es de la app Respira. El panel es solo para el personal del ayuntamiento.');
        }
        if ($cliente === 'app' && !in_array($u['rol'], self::ROLES_APP, true)) {
            throw new ProhibidoException('Las cuentas del ayuntamiento entran en el panel web, no en la app.');
        }
    }

    // --------------------------------------------------------------
    // # effects: consulta la base de datos; consulta el reloj inyectado.
    // + token: Text | Ausente
    //     -->
    //         usuarioDeSesion() <--
    //     <--
    //     Registro | Ausente | Error
    // --------------------------------------------------------------
    public function usuarioDeSesion(?string $token): ?array {
        if ($token === null || $token === '') {
            return null;
        }
        return $this->fila("SELECT u.* FROM {$this->p}token s JOIN {$this->p}usuario u ON u.id = s.usuario_id
            WHERE s.token_hash = ? AND s.tipo IN ('app', 'web') AND s.caduca > ?", [self::hashToken($token), self::fechaBD($this->ahora())]);
    }

    // --------------------------------------------------------------
    // # effects: consulta y modifica la base de datos.
    // + token: Text
    //     -->
    //         cerrarSesion() -->
    //     <--
    //     0 | Error
    // --------------------------------------------------------------
    public function cerrarSesion(string $token): void {
        $this->ejecutar("DELETE FROM {$this->p}token WHERE token_hash = ? AND tipo IN ('app', 'web')", [self::hashToken($token)]);
    }

    // --------------------------------------------------------------
    // # Borrar la cuenta (vecinos, desde la app). Se borran sus percepciones
    // # y sus puntos; sus mediciones se quedan, pero sin portador.
    // # effects: consulta y modifica la base de datos.
    // + u: Registro | Ausente
    //     -->
    //         borrarCuenta() -->
    //     <--
    //     0 | Error
    // --------------------------------------------------------------
    public function borrarCuenta(?array $u): void {
        self::exigirRol($u, ['vecino']);
        $abierto = $this->valor("SELECT nodo FROM {$this->p}prestamo WHERE usuario_id = ? AND estado_devolucion = 'No devuelto'", [$u['id']]);
        if ($abierto) {
            throw new ConflictoException("Antes de borrar la cuenta, devuelve el sensor $abierto en un punto de reparto.");
        }
        $this->transaccion(function () use ($u) {
            // (SQLite no aplica ON DELETE si no se activan las FK: se hace a mano)
            foreach (['alerta_emitida', 'alerta', 'favorito', 'token', 'percepcion_envio', 'percepcion', 'puntos_dia'] as $t) {
                $this->ejecutar("DELETE FROM {$this->p}$t WHERE usuario_id = ?", [$u['id']]);
            }
            $this->ejecutar("UPDATE {$this->p}medicion SET portador_id = NULL WHERE portador_id = ?", [$u['id']]);
            $this->ejecutar("UPDATE {$this->p}prestamo SET usuario_id = NULL WHERE usuario_id = ?", [$u['id']]);
            $this->ejecutar("UPDATE {$this->p}canje SET usuario_id = NULL WHERE usuario_id = ?", [$u['id']]);
            $this->ejecutar("DELETE FROM {$this->p}usuario WHERE id = ?", [$u['id']]);
        });
    }

    // ==============================================================
    // Seguimiento de parques (app): favoritos y alertas de la cuenta
    // ==============================================================
    const MAX_FAVORITOS = 50;
    const CONDICIONES = ['bajo', 'medio', 'alto', 'custom'];

    // --------------------------------------------------------------
    // # effects: consulta la base de datos.
    // + u: Registro
    //     -->
    //         seguimiento() <--
    //     <--
    //     Registro | Error
    // --------------------------------------------------------------
    public function seguimiento(array $u): array {
        $fav = array_column($this->filas("SELECT zona_id FROM {$this->p}favorito WHERE usuario_id = ? ORDER BY creado, zona_id",
            [$u['id']]), 'zona_id');
        $al = [];
        foreach ($this->filas("SELECT zona_id, magnitud, condicion, valor FROM {$this->p}alerta WHERE usuario_id = ? ORDER BY zona_id, magnitud",
            [$u['id']]) as $a) {
            $al[] = ['zona' => $a['zona_id'], 'magnitud' => $a['magnitud'], 'condicion' => $a['condicion'],
                'valor' => $a['condicion'] === 'custom' ? (float) $a['valor'] : null];
        }
        return ['favoritos' => $fav, 'alertas' => $al];
    }

    // --------------------------------------------------------------
    // # Sustituye todo lo guardado (el móvil manda siempre la lista completa).
    // # Un parque con alerta se sigue aunque no venga en «favoritos».
    // # effects: consulta y modifica la base de datos; consulta el reloj inyectado.
    // + u: Registro | Ausente, c: Registro
    //     -->
    //         guardarSeguimiento() -->
    //     <--
    //     Registro | Error
    // --------------------------------------------------------------
    public function guardarSeguimiento(?array $u, array $c): array {
        self::exigirRol($u, ['vecino', 'trabajador']);
        $zonas = array_column($this->filas("SELECT id FROM {$this->p}zona"), 'id');
        $fav = $c['favoritos'] ?? [];
        $al = $c['alertas'] ?? [];
        if (!is_array($fav) || !is_array($al)) {
            throw new DatosNoValidosException('Formato no válido: favoritos y alertas son listas.');
        }
        $limpias = [];
        foreach ($al as $a) {
            $z = (string) ($a['zona'] ?? '');
            $m = (string) ($a['magnitud'] ?? '');
            $cond = (string) ($a['condicion'] ?? '');
            if (!in_array($z, $zonas, true)) {
                throw new DatosNoValidosException("La zona «{$z}» no existe.", 'zona');
            }
            if (!in_array($m, Logica::MAGNITUDES, true) || !in_array($cond, self::CONDICIONES, true)) {
                throw new DatosNoValidosException('Alerta no válida: elige ozono, temperatura o ruido y un nivel o un límite.', 'alerta');
            }
            $v = 0.0;
            if ($cond === 'custom') {
                [$min, $max] = Logica::RANGO[$m];
                if (!is_numeric($a['valor'] ?? null) || $a['valor'] < $min || $a['valor'] > $max) {
                    throw new DatosNoValidosException("El límite de la alerta debe ser un número entre $min y $max.", 'valor');
                }
                $v = (float) $a['valor'];
            }
            $limpias[$z . ':' . $m] = [$z, $m, $cond, $v];
        }
        $ids = [];
        foreach (array_merge($fav, array_column($limpias, 0)) as $z) {
            $z = (string) $z;
            if (!in_array($z, $zonas, true)) {
                throw new DatosNoValidosException("La zona «{$z}» no existe.", 'zona');
            }
            $ids[$z] = true;
        }
        if (count($ids) > self::MAX_FAVORITOS) {
            throw new DatosNoValidosException('Puedes seguir como mucho ' . self::MAX_FAVORITOS . ' parques.', 'favoritos');
        }
        $ahora = self::fechaBD($this->ahora());
        $this->transaccion(function () use ($u, $ids, $limpias, $ahora) {
            // se conserva la fecha en que se empezó a seguir cada parque
            $antes = [];
            foreach ($this->filas("SELECT zona_id, creado FROM {$this->p}favorito WHERE usuario_id = ?", [$u['id']]) as $f) {
                $antes[$f['zona_id']] = $f['creado'];
            }
            $this->ejecutar("DELETE FROM {$this->p}alerta WHERE usuario_id = ?", [$u['id']]);
            $this->ejecutar("DELETE FROM {$this->p}favorito WHERE usuario_id = ?", [$u['id']]);
            foreach (array_keys($ids) as $z) {
                $this->ejecutar("INSERT INTO {$this->p}favorito (usuario_id, zona_id, creado) VALUES (?, ?, ?)",
                    [$u['id'], $z, $antes[$z] ?? $ahora]);
            }
            foreach ($limpias as [$z, $m, $cond, $v]) {
                $this->ejecutar("INSERT INTO {$this->p}alerta (usuario_id, zona_id, magnitud, condicion, valor) VALUES (?, ?, ?, ?, ?)",
                    [$u['id'], $z, $m, $cond, $v]);
            }
        });
        return $this->seguimiento($u);
    }

    // --------------------------------------------------------------
    // # Solo acepta avisos coherentes con la alerta configurada y las lecturas de la zona.
    // # effects: consulta y modifica la base de datos.
    // + u: Registro, aviso: Registro, logica: Logica
    //     -->
    //         registrarAlertaEmitida() -->
    //     <--
    //     0 | Error
    // --------------------------------------------------------------
    public function registrarAlertaEmitida(array $u, array $aviso, Logica $logica): void {
        self::exigirRol($u, self::ROLES_APP);
        $zona = (string) ($aviso['zona'] ?? '');
        $magnitud = (string) ($aviso['magnitud'] ?? '');
        $momento = (string) ($aviso['momento'] ?? '');
        $origen = (string) ($aviso['origen'] ?? '');
        $valor = $aviso['valor'] ?? null;
        if (!in_array($magnitud, Logica::MAGNITUDES, true) || !is_numeric($valor)
            || !in_array($origen, ['real', 'simulado'], true)) {
            throw new DatosNoValidosException('Faltan la magnitud, el valor o el origen del aviso.', 'alerta');
        }
        if (strlen($momento) === 20 && str_ends_with($momento, 'Z')) {
            $momento = substr($momento, 0, -1) . '.000Z';
        }
        $fecha = DateTimeImmutable::createFromFormat('!Y-m-d\TH:i:s.v\Z', $momento, new DateTimeZone('UTC'));
        if (!$fecha || $fecha->format('Y-m-d\TH:i:s.v\Z') !== $momento) {
            throw new DatosNoValidosException('La hora del aviso no es válida.', 'momento');
        }
        $fechaBD = $fecha->format('Y-m-d H:i:s.v');
        $clave = [$u['id'], $zona, $magnitud, $fechaBD];
        if ($this->fila("SELECT 1 FROM {$this->p}alerta_emitida WHERE usuario_id = ? AND zona_id = ? AND magnitud = ? AND momento = ?", $clave)) {
            return; // el móvil puede reenviar el mismo aviso si perdió la respuesta
        }
        $config = $this->fila("SELECT condicion, valor FROM {$this->p}alerta WHERE usuario_id = ? AND zona_id = ? AND magnitud = ?",
            [$u['id'], $zona, $magnitud]);
        if (!$config) {
            throw new NoEncontradoException('No hay una alerta configurada para esa magnitud y zona.');
        }
        $datos = null;
        foreach ($logica->zonas() as $fila) {
            if ($fila['id'] === $zona) {
                $datos = $fila['detalle'][$magnitud] ?? null;
                break;
            }
        }
        if (!$datos || $datos['valor'] === null || (float) $datos['valor'] !== (float) $valor
            || $datos['momento'] !== $momento || $datos['origen'] !== $origen || $datos['estado'] !== 'reciente') {
            throw new ConflictoException('La lectura del aviso ya no coincide con la publicada para esa zona.');
        }
        [$medio, $alto] = Logica::LIMITES[$magnitud];
        $cumple = match ($config['condicion']) {
            'bajo' => $valor < $medio,
            'medio' => $valor >= $medio,
            'alto' => $valor >= $alto,
            'custom' => $valor > $config['valor'],
        };
        if (!$cumple) {
            throw new DatosNoValidosException('La lectura no alcanza el umbral del aviso.', 'valor');
        }
        $ignorar = $this->bd->getAttribute(PDO::ATTR_DRIVER_NAME) === 'sqlite' ? 'OR IGNORE' : 'IGNORE';
        $this->ejecutar("INSERT $ignorar INTO {$this->p}alerta_emitida (usuario_id, zona_id, magnitud, momento, valor, origen)
            VALUES (?, ?, ?, ?, ?, ?)", [...$clave, $valor, $origen]);
    }

    // ==============================================================
    // Trabajadores municipales (solo administración)
    // ==============================================================

    // --------------------------------------------------------------
    // # effects: consulta la base de datos.
    // + quien: Registro | Ausente
    //     -->
    //         trabajadores() <--
    //     <--
    //     [Registro] | Error
    // --------------------------------------------------------------
    public function trabajadores(?array $quien): array {
        self::exigirRol($quien, ['admin']);
        $out = [];
        foreach ($this->filas("SELECT u.id, u.nombre, u.email, u.alta, e.ayuntamiento_id, e.num_empleado, e.servicio
            FROM {$this->p}usuario u JOIN {$this->p}empleado e ON e.usuario_id = u.id
            WHERE u.rol = 'trabajador' ORDER BY u.id") as $t) {
            $out[] = ['id' => $t['id'], 'ayuntamiento' => $t['ayuntamiento_id'], 'nombre' => $t['nombre'], 'email' => $t['email'],
                'num_empleado' => $t['num_empleado'],
                'servicio' => $t['servicio'], 'alta' => self::isoDeBD($t['alta'])];
        }
        return $out;
    }

    // --------------------------------------------------------------
    // # Alta de un trabajador con contraseña inicial de cambio obligatorio.
    // # effects: consulta y modifica la base de datos; consulta el reloj inyectado.
    // + quien: Registro | Ausente, nombre: Text, email: Text, numEmpleado: Text, servicio: Text, clave: Text, ayuntamiento: Text | Ausente
    //     -->
    //         altaTrabajador() -->
    //     <--
    //     Registro | Error
    // --------------------------------------------------------------
    public function altaTrabajador(?array $quien, string $nombre, string $email, string $numEmpleado, string $servicio,
                                   string $clave, ?string $ayuntamiento = null): array {
        self::exigirRol($quien, ['admin']);
        $n = preg_replace('/\s+/', ' ', trim($nombre));
        $m = strtolower(trim($email));
        $emp = trim($numEmpleado);
        $e = [];
        if ($n === '') {
            $e['nombre'] = 'Escribe el nombre y los apellidos.';
        } elseif (mb_strlen($n) > 80) {
            $e['nombre'] = 'El nombre debe tener como máximo 80 caracteres.';
        } elseif (count(explode(' ', $n)) < 2) {
            $e['nombre'] = 'Escribe al menos un nombre y un apellido.';
        }
        if ($m === '') {
            $e['email'] = 'Escribe el correo municipal.';
        } elseif (strlen($m) > 120) {
            $e['email'] = 'El correo debe tener como máximo 120 caracteres.';
        } elseif (!self::esEmail($m)) {
            $e['email'] = 'El correo no tiene un formato válido. Ejemplo: nombre@ayto.es';
        } elseif (substr($m, -8) !== '@ayto.es') {
            $e['email'] = 'Tiene que ser un correo municipal (termina en @ayto.es).';
        } elseif ($this->fila("SELECT id FROM {$this->p}usuario WHERE email = ?", [$m])) {
            $e['email'] = 'Ya hay una cuenta con este correo.';
        }
        if ($emp === '') {
            $e['num_empleado'] = 'Escribe el número de empleado.';
        } elseif (!preg_match('/^\d{6}$/', $emp)) {
            $e['num_empleado'] = 'Son 6 cifras, por ejemplo 104233.';
        } elseif ($this->fila("SELECT usuario_id FROM {$this->p}empleado WHERE num_empleado = ?", [$emp])) {
            $e['num_empleado'] = 'Este número de empleado ya tiene una cuenta.';
        }
        if (!in_array($servicio, self::SERVICIOS, true)) {
            $e['servicio'] = 'Elige el servicio.';
        }
        $ec = self::errorClave('trabajador', $clave, $m);
        if ($ec) {
            $e['clave'] = $ec;
        }
        if ($e) {
            throw new ErroresDeCamposException($e);
        }
        $ayto = $this->ayuntamientoDe($ayuntamiento);
        $hash = password_hash($clave, PASSWORD_DEFAULT);
        return $this->altaSinColision(function () use ($quien, $n, $m, $emp, $servicio, $hash, $ayto) {
            $num = (int) substr((string) $this->valor("SELECT MAX(id) FROM {$this->p}usuario WHERE rol = 'trabajador'"), 2) + 1;
            if ($num > 9999) {
                throw new ConflictoException('No quedan identificadores de trabajador disponibles.');
            }
            $id = 'T-' . str_pad((string) $num, 4, '0', STR_PAD_LEFT);
            $this->ejecutar("INSERT INTO {$this->p}usuario (id, rol, nombre, email, clave_hash, alta)
                VALUES (?, 'trabajador', ?, ?, ?, ?)",
                [$id, $n, $m, $hash, self::fechaBD($this->ahora())]);
            $this->ejecutar("INSERT INTO {$this->p}empleado (usuario_id, ayuntamiento_id, num_empleado, servicio) VALUES (?, ?, ?, ?)",
                [$id, $ayto, $emp, $servicio]);
            $t = array_values(array_filter($this->trabajadores($quien), function ($x) use ($id) { return $x['id'] === $id; }));
            return ['trabajador' => $t[0]];
        }, function () use ($m, $emp) {
            $e = [];
            if ($this->fila("SELECT id FROM {$this->p}usuario WHERE email = ?", [$m])) {
                $e['email'] = 'Ya hay una cuenta con este correo.';
            }
            if ($this->fila("SELECT usuario_id FROM {$this->p}empleado WHERE num_empleado = ?", [$emp])) {
                $e['num_empleado'] = 'Este número de empleado ya tiene una cuenta.';
            }
            return $e;
        });
    }

    // --------------------------------------------------------------
    // # effects: consulta la base de datos.
    // + quien: Registro | Ausente
    //     -->
    //         vecinos() <--
    //     <--
    //     [Registro] | Error
    // --------------------------------------------------------------
    public function vecinos(?array $quien): array {
        self::exigirRol($quien, self::ROLES_PANEL);
        return $this->filas("SELECT id, nombre FROM {$this->p}usuario WHERE rol = 'vecino' ORDER BY id");
    }
}
