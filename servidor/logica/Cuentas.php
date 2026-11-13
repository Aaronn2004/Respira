<?php
// ==============================================================
// Fichero:     Cuentas.php
// Descripción: Cuentas y seguridad: registro de vecinos (app), inicio y
//              cierre de sesión (app y web), enlaces de un solo uso
//              (recuperar la contraseña) y alta de trabajadores
//              municipales desde administración: quedan como
//              «invitación» hasta que activan su cuenta con el enlace.
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
require_once __DIR__ . '/Logica.php';   // MAGNITUDES y RANGO (alertas)

class Cuentas extends Base {

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
        $servicio = $u['rol'] === 'trabajador'
            ? $this->valor("SELECT servicio FROM {$this->p}empleado WHERE usuario_id = ?", [$u['id']]) : null;
        return ['id' => $u['id'], 'rol' => $u['rol'], 'nombre' => $u['nombre'], 'email' => $u['email'],
            'servicio' => $servicio, 'punto' => null];
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
            // sin aceptar las condiciones no se llega aquí: «alta» es también el momento en que las aceptó
            $this->ejecutar("INSERT INTO {$this->p}usuario (id, rol, nombre, email, clave_hash, alta)
                VALUES (?, 'vecino', ?, ?, ?, ?)",
                [$id, $n, $m, password_hash($clave, PASSWORD_DEFAULT), self::fechaBD($ahora)]);
            $u = $this->usuarioPorId($id);
            return ['token' => $this->abrirSesion($u, 'app'), 'usuario' => $this->perfil($u)];
        });
    }

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
    // email, clave, cliente ('app' | 'web') --> iniciarSesion() --> {token, usuario}
    //   401 correo o contraseña mal
    //   403 cuenta que no corresponde a ese cliente
    // --------------------------------------------------------------
    public function iniciarSesion(string $email, string $clave, string $cliente): array {
        $m = strtolower(trim($email));
        $u = $this->fila("SELECT * FROM {$this->p}usuario WHERE email = ?", [$m]);
        if (!$u || !password_verify($clave, $u['clave_hash'])) {
            throw new NoAutorizadoException('Correo o contraseña incorrectos.');
        }
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
        return $this->fila("SELECT u.* FROM {$this->p}token s JOIN {$this->p}usuario u ON u.id = s.usuario_id
            WHERE s.token_hash = ? AND s.tipo IN ('app', 'web') AND s.caduca > ?", [self::hashToken($token), self::fechaBD($this->ahora())]);
    }

    public function cerrarSesion(string $token): void {
        $this->ejecutar("DELETE FROM {$this->p}token WHERE token_hash = ? AND tipo IN ('app', 'web')", [self::hashToken($token)]);
    }

    // --------------------------------------------------------------
    // Borrar la cuenta (vecinos, desde la app). Se borran sus percepciones
    // y sus puntos; sus mediciones se quedan, pero sin portador.
    // --------------------------------------------------------------
    public function borrarCuenta(?array $u): void {
        self::exigirRol($u, ['vecino']);
        $abierto = $this->valor("SELECT nodo FROM {$this->p}prestamo WHERE usuario_id = ? AND estado_devolucion = 'No devuelto'", [$u['id']]);
        if ($abierto) {
            throw new ConflictoException("Antes de borrar la cuenta, devuelve el sensor $abierto en un punto de reparto.");
        }
        $this->transaccion(function () use ($u) {
            // (SQLite no aplica ON DELETE si no se activan las FK: se hace a mano)
            foreach (['alerta', 'favorito', 'token', 'percepcion', 'revision'] as $t) {
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
    // usuario --> seguimiento() --> {favoritos: [zonaId], alertas: [{zona, magnitud, condicion, valor | null}]}
    // --------------------------------------------------------------
    public function seguimiento(array $u): array {
        $fav = array_column($this->filas("SELECT zona_id FROM {$this->p}favorito WHERE usuario_id = ? ORDER BY creado, zona_id",
            [$u['id']]), 'zona_id');
        $al = [];
        foreach ($this->filas("SELECT zona_id, magnitud, condicion, valor FROM {$this->p}alerta WHERE usuario_id = ? ORDER BY zona_id",
            [$u['id']]) as $a) {
            $al[] = ['zona' => $a['zona_id'], 'magnitud' => $a['magnitud'], 'condicion' => $a['condicion'],
                'valor' => $a['condicion'] === 'custom' ? (float) $a['valor'] : null];
        }
        return ['favoritos' => $fav, 'alertas' => $al];
    }

    // --------------------------------------------------------------
    // usuario, {favoritos, alertas} --> guardarSeguimiento() --> seguimiento()
    // Sustituye todo lo guardado (el móvil manda siempre la lista completa).
    // Un parque con alerta se sigue aunque no venga en «favoritos».
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
                if (!is_numeric($a['valor'] ?? null) || $a['valor'] <= $min || $a['valor'] > $max) {
                    throw new DatosNoValidosException("El límite de la alerta debe ser un número entre $min y $max.", 'valor');
                }
                $v = (float) $a['valor'];
            }
            $limpias[$z] = [$z, $m, $cond, $v];
        }
        $ids = [];
        foreach (array_merge($fav, array_keys($limpias)) as $z) {
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

    // ==============================================================
    // Enlaces de un solo uso
    //   recuperar  fila en «token» (1 h), para cuentas que ya existen
    //   activar    la propia «invitacion» del trabajador (48 h)
    // ==============================================================

    private function crearEnlaceRecuperar(string $usuarioId): string {
        $token = self::tokenNuevo();
        // un enlace nuevo invalida el anterior
        $this->ejecutar("DELETE FROM {$this->p}token WHERE usuario_id = ? AND tipo = 'recuperar'", [$usuarioId]);
        $this->ejecutar("INSERT INTO {$this->p}token (token_hash, usuario_id, tipo, caduca) VALUES (?, ?, 'recuperar', ?)",
            [self::hashToken($token), $usuarioId, self::fechaBD($this->ahora() + self::RECUPERAR_MS)]);
        return $token;
    }

    // --------------------------------------------------------------
    // email, urlWeb --> solicitarRecuperacion()
    // Siempre responde igual (no revela qué correos existen). Si la cuenta
    // existe, envía un correo con el enlace (válido 1 h).
    // --------------------------------------------------------------
    public function solicitarRecuperacion(string $email, string $urlWeb): void {
        $m = strtolower(trim($email));
        if (!self::esEmail($m)) {
            throw new DatosNoValidosException('El correo no tiene un formato válido.', 'email');
        }
        $u = $this->fila("SELECT * FROM {$this->p}usuario WHERE email = ?", [$m]);
        if (!$u) {
            return;
        }
        $token = $this->crearEnlaceRecuperar($u['id']);
        $enlace = rtrim($urlWeb, '/') . '/#activar/' . $token;
        call_user_func($this->correo, $m, 'Respira: crea una contraseña nueva',
            "Hola, {$u['nombre']}:\n\nPara crear una contraseña nueva, abre este enlace (caduca en 1 hora y solo sirve una vez):\n\n$enlace\n\n"
            . "Si no lo has pedido tú, no hagas nada: tu contraseña no cambia.\n\nRespira · Nodia");
    }

    // --------------------------------------------------------------
    // token --> verEnlace() --> {estado: valido|caducado|no_valido, tipo, id, nombre, email, rol, horas}
    // Un enlace usado ya no existe: se borra al usarlo.
    // --------------------------------------------------------------
    public function verEnlace(string $token): array {
        $h = self::hashToken($token);
        $f = $this->fila("SELECT 'recuperar' AS tipo, t.caduca, u.id, u.nombre, u.email, u.rol
            FROM {$this->p}token t JOIN {$this->p}usuario u ON u.id = t.usuario_id
            WHERE t.token_hash = ? AND t.tipo = 'recuperar'", [$h])
            ?? $this->fila("SELECT 'activar' AS tipo, caduca, id, nombre, email, 'trabajador' AS rol
            FROM {$this->p}invitacion WHERE token_hash = ?", [$h]);
        if (!$f) {
            return ['estado' => 'no_valido'];
        }
        $estado = self::msDeFechaBD($f['caduca']) < $this->ahora() ? 'caducado' : 'valido';
        return ['estado' => $estado, 'tipo' => $f['tipo'], 'id' => $f['id'], 'nombre' => $f['nombre'],
            'email' => $f['email'], 'rol' => $f['rol'], 'horas' => $f['tipo'] === 'activar' ? 48 : 1];
    }

    // --------------------------------------------------------------
    // token, clave --> usarEnlace() --> email
    //   activar:   crea la cuenta (usuario + empleado) y borra la invitación
    //   recuperar: cambia la contraseña y cierra las sesiones abiertas
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
        $hash = password_hash($clave, PASSWORD_DEFAULT);
        $ahora = $this->ahora();
        $this->transaccion(function () use ($v, $hash, $ahora) {
            if ($v['tipo'] === 'activar') {
                $inv = $this->fila("SELECT * FROM {$this->p}invitacion WHERE id = ?", [$v['id']]);
                $this->ejecutar("INSERT INTO {$this->p}usuario (id, rol, nombre, email, clave_hash, alta) VALUES (?, 'trabajador', ?, ?, ?, ?)",
                    [$inv['id'], $inv['nombre'], $inv['email'], $hash, self::fechaBD($ahora)]);
                $this->ejecutar("INSERT INTO {$this->p}empleado (usuario_id, num_empleado, servicio) VALUES (?, ?, ?)",
                    [$inv['id'], $inv['num_empleado'], $inv['servicio']]);
                $this->ejecutar("DELETE FROM {$this->p}invitacion WHERE id = ?", [$inv['id']]);
            } else {
                $this->ejecutar("UPDATE {$this->p}usuario SET clave_hash = ? WHERE id = ?", [$hash, $v['id']]);
                // el enlace ya no sirve y las sesiones abiertas se cierran
                $this->ejecutar("DELETE FROM {$this->p}token WHERE usuario_id = ?", [$v['id']]);
            }
        });
        return $v['email'];
    }

    // ==============================================================
    // Trabajadores municipales (solo administración)
    // ==============================================================

    // --------------------------------------------------------------
    // trabajadores() --> [{id, nombre, email, num_empleado, servicio, alta,
    //                      estado: activo|pendiente|caducado}]
    //   activos: usuario + empleado · pendientes: invitaciones
    // --------------------------------------------------------------
    public function trabajadores(?array $quien): array {
        self::exigirRol($quien, ['admin']);
        $ahora = $this->ahora();
        $out = [];
        foreach ($this->filas("SELECT u.id, u.nombre, u.email, u.alta, e.num_empleado, e.servicio
            FROM {$this->p}usuario u JOIN {$this->p}empleado e ON e.usuario_id = u.id
            WHERE u.rol = 'trabajador'") as $t) {
            $out[] = ['id' => $t['id'], 'nombre' => $t['nombre'], 'email' => $t['email'], 'num_empleado' => $t['num_empleado'],
                'servicio' => $t['servicio'], 'alta' => self::isoDeBD($t['alta']), 'estado' => 'activo'];
        }
        foreach ($this->filas("SELECT * FROM {$this->p}invitacion") as $t) {
            $cad = self::msDeFechaBD($t['caduca']);
            $out[] = ['id' => $t['id'], 'nombre' => $t['nombre'], 'email' => $t['email'], 'num_empleado' => $t['num_empleado'],
                'servicio' => $t['servicio'], 'alta' => self::fechaISO($cad - self::ACTIVAR_MS),
                'estado' => $cad < $ahora ? 'caducado' : 'pendiente'];
        }
        usort($out, function ($a, $b) { return strcmp($a['id'], $b['id']); });
        return $out;
    }

    private function trabajador(?array $quien, string $id): array {
        return array_values(array_filter($this->trabajadores($quien), function ($x) use ($id) { return $x['id'] === $id; }))[0];
    }

    // --------------------------------------------------------------
    // Alta de un trabajador: queda como invitación hasta que él mismo
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
        } elseif ($this->fila("SELECT id FROM {$this->p}usuario WHERE email = ?", [$m])
            || $this->fila("SELECT id FROM {$this->p}invitacion WHERE email = ?", [$m])) {
            $e['email'] = 'Ya hay una cuenta con este correo.';
        }
        if ($emp === '') {
            $e['num_empleado'] = 'Escribe el número de empleado.';
        } elseif (!preg_match('/^\d{6}$/', $emp)) {
            $e['num_empleado'] = 'Son 6 cifras, por ejemplo 104233.';
        } elseif ($this->fila("SELECT usuario_id FROM {$this->p}empleado WHERE num_empleado = ?", [$emp])
            || $this->fila("SELECT id FROM {$this->p}invitacion WHERE num_empleado = ?", [$emp])) {
            $e['num_empleado'] = 'Este número de empleado ya tiene una cuenta.';
        }
        if (!in_array($servicio, self::SERVICIOS, true)) {
            $e['servicio'] = 'Elige el servicio.';
        }
        if ($e) {
            throw new ErroresDeCamposException($e);
        }
        return $this->transaccion(function () use ($quien, $n, $m, $emp, $servicio) {
            $max = max((int) substr((string) $this->valor("SELECT MAX(id) FROM {$this->p}usuario WHERE rol = 'trabajador'"), 2),
                (int) substr((string) $this->valor("SELECT MAX(id) FROM {$this->p}invitacion"), 2));
            $id = 'T-' . str_pad((string) ($max + 1), 4, '0', STR_PAD_LEFT);
            $token = self::tokenNuevo();
            $this->ejecutar("INSERT INTO {$this->p}invitacion (id, nombre, email, num_empleado, servicio, token_hash, caduca)
                VALUES (?, ?, ?, ?, ?, ?, ?)",
                [$id, $n, $m, $emp, $servicio, self::hashToken($token), self::fechaBD($this->ahora() + self::ACTIVAR_MS)]);
            return ['trabajador' => $this->trabajador($quien, $id), 'token' => $token];
        });
    }

    // --------------------------------------------------------------
    // Enlace de activación nuevo (el anterior deja de servir) --> {trabajador, token}
    // --------------------------------------------------------------
    public function reenviarActivacion(?array $quien, string $id): array {
        self::exigirRol($quien, ['admin']);
        if (!$this->fila("SELECT id FROM {$this->p}invitacion WHERE id = ?", [$id])) {
            if ($this->fila("SELECT id FROM {$this->p}usuario WHERE id = ? AND rol = 'trabajador'", [$id])) {
                throw new ConflictoException('Esta cuenta ya está activada.');
            }
            throw new NoEncontradoException("No existe el trabajador $id.");
        }
        $token = self::tokenNuevo();
        $this->ejecutar("UPDATE {$this->p}invitacion SET token_hash = ?, caduca = ? WHERE id = ?",
            [self::hashToken($token), self::fechaBD($this->ahora() + self::ACTIVAR_MS), $id]);
        return ['trabajador' => $this->trabajador($quien, $id), 'token' => $token];
    }

    // --------------------------------------------------------------
    // vecinos() --> [{id, nombre}]   (para el panel de préstamos)
    // --------------------------------------------------------------
    public function vecinos(?array $quien): array {
        self::exigirRol($quien, self::ROLES_PANEL);
        return $this->filas("SELECT id, nombre FROM {$this->p}usuario WHERE rol = 'vecino' ORDER BY id");
    }
}
