<?php
// ==============================================================
// Fichero:     Cuentas.php
// Descripción: Cuentas y seguridad: registro de vecinos (app), inicio y
//              cierre de sesión (app y web), bloqueo tras 5 fallos,
//              enlaces de un solo uso (activar la cuenta de un
//              trabajador, recuperar la contraseña) y alta de
//              trabajadores municipales desde administración.
//
//   - Las contraseñas se guardan con password_hash (bcrypt).
//   - Los tokens de sesión y de los enlaces son 32 bytes aleatorios; en
//     la BD solo se guarda su sha256 (si alguien lee la BD, no le sirven).
// Diseño:      doc/servidor.md
// Autor:       Aaron Blasco Blay
// Fecha:       09/11/2026
// Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
// ==============================================================

require_once __DIR__ . '/Base.php';

class Cuentas extends Base {

    const MAX_INTENTOS = 5;
    const BLOQUEO_MS = 15 * 60000;
    const SESION_APP_MS = 90 * Base::DIA_MS;   // la app no pide la contraseña cada día
    const SESION_WEB_MS = 12 * Base::HORA_MS;  // el panel caduca en una jornada
    const ACTIVAR_MS = 48 * Base::HORA_MS;
    const RECUPERAR_MS = 1 * Base::HORA_MS;
    const ROLES_PANEL = ['admin', 'tecnico', 'reparto'];
    const ROLES_APP = ['vecino', 'trabajador'];
    const SERVICIOS = ['Limpieza viaria', 'Parques y jardines', 'Mantenimiento urbano', 'Policía local',
        'Movilidad', 'Otro servicio municipal'];

    private $correo;   // (para, asunto, texto) --> B   (se cambia en los tests)

    public function __construct(PDO $bd, string $prefijo = '', ?callable $reloj = null, ?callable $correo = null) {
        parent::__construct($bd, $prefijo, $reloj);
        $this->correo = $correo ?? function ($para, $asunto, $texto) { return false; };
    }

    // ---------- utilidades ----------

    public static function hashToken(string $token): string {
        return hash('sha256', $token);
    }

    private static function tokenNuevo(): string {
        return bin2hex(random_bytes(32));
    }

    public static function esEmail(string $m): bool {
        return (bool) preg_match('/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/', $m);
    }

    // --------------------------------------------------------------
    // fila: usuario (BD) --> perfil() --> lo que ve el propio usuario
    // --------------------------------------------------------------
    public function perfil(array $u): array {
        $punto = null;
        if ($u['punto_reparto_id'] !== null) {
            $punto = $this->fila("SELECT id, nombre FROM {$this->p}punto_reparto WHERE id = ?", [$u['punto_reparto_id']]);
            if ($punto) {
                $punto['id'] = (int) $punto['id'];
            }
        }
        return ['id' => $u['id'], 'rol' => $u['rol'], 'nombre' => $u['nombre'], 'email' => $u['email'],
            'servicio' => $u['servicio'], 'punto' => $punto];
    }

    public function usuarioPorId(string $id): ?array {
        return $this->fila("SELECT * FROM {$this->p}usuario WHERE id = ?", [$id]);
    }

    // --------------------------------------------------------------
    // rol, clave, email --> errorClave() --> Text | null
    //   vecinos: 8 caracteres (como la app)
    //   personal del ayuntamiento: 10, con letras y números, sin su usuario
    // --------------------------------------------------------------
    public static function errorClave(string $rol, string $clave, string $email): ?string {
        if ($clave === '') {
            return 'Escribe una contraseña.';
        }
        if ($rol === 'vecino') {
            return strlen($clave) < 8 ? 'La contraseña debe tener al menos 8 caracteres.' : null;
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
    // Registro desde la app (vecinos). Deja la sesión abierta.
    // nombre, email, clave: Text, acepta: B --> registrar() --> {token, usuario}
    // Lanza ErroresDeCamposException {nombre|email|clave|acepta: mensaje}
    // --------------------------------------------------------------
    public function registrar(string $nombre, string $email, string $clave, bool $acepta): array {
        $n = trim($nombre);
        $m = strtolower(trim($email));
        $e = [];
        if ($n === '') {
            $e['nombre'] = 'Elige un nombre de usuario.';
        } elseif (!preg_match('/^[A-Za-z0-9._]{3,20}$/', $n)) {
            $e['nombre'] = 'Usa de 3 a 20 letras, números, puntos o guiones bajos, sin espacios.';
        } elseif ((int) $this->valor("SELECT COUNT(*) FROM {$this->p}usuario WHERE rol = 'vecino' AND LOWER(nombre) = ?", [strtolower($n)]) > 0) {
            $e['nombre'] = 'Ese nombre de usuario ya existe. Prueba con otro.';
        }
        if ($m === '') {
            $e['email'] = 'Escribe tu correo.';
        } elseif (!self::esEmail($m)) {
            $e['email'] = 'El correo no tiene un formato válido. Ejemplo: nombre@correo.es';
        } elseif ($this->fila("SELECT id FROM {$this->p}usuario WHERE email = ?", [$m])) {
            $e['email'] = 'Ya hay una cuenta con este correo. Inicia sesión o recupera la contraseña.';
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
        return $this->transaccion(function () use ($n, $m, $clave, $ahora) {
            do {
                $id = 'U-' . random_int(10000, 99999);
            } while ($this->usuarioPorId($id));
            $this->ejecutar("INSERT INTO {$this->p}usuario (id, rol, nombre, email, clave_hash, estado, acepta_condiciones, alta)
                VALUES (?, 'vecino', ?, ?, ?, 'activo', ?, ?)",
                [$id, $n, $m, password_hash($clave, PASSWORD_DEFAULT), self::fechaBD($ahora), self::fechaBD($ahora)]);
            $u = $this->usuarioPorId($id);
            return ['token' => $this->abrirSesion($u, 'app'), 'usuario' => $this->perfil($u)];
        });
    }

    private function abrirSesion(array $u, string $cliente): string {
        $token = self::tokenNuevo();
        $ahora = $this->ahora();
        $dura = $cliente === 'web' ? self::SESION_WEB_MS : self::SESION_APP_MS;
        $this->ejecutar("INSERT INTO {$this->p}sesion (token_hash, usuario_id, cliente, creada, caduca) VALUES (?, ?, ?, ?, ?)",
            [self::hashToken($token), $u['id'], $cliente, self::fechaBD($ahora), self::fechaBD($ahora + $dura)]);
        // limpieza de sesiones caducadas (de paso)
        $this->ejecutar("DELETE FROM {$this->p}sesion WHERE caduca < ?", [self::fechaBD($ahora)]);
        return $token;
    }

    // --------------------------------------------------------------
    // email, clave, cliente ('app' | 'web') --> iniciarSesion() --> {token, usuario}
    //   401 correo o contraseña mal (con los intentos que quedan)
    //   423 bloqueada 15 min tras 5 fallos
    //   403 cuenta que no corresponde a ese cliente
    // --------------------------------------------------------------
    public function iniciarSesion(string $email, string $clave, string $cliente): array {
        $m = strtolower(trim($email));
        $ahora = $this->ahora();
        $u = $this->fila("SELECT * FROM {$this->p}usuario WHERE email = ?", [$m]);
        if ($u && $u['bloqueado_hasta'] !== null && self::msDeFechaBD($u['bloqueado_hasta']) > $ahora) {
            throw new BloqueadoException('Has fallado ' . self::MAX_INTENTOS
                . ' veces. Por seguridad, el acceso queda bloqueado 15 minutos.');
        }
        $bien = $u && $u['clave_hash'] !== null && password_verify($clave, $u['clave_hash']);
        if (!$bien) {
            if (!$u) {
                throw new NoAutorizadoException('Correo o contraseña incorrectos.');
            }
            $intentos = (int) $u['intentos_fallidos'] + 1;
            if ($intentos >= self::MAX_INTENTOS) {
                $this->ejecutar("UPDATE {$this->p}usuario SET intentos_fallidos = 0, bloqueado_hasta = ? WHERE id = ?",
                    [self::fechaBD($ahora + self::BLOQUEO_MS), $u['id']]);
                throw new BloqueadoException('Has fallado ' . self::MAX_INTENTOS
                    . ' veces. Por seguridad, el acceso queda bloqueado 15 minutos.');
            }
            $this->ejecutar("UPDATE {$this->p}usuario SET intentos_fallidos = ? WHERE id = ?", [$intentos, $u['id']]);
            $quedan = self::MAX_INTENTOS - $intentos;
            throw new NoAutorizadoException('Correo o contraseña incorrectos. Te quedan ' . $quedan
                . ($quedan === 1 ? ' intento.' : ' intentos.'), $quedan);
        }
        $this->ejecutar("UPDATE {$this->p}usuario SET intentos_fallidos = 0, bloqueado_hasta = NULL WHERE id = ?", [$u['id']]);
        if ($cliente === 'web' && !in_array($u['rol'], self::ROLES_PANEL, true)) {
            throw new ProhibidoException($u['rol'] === 'trabajador'
                ? 'Tu cuenta es de trabajador municipal. No tiene acceso al panel de gestión: entra en la app Respira con este correo.'
                : 'Esta cuenta es de la app Respira. El panel es solo para el personal del ayuntamiento.');
        }
        if ($cliente === 'app' && !in_array($u['rol'], self::ROLES_APP, true)) {
            throw new ProhibidoException('Las cuentas del ayuntamiento entran en el panel web, no en la app.');
        }
        return ['token' => $this->abrirSesion($u, $cliente), 'usuario' => $this->perfil($u)];
    }

    // --------------------------------------------------------------
    // token: Text | null --> usuarioDeSesion() --> usuario | null
    // --------------------------------------------------------------
    public function usuarioDeSesion(?string $token): ?array {
        if ($token === null || $token === '') {
            return null;
        }
        return $this->fila("SELECT u.* FROM {$this->p}sesion s JOIN {$this->p}usuario u ON u.id = s.usuario_id
            WHERE s.token_hash = ? AND s.caduca > ?", [self::hashToken($token), self::fechaBD($this->ahora())]);
    }

    public function cerrarSesion(string $token): void {
        $this->ejecutar("DELETE FROM {$this->p}sesion WHERE token_hash = ?", [self::hashToken($token)]);
    }

    // --------------------------------------------------------------
    // Borrar la cuenta (vecinos, desde la app). Se borran sus percepciones
    // y sus puntos; sus mediciones se quedan, pero sin portador.
    // --------------------------------------------------------------
    public function borrarCuenta(?array $u): void {
        self::exigirRol($u, ['vecino']);
        $abierto = $this->valor("SELECT nodo FROM {$this->p}prestamo WHERE usuario_id = ? AND fin IS NULL", [$u['id']]);
        if ($abierto) {
            throw new ConflictoException("Antes de borrar la cuenta, devuelve el sensor $abierto en un punto de reparto.");
        }
        $this->transaccion(function () use ($u) {
            // (SQLite no aplica ON DELETE si no se activan las FK: se hace a mano)
            foreach (['sesion', 'token_cuenta', 'percepcion', 'jornada'] as $t) {
                $this->ejecutar("DELETE FROM {$this->p}$t WHERE usuario_id = ?", [$u['id']]);
            }
            $this->ejecutar("UPDATE {$this->p}medicion SET portador_id = NULL WHERE portador_id = ?", [$u['id']]);
            $this->ejecutar("UPDATE {$this->p}prestamo SET usuario_id = NULL WHERE usuario_id = ?", [$u['id']]);
            $this->ejecutar("UPDATE {$this->p}canje SET usuario_id = NULL WHERE usuario_id = ?", [$u['id']]);
            $this->ejecutar("DELETE FROM {$this->p}usuario WHERE id = ?", [$u['id']]);
        });
    }

    // ==============================================================
    // Enlaces de un solo uso
    // ==============================================================

    private function crearEnlace(string $usuarioId, string $tipo): string {
        $token = self::tokenNuevo();
        $ahora = $this->ahora();
        // un enlace nuevo invalida los anteriores del mismo tipo
        $this->ejecutar("DELETE FROM {$this->p}token_cuenta WHERE usuario_id = ? AND tipo = ? AND usado IS NULL",
            [$usuarioId, $tipo]);
        $this->ejecutar("INSERT INTO {$this->p}token_cuenta (token_hash, usuario_id, tipo, creado, caduca) VALUES (?, ?, ?, ?, ?)",
            [self::hashToken($token), $usuarioId, $tipo, self::fechaBD($ahora),
                self::fechaBD($ahora + ($tipo === 'activar' ? self::ACTIVAR_MS : self::RECUPERAR_MS))]);
        return $token;
    }

    // --------------------------------------------------------------
    // email, urlWeb --> solicitarRecuperacion()
    // Siempre responde igual (no revela qué correos existen). Si la cuenta
    // existe y está activa, envía un correo con el enlace (válido 1 h).
    // --------------------------------------------------------------
    public function solicitarRecuperacion(string $email, string $urlWeb): void {
        $m = strtolower(trim($email));
        if (!self::esEmail($m)) {
            throw new DatosNoValidosException('El correo no tiene un formato válido.', 'email');
        }
        $u = $this->fila("SELECT * FROM {$this->p}usuario WHERE email = ? AND estado = 'activo'", [$m]);
        if (!$u) {
            return;
        }
        $token = $this->crearEnlace($u['id'], 'recuperar');
        $enlace = rtrim($urlWeb, '/') . '/#activar/' . $token;
        call_user_func($this->correo, $m, 'Respira: crea una contraseña nueva',
            "Hola, {$u['nombre']}:\n\nPara crear una contraseña nueva, abre este enlace (caduca en 1 hora y solo sirve una vez):\n\n$enlace\n\n"
            . "Si no lo has pedido tú, no hagas nada: tu contraseña no cambia.\n\nRespira · Nodia");
    }

    // --------------------------------------------------------------
    // token --> verEnlace() --> {estado: valido|caducado|no_valido, tipo, id, nombre, email, rol}
    // --------------------------------------------------------------
    public function verEnlace(string $token): array {
        $f = $this->fila("SELECT t.tipo, t.caduca, t.usado, u.id, u.nombre, u.email, u.rol
            FROM {$this->p}token_cuenta t JOIN {$this->p}usuario u ON u.id = t.usuario_id WHERE t.token_hash = ?",
            [self::hashToken($token)]);
        if (!$f || $f['usado'] !== null) {
            return ['estado' => 'no_valido'];
        }
        $estado = self::msDeFechaBD($f['caduca']) < $this->ahora() ? 'caducado' : 'valido';
        return ['estado' => $estado, 'tipo' => $f['tipo'], 'id' => $f['id'], 'nombre' => $f['nombre'],
            'email' => $f['email'], 'rol' => $f['rol'], 'horas' => $f['tipo'] === 'activar' ? 48 : 1];
    }

    // --------------------------------------------------------------
    // token, clave --> usarEnlace() --> email
    // Crea la contraseña (activar) o la cambia (recuperar). Cierra las
    // sesiones abiertas de esa cuenta.
    // --------------------------------------------------------------
    public function usarEnlace(string $token, string $clave): string {
        $v = $this->verEnlace($token);
        if ($v['estado'] === 'no_valido') {
            throw new NoEncontradoException('Este enlace no es válido. Puede que ya se haya usado o que se haya enviado uno nuevo.');
        }
        if ($v['estado'] === 'caducado') {
            throw new ConflictoException('El enlace ha caducado. Pide uno nuevo.');
        }
        $e = self::errorClave($v['rol'], $clave, $v['email']);
        if ($e) {
            throw new DatosNoValidosException($e, 'clave');
        }
        $ahora = $this->ahora();
        $this->transaccion(function () use ($token, $clave, $v, $ahora) {
            $this->ejecutar("UPDATE {$this->p}usuario SET clave_hash = ?, estado = 'activo', intentos_fallidos = 0,
                bloqueado_hasta = NULL WHERE id = ?", [password_hash($clave, PASSWORD_DEFAULT), $v['id']]);
            $this->ejecutar("UPDATE {$this->p}token_cuenta SET usado = ? WHERE token_hash = ?",
                [self::fechaBD($ahora), self::hashToken($token)]);
            $this->ejecutar("DELETE FROM {$this->p}sesion WHERE usuario_id = ?", [$v['id']]);
        });
        return $v['email'];
    }

    // ==============================================================
    // Trabajadores municipales (solo administración)
    // ==============================================================

    // --------------------------------------------------------------
    // trabajadores() --> [{id, nombre, email, num_empleado, servicio, alta,
    //                      estado: activo|pendiente|caducado}]
    // --------------------------------------------------------------
    public function trabajadores(?array $quien): array {
        self::exigirRol($quien, ['admin']);
        $ahora = $this->ahora();
        $out = [];
        foreach ($this->filas("SELECT * FROM {$this->p}usuario WHERE rol = 'trabajador' ORDER BY id") as $t) {
            $estado = $t['estado'];
            if ($estado === 'pendiente') {
                $cad = $this->valor("SELECT MAX(caduca) FROM {$this->p}token_cuenta WHERE usuario_id = ? AND tipo = 'activar' AND usado IS NULL", [$t['id']]);
                if (!$cad || self::msDeFechaBD($cad) < $ahora) {
                    $estado = 'caducado';
                }
            }
            $out[] = ['id' => $t['id'], 'nombre' => $t['nombre'], 'email' => $t['email'], 'num_empleado' => $t['num_empleado'],
                'servicio' => $t['servicio'], 'alta' => self::isoDeBD($t['alta']), 'estado' => $estado];
        }
        return $out;
    }

    // --------------------------------------------------------------
    // Alta de un trabajador: la cuenta queda pendiente hasta que él mismo
    // crea su contraseña con el enlace (48 h). --> {trabajador, token}
    // --------------------------------------------------------------
    public function altaTrabajador(?array $quien, string $nombre, string $email, string $numEmpleado, string $servicio): array {
        self::exigirRol($quien, ['admin']);
        $n = preg_replace('/\s+/', ' ', trim($nombre));
        $m = strtolower(trim($email));
        $emp = trim($numEmpleado);
        $e = [];
        if ($n === '') {
            $e['nombre'] = 'Escribe el nombre y los apellidos.';
        } elseif (count(explode(' ', $n)) < 2) {
            $e['nombre'] = 'Escribe al menos un nombre y un apellido.';
        }
        if ($m === '') {
            $e['email'] = 'Escribe el correo municipal.';
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
        } elseif ($this->fila("SELECT id FROM {$this->p}usuario WHERE num_empleado = ?", [$emp])) {
            $e['num_empleado'] = 'Este número de empleado ya tiene una cuenta.';
        }
        if (!in_array($servicio, self::SERVICIOS, true)) {
            $e['servicio'] = 'Elige el servicio.';
        }
        if ($e) {
            throw new ErroresDeCamposException($e);
        }
        $ahora = $this->ahora();
        return $this->transaccion(function () use ($quien, $n, $m, $emp, $servicio, $ahora) {
            $max = $this->valor("SELECT MAX(id) FROM {$this->p}usuario WHERE rol = 'trabajador'");
            $id = 'T-' . str_pad((string) ($max ? ((int) substr($max, 2)) + 1 : 1), 4, '0', STR_PAD_LEFT);
            $this->ejecutar("INSERT INTO {$this->p}usuario (id, rol, nombre, email, clave_hash, estado, num_empleado, servicio, alta, alta_por)
                VALUES (?, 'trabajador', ?, ?, NULL, 'pendiente', ?, ?, ?, ?)",
                [$id, $n, $m, $emp, $servicio, self::fechaBD($ahora), $quien['id']]);
            $token = $this->crearEnlace($id, 'activar');
            $t = array_values(array_filter($this->trabajadores($quien), function ($x) use ($id) { return $x['id'] === $id; }))[0];
            return ['trabajador' => $t, 'token' => $token];
        });
    }

    // --------------------------------------------------------------
    // Enlace de activación nuevo (el anterior deja de servir) --> {trabajador, token}
    // --------------------------------------------------------------
    public function reenviarActivacion(?array $quien, string $id): array {
        self::exigirRol($quien, ['admin']);
        $t = $this->fila("SELECT * FROM {$this->p}usuario WHERE id = ? AND rol = 'trabajador'", [$id]);
        if (!$t) {
            throw new NoEncontradoException("No existe el trabajador $id.");
        }
        if ($t['estado'] !== 'pendiente') {
            throw new ConflictoException('Esta cuenta ya está activada.');
        }
        $token = $this->crearEnlace($id, 'activar');
        $tr = array_values(array_filter($this->trabajadores($quien), function ($x) use ($id) { return $x['id'] === $id; }))[0];
        return ['trabajador' => $tr, 'token' => $token];
    }

    // --------------------------------------------------------------
    // vecinos() --> [{id, nombre}]   (para el panel de préstamos)
    // --------------------------------------------------------------
    public function vecinos(?array $quien): array {
        self::exigirRol($quien, self::ROLES_PANEL);
        return $this->filas("SELECT id, nombre FROM {$this->p}usuario WHERE rol = 'vecino' ORDER BY id");
    }
}
