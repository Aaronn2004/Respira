# Servidor de Respira (API REST + lógica)

PHP 8 + PDO. Tres piezas, como en el Sprint 0: API (HTTP ↔ lógica), lógica del negocio (reglas, SQL) y base de datos.

```
servidor/
├── api/index.php            API REST (rutas, JSON, códigos HTTP, sesión)
├── api/config.example.php   plantilla de config.php (NO se sube a git)
├── logica/Base.php          errores del dominio, fechas, ayudas de SQL
├── logica/Logica.php        mediciones, zonas, series, sensores (público)
├── logica/Cuentas.php       registro, sesiones, enlaces, alta de trabajadores
├── logica/Puntos.php        puntos de cada día (puntos_dia), saldo, regalos, canjes; reglas fijas
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
- Cinco fallos para el mismo correo bloquean el acceso durante 15 minutos. La respuesta 423 incluye la fecha de desbloqueo en `hasta`.
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
| `GET /percepciones` | Las de las últimas 24 h (con sesión, `mia: true` en las tuyas). En modo demo incluye ejemplos con `simulado: true`. |
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
| `GET /yo` | Perfil, puntos, «hoy», historial, canjes, reglas, préstamo en curso (`desde`, `hasta` = fecha límite) y `seguimiento` (`favoritos`, `alertas`). «hoy» y cada día del historial llevan `recorrido`: `inicio`, `fin`, `duracion_min`, `distancia_m`, `lecturas` y `parques` |
| `PUT /yo/clave` `{clave}` | Cambia la contraseña inicial del trabajador. Devuelve una sesión nueva e invalida la provisional. |
| `PUT /yo/seguimiento` `{favoritos: [zona], alertas: [{zona, magnitud, condicion, valor}]}` | Guarda en la cuenta los parques que sigues y sus alertas por magnitud (sustituye la lista; máximo 50 parques). |
| `POST /yo/alertas/emitidas` `{zona, magnitud, valor, momento, origen}` | Registra un aviso de una alerta configurada a partir de una lectura reciente. El historial publica el dato sin identificar a la cuenta. |
| `DELETE /yo` | Borrar la cuenta (no con un sensor prestado) |
| `POST /yo/sensor` `{codigo}` | ¿Puedo usar este sensor? (préstamo a mi nombre o sensor de mi servicio) |
| `GET /yo/percepciones` | Mis percepciones (30 días) |
| `POST /percepciones` · `PUT /percepciones/{id}` · `DELETE /percepciones/{id}` | Publicar, editar (tipo y nivel), borrar. 429 si van 3 en una hora |
| `POST /canjes` `{regalo}` | Canjear. 409 sin puntos o agotado |

**Panel municipal (web, con sesión)**: entre paréntesis, los roles que pueden.

| Método y ruta | Qué hace |
|---|---|
| `GET /panel/nodos` (todos) · `POST` · `PUT /panel/nodos/{c}` (admin, técnico) · `DELETE /panel/nodos/{c}` `{motivo}` (admin) | Flota, alta, edición/mantenimiento, baja. El alta admite `ayuntamiento` (obligatorio solo si hay varios activos) |
| `GET /panel/prestamos` · `POST /panel/prestamos` `{usuario, codigo, punto}` · `POST /panel/devoluciones` `{codigo, estado: Correcto\|Defectuoso}` (todos) | Préstamos: plazo de 10 días desde la recogida (`fecha_limite`, `fuera_de_plazo`) |
| `GET /panel/vecinos` (todos) | IDs de vecinos para prestar |
| `GET /panel/reglas` (admin) | Reglas de puntos (fijas en el código; solo se consultan) |
| `POST` · `PUT` · `DELETE /panel/regalos[/{id}]` (admin) | Catálogo |
| `GET /panel/canjes` (admin) · `GET /panel/canjes/{c}` · `POST /panel/canjes/{c}/entrega` (admin, reparto) | Canjes |
| `GET` · `POST /panel/trabajadores` (admin) | Alta de trabajadores: `{nombre, email, num_empleado, servicio, clave, ayuntamiento?}`. La clave inicial caduca a las 48 h. El primer acceso devuelve `requiere_cambio_clave: true` en el perfil y una sesión provisional. |

**Qué devuelven las rutas del mapa**

- `GET /zonas` devuelve una media móvil de los últimos 60 minutos por magnitud. Para una lectura antigua muestra la media de la hora que acabó en esa lectura y su fecha. `detalle` contiene `valor`, `estado` (`reciente`, `antiguo`, `estimado`, `sin_datos`), `momento` en UTC y `origen` (`real`, `simulado`, `estimado`). Si faltan lecturas de una magnitud, se puede estimar mediante una media ponderada por el inverso de la distancia. Se requieren al menos dos zonas del mismo ayuntamiento con datos recientes del mismo origen a 1,5 km o menos entre centros. Tienen prioridad las lecturas reales. Las estimaciones quedan fuera de `mejor` (mínimo de la última hora) y de otras estimaciones. `geometria` conserva el GeoJSON y `poligonos` ofrece sus partes y anillos en orden [latitud, longitud].
- `GET /zonas/{id}/serie` devuelve la media de cada tramo en `valores`, su `origen`, la duración `paso_ms` y `aprox` (estimaciones lineales para huecos internos de hasta 2 h). `resumen` contiene mínimo, media ponderada por número de lecturas, máximo y horas sin datos. Los estadísticos usan lecturas sin interpolar. Un tramo con lecturas reales usa solo esas lecturas. `eventos` incluye los periodos en nivel Alto (o Medio si no hay Alto), los picos y los avisos emitidos, cada uno con nivel y origen.
- El periodo personalizado `rango` admite fechas UTC de inicio y fin, ambas incluidas, hasta 90 días y sin fechas futuras. Las comparaciones de 24 h, 7 d y 30 d usan la misma `referencia` en UTC, redondeada al minuto inferior. Se admite un adelanto de hasta 60 segundos por desajuste de relojes. Las lecturas posteriores a la hora del servidor quedan fuera.

Errores: `{"error": "mensaje para la persona", "errores": {campo: mensaje}}` con 400, 401, 403, 404, 409, 429 o 500 (sin detalles internos).

## Datos simulados

Mientras no haya sensores por la calle, el servidor **simula** mediciones para que el mapa, las gráficas, las alertas y los picos tengan datos (`logica/Simulador.php`).

- Se guardan en `medicion` con `origen = 'simulado'`, de los nodos de demostración (o de `RSP-SIM0` si no los hay).
- Valores realistas para Gandia en noviembre y **deterministas** (mismo instante, mismo valor): ozono con campana de tarde y días de episodio, temperatura con su ciclo diario, ruido con horas punta.
- Huecos a propósito: algunos de 40–100 min (se ven aproximados) y, en parques, noches de 2–5 h sin nadie (se ven en gris).
- **Oeste** y **Marenys** carecen de lecturas propias. **Estación** y **Ermita** llevan 3 h de retraso para mostrar el estado «Antiguo».
- No hace falta tarea programada: cada petición a la API rellena hasta «ahora» (como mucho una vez cada 5 min) y el historial hacia atrás, 5 días por petición, hasta 30 días. Un bloqueo evita que varias peticiones completen el mismo tramo simultáneamente. Tras subirlo, las primeras peticiones tardan un poco más.
- **Los datos reales mandan**: si una magnitud tiene mediciones reales en las últimas 24 h, se usan esas. En las gráficas se elige el dato real por tramo.
- Se apaga con `'simulacion' => false` en `config.php`. Para borrar lo simulado: `DELETE FROM medicion WHERE origen = 'simulado';`.

## Subirlo a Plesk

Ya tienes la BD `respira` (usuario `respira`) y `httpdocs/respira/` del paso anterior.

1. **Base de datos.** Elige una:
   - **Empezar de cero** (recomendado si aún no hay mediciones reales): en phpMyAdmin, BD `respira` › **Importar** › `servidor/bd/esquema.sql`. Borra lo que hubiera.
   - **Conservar las mediciones**: **Importar** › `servidor/bd/actualizar_desde_v1.sql`. Solo una vez.
   - **Ya tienes el `esquema.sql` del 10/11/2026** (lo normal): **Importar** › `servidor/bd/actualizar_plesk.sql` (se puede repetir sin problema).

   Para actualizar una base existente, aplica después `servidor/bd/actualizar_cuentas_comunidad.sql` y `servidor/bd/actualizar_zonas_costeras.sql`. Haz una copia de seguridad antes de migrar. Si la restricción de nombres duplicados falla, resuelve esas cuentas con sus titulares. Los scripts conservan las cuentas y el historial. Deben quedar 18 tablas.
2. **Archivos.** Sube el ZIP `_plesk/respira_plesk.zip` a `httpdocs`, **Extract Files** marcando que reemplace los existentes, y borra el ZIP. No trae `config.php`: el tuyo se queda como está.
3. **Correo (opcional).** Para que lleguen los correos (contraseña nueva), Plesk tiene que poder enviar desde el dominio.
4. **Prueba.**
   - `…/respira/api/index.php/estado` → `ok`.
   - `…/respira/web/#acceso` → entra con `admin@ayto.es` / `Admin2026!`.
   - En la app, entra con `lucia@correo.es` / `Respira2026`.

## Tests

En cualquier equipo con PHP 8 y `pdo_sqlite`. No tocan Plesk: usan SQLite en memoria con las mismas tablas de `esquema.sql`, que se traducen solas.

```bash
php servidor/tests/LogicaTest.php          # 15 · mediciones, zonas, series
php servidor/tests/CuentasTest.php         # 14 · registro, sesión, enlaces, alta de trabajadores, seguimiento
php servidor/tests/PuntosTest.php          # 12 · cálculo de puntos, puntos_dia, canjes, reglas
php servidor/tests/ComunidadFlotaTest.php  # 13 · percepciones, préstamos, sensores
php servidor/tests/ApiTest.php             # 24 · HTTP de verdad (levanta php -S él solo)
php servidor/tests/SimuladorTest.php       # 5 · simulación, aproximación de huecos, alertas y picos
```

## Pendiente

- **Notificaciones push (FCM).** Las alertas solo saltan con la app abierta.
- **Correo.** Depende de que Plesk pueda enviarlo.
- **Límite de peticiones por IP** en `/sesiones` y `/mediciones`: hoy se pueden probar contraseñas sin límite.
- **Contraseñas de demostración.** Cámbialas antes de un uso real.
