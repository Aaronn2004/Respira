# Servidor de Respira (API REST + lógica)

PHP 8 + PDO. Tres piezas, como en el Sprint 0: API (HTTP ↔ lógica), lógica del negocio (reglas, SQL) y base de datos.

```
servidor/
├── api/index.php            API REST (rutas, JSON, códigos HTTP, sesión)
├── api/config.example.php   plantilla de config.php (NO se sube a git)
├── logica/Base.php          errores del dominio, fechas, ayudas de SQL
├── logica/Logica.php        mediciones, zonas, series, sensores (público)
├── logica/Cuentas.php       registro, sesiones, enlaces, trabajadores (invitaciones)
├── logica/Puntos.php        cálculo de puntos, saldo, regalos, canjes, revisiones, reglas
├── logica/Comunidad.php     percepciones de los vecinos
├── logica/Flota.php         sensores del panel, préstamos, devoluciones, sensor de la app
├── logica/Simulador.php     datos simulados mientras no haya sensores reales (se puede apagar)
├── bd/esquema.sql           BD completa (desde cero, con datos de demostración)
├── bd/actualizar_desde_v1.sql de la versión 1 a la actual sin perder las mediciones válidas
└── tests/                   tests automáticos (SQLite en memoria; no tocan Plesk)
```

## Sesión y seguridad

- `POST /sesiones` devuelve un **token** (64 caracteres). El cliente lo manda en cada petición en la cabecera `X-Respira-Token` (también vale `Authorization: Bearer …`).
- En la BD solo se guarda el **hash** del token y de las contraseñas (bcrypt).
- No hay bloqueo por contraseñas fallidas (decisión del sprint).
- Cada ruta comprueba el **rol** en el servidor: la web no puede saltárselo.
- La app entra con cuentas `vecino`/`trabajador`; el panel, con `admin`/`tecnico`/`reparto`.
- `POST /mediciones` sigue abierto (los sensores no tienen cuenta), pero **los puntos solo se dan** si la medición llega con la sesión de quien tiene ese sensor prestado.

## Rutas

Base: `https://ablabla.upv.edu.es/respira/api/index.php` (si en tu Plesk falla la parte `/index.php/…`, usa `index.php?ruta=/zonas`). Si el hosting no deja pasar `PUT`/`DELETE`, se puede mandar `POST` con la cabecera `X-HTTP-Method-Override`.

**Públicas**

| Método y ruta | Qué hace |
|---|---|
| `GET /estado` | Comprueba que la API y la BD responden |
| `POST /mediciones` | Una medición, o `{"mediciones":[…]}` en lote. 201 · 409 repetida (mismo sensor, tipo y valor en menos de 10 s) · 400 sin ubicación, fuera de las zonas o fuera de rango |
| `GET /zonas` · `GET /zonas/{id}/serie?magnitud=&periodo=24h\|7d\|30d\|rango` | Mapa y gráficas |
| `GET /nodos` · `GET /nodos/{codigo}` | Sensores con su último dato |
| `GET /percepciones` | Las de las últimas 24 h (con sesión, `mia: true` en las tuyas) |
| `GET /regalos` · `GET /ayuntamientos` · `GET /puntos-reparto` | Catálogo y datos fijos |

**Cuentas**

| Método y ruta | Qué hace |
|---|---|
| `POST /sesiones` `{email, clave, cliente: app\|web}` | Inicia sesión → `{token, usuario}`. 401 correo o contraseña mal, 403 cliente equivocado |
| `DELETE /sesiones` | Cierra la sesión |
| `POST /usuarios` `{nombre, email, clave, acepta}` | Registro de vecinos (app). 400 con `errores` por campo |
| `POST /recuperaciones` `{email}` | Manda por correo un enlace para cambiar la contraseña. Siempre 202 |
| `GET /enlaces/{token}` · `POST /enlaces/{token}` `{clave}` | Ver y usar un enlace (activar cuenta o contraseña nueva) |

**Mi cuenta (app, con sesión)**

| Método y ruta | Qué hace |
|---|---|
| `GET /yo` | Perfil, puntos, «hoy», historial, canjes, reglas y préstamo en curso (`desde`, `hasta` = fecha límite) |
| `DELETE /yo` | Borrar la cuenta (no con un sensor prestado) |
| `POST /yo/sensor` `{codigo}` | ¿Puedo usar este sensor? (préstamo a mi nombre o sensor de mi servicio) |
| `GET /yo/percepciones` | Mis percepciones (30 días) |
| `POST /percepciones` · `PUT /percepciones/{id}` · `DELETE /percepciones/{id}` | Publicar, editar (tipo y nivel), borrar. 429 si van 3 en una hora |
| `POST /canjes` `{regalo}` | Canjear. 409 sin puntos o agotado |

**Panel municipal (web, con sesión)**: entre paréntesis, los roles que pueden.

| Método y ruta | Qué hace |
|---|---|
| `GET /panel/nodos` (todos) · `POST` · `PUT /panel/nodos/{c}` (admin, técnico) · `DELETE /panel/nodos/{c}` `{motivo}` (admin) | Flota, alta, edición/mantenimiento, baja |
| `GET /panel/prestamos` · `POST /panel/prestamos` `{usuario, codigo, punto}` · `POST /panel/devoluciones` `{codigo, estado: Correcto\|Defectuoso}` (todos) | Préstamos: plazo de 10 días desde la recogida (`fecha_limite`, `fuera_de_plazo`) |
| `GET /panel/vecinos` (todos) | IDs de vecinos para prestar |
| `GET` · `PUT /panel/reglas` (admin) | Reglas de puntos (desde mañana) |
| `GET /panel/revisiones` · `POST /panel/revisiones/{id}` `{decision: aprobar\|anular, razon}` (admin) | Días sospechosos |
| `POST` · `PUT` · `DELETE /panel/regalos[/{id}]` (admin) | Catálogo |
| `GET /panel/canjes` (admin) · `GET /panel/canjes/{c}` · `POST /panel/canjes/{c}/entrega` (admin, reparto) | Canjes |
| `GET` · `POST /panel/trabajadores` · `POST /panel/trabajadores/{id}/enlace` (admin) | Alta de trabajadores (enlace de activación por correo y en pantalla) |

**Qué devuelven las rutas del mapa**

- `GET /zonas`: por zona, `o3`, `temp`, `ruido` (media de la última hora), `hace_min`, `n`, `media_24h {o3, temp, ruido}`, `mejor {o3, temp, ruido}` (la medida más baja de esa última hora; la portada la enseña en los parques) y `simulado` (true si lo que se ve son datos simulados).
- `GET /zonas/{id}/serie`: `valores` (media de cada tramo, `null` = nadie midió), `aprox` (huecos cortos de hasta 2 h con dato a los dos lados, rellenados en línea recta; **no cuentan** en mínimo, media y máximo) y `eventos`:
  - `alertas`: ratos seguidos en nivel Alto (si no hubo ninguno, en Medio) con `desde`, `hasta`, `max`, `nivel` (máx. 10).
  - `picos`: los 3 valores más altos medidos, en tramos no seguidos, con su hora exacta (`momento`, `valor`).

Errores: `{"error": "mensaje para la persona", "errores": {campo: mensaje}}` con 400, 401, 403, 404, 409, 429 o 500 (sin detalles internos).

## Datos simulados

Mientras no haya sensores por la calle, el servidor **simula** mediciones para que el mapa, las gráficas, las alertas y los picos tengan datos (`logica/Simulador.php`).

- Se guardan en `medicion` con `origen = 'simulado'`, de los nodos de demostración (o de `RSP-SIM0` si no los hay).
- Valores realistas para Gandia en noviembre y **deterministas** (mismo instante, mismo valor): ozono con campana de tarde y días de episodio, temperatura con su ciclo diario, ruido con horas punta.
- Huecos a propósito: algunos de 40–100 min (se ven aproximados) y, en parques, noches de 2–5 h sin nadie (se ven en gris).
- **Oeste** se queda siempre «Sin datos» y **Estación** va 3 h atrasada («Antiguo»), para que se vean los tres estados.
- No hace falta tarea programada: cada petición a la API rellena hasta «ahora» (como mucho una vez cada 5 min) y el historial hacia atrás, 5 días por petición, hasta 30 días. Tras subirlo, las primeras peticiones tardan un poco más.
- **Los datos reales mandan**: si una zona tiene mediciones reales en las últimas 24 h, se ven esas; en las gráficas, cada tramo con dato real usa el real.
- Se apaga con `'simulacion' => false` en `config.php`. Para borrar lo simulado: `DELETE FROM medicion WHERE origen = 'simulado';`.

## Subirlo a Plesk

Ya tienes la BD `respira` (usuario `respira`) y `httpdocs/respira/` del paso anterior.

1. **Base de datos.** Elige una:
   - **Empezar de cero** (recomendado si aún no hay mediciones reales): en phpMyAdmin, BD `respira` › **Importar** › `servidor/bd/esquema.sql`. Borra lo que hubiera.
   - **Conservar las mediciones**: **Importar** › `servidor/bd/actualizar_desde_v1.sql`. Solo una vez.

   Deben quedar 15 tablas.
2. **Archivos.** Sube el ZIP `_plesk/respira_plesk.zip` a `httpdocs`, **Extract Files** marcando que reemplace los existentes, y borra el ZIP. No trae `config.php`: el tuyo se queda como está.
3. **Correo (opcional).** Para que lleguen los correos (contraseña nueva, activación de trabajadores), Plesk tiene que poder enviar desde el dominio. Si no llegan, administración ve el enlace de activación en el panel («Abrir correo de prueba») y se lo puede dar al trabajador en mano.
4. **Prueba.**
   - `…/respira/api/index.php/estado` → `ok`.
   - `…/respira/web/#acceso` → entra con `admin@ayto.es` / `Admin2026!`.
   - En la app, entra con `lucia@correo.es` / `Respira2026`.

## Tests

En cualquier equipo con PHP 8 y `pdo_sqlite`. No tocan Plesk: usan SQLite en memoria con las mismas tablas de `esquema.sql`, que se traducen solas.

```bash
php servidor/tests/LogicaTest.php          # 15 · mediciones, zonas, series
php servidor/tests/CuentasTest.php         # 12 · registro, sesión, enlaces, invitaciones de trabajadores
php servidor/tests/PuntosTest.php          # 11 · cálculo de puntos, canjes, revisiones, reglas
php servidor/tests/ComunidadFlotaTest.php  # 13 · percepciones, préstamos, sensores
php servidor/tests/ApiTest.php             # 23 · HTTP de verdad (levanta php -S él solo)
php servidor/tests/SimuladorTest.php       # 5 · simulación, aproximación de huecos, alertas y picos
```

## Pendiente

- **Notificaciones push (FCM).** Las alertas solo saltan con la app abierta.
- **Correo.** Depende de que Plesk pueda enviarlo.
- **Límite de peticiones por IP** en `/sesiones` y `/mediciones`: hoy se pueden probar contraseñas sin límite.
- **Contraseñas de demostración.** Cámbialas antes de un uso real.
