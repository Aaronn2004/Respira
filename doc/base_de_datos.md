# Base de datos de Respira

MariaDB 10.11 en Plesk. Todo lo que usan la app y la web está aquí:

| Fichero | Para qué |
|---|---|
| `servidor/bd/esquema.sql` | BD completa desde cero: **borra** las tablas y las vuelve a crear, con los datos base y los de demostración |
| `servidor/bd/actualizar_desde_v1.sql` | Pasa de la versión 1 (solo `zona`, `nodo`, `medicion`) a la actual **sin perder las mediciones válidas** |
| `servidor/bd/actualizar_seguimiento.sql` | Añade `favorito` y `alerta` a una BD importada con el `esquema.sql` del 10/11/2026 (la de Plesk). **No borra nada** y se puede repetir |

Todas las fechas se guardan en **UTC** (`DATETIME(3)`, con milisegundos). Los «días» de los puntos se cuentan en hora de España.

**17 tablas y 99 campos.** Reglas del diseño:

- **Todo lo que apunta a otra tabla es una clave ajena (FK).** Por ejemplo, `medicion.id_nodo` → `nodo.codigo`.
- **Ningún campo admite NULL** salvo tres, que se vacían a propósito:
  - `medicion.portador_id`: cuando nadie con sesión llevaba el sensor (o es simulada).
  - `prestamo.usuario_id` y `canje.usuario_id`: cuando un vecino borra su cuenta (sus datos personales desaparecen, pero el historial del sensor y del regalo se conserva).
- Lo que se puede calcular a partir de otros datos no se guarda dos veces (puntos de cada día, última medición de un sensor, UUID del iBeacon).

```
punto_reparto ─< prestamo >─ nodo ─< medicion >─ zona
                    │                   │
usuario ────────────┴───────────────────┘ (portador)
usuario ─< token · revision · canje >─ regalo · percepcion
usuario ─< favorito >─ zona          favorito ─── alerta (una por parque seguido)
usuario ─── empleado (trabajadores)       invitacion (trabajadores sin activar)
ayuntamiento (portada) · reglas_puntos (historial de reglas)
```

## 1. Mapa y sensores

**ayuntamiento**: los municipios que usan Respira (portada de la web). Campos: `id`, `nombre`, `comarca`, `desde`, `activo`. No se enlaza con nada porque hoy solo hay uno (Gandia); con un segundo municipio habría que añadir `ayuntamiento_id` a `zona` y `punto_reparto`.

**zona**: parques y barrios. `poligono` es JSON `[[lat,lng],…]` con `CHECK (JSON_VALID)`. `etiqueta_lat/lng` es dónde se escribe el nombre. Se cargan las 9 zonas de la web y el **Campus UPV** (rectángulo **aproximado**: compruébalo). Si un punto cae en un parque y en un barrio, cuenta para el parque.

**nodo**: cada sensor físico.

| Campo | Para qué |
|---|---|
| `codigo` PK | `RSP-XXXX` (`CHECK REGEXP`). El UUID que anuncia el Arduino es `RESPIRA-` + código: no hace falta guardarlo |
| `tipo` | `vol` (se presta), `trab` (de un servicio), `bici`, `bus` |
| `magnitudes` | SET de `o3`, `temp`, `ruido` |
| `estado` | `disponible`, `prestado`, `activo`, `mant`, `baja` |
| `asignado_a` | servicio o vehículo (nunca una persona). **«No asignado»** en los de voluntariado |
| `motivo` | **«Sin incidencias»**, o por qué está en mantenimiento o de baja |

«Hace cuánto mandó el último dato» sale de `medicion` (índice `id_nodo, tipo, fecha_medicion`).

**punto_reparto**: donde se prestan y se devuelven los sensores y se entregan los regalos.

## 2. Cuentas

**usuario**: todas las personas con cuenta. Campos: `id`, `rol`, `nombre`, `email` (UNIQUE), `clave_hash` (**bcrypt**), `alta`.

| `rol` | Dónde entra | `id` |
|---|---|---|
| `vecino` | app (se registra solo) | `U-XXXXX` |
| `trabajador` | app (lo da de alta administración) | `T-XXXX` |
| `admin`, `tecnico`, `reparto` | panel web | `A-XXXX` |

- Un vecino no puede registrarse sin aceptar las condiciones, así que su `alta` es también el momento en que las aceptó.
- No hay bloqueo por contraseñas fallidas (decisión del sprint): se puede reintentar sin límite.

**empleado**: los datos de empleado de cada trabajador (`usuario_id` PK y FK, `num_empleado` UNIQUE, `servicio`). Están aparte para que los vecinos no tengan esos campos vacíos.

**invitacion**: un trabajador dado de alta por administración que aún no ha puesto su contraseña. Guarda `id` (el `T-XXXX` que tendrá), `nombre`, `email`, `num_empleado`, `servicio`, el sha256 del enlace (`token_hash`) y `caduca` (48 h). Al activar la cuenta se crean su `usuario` y su `empleado`, y la invitación se borra. Pedir un enlace nuevo cambia el token y la caducidad.

**token**: sesiones abiertas y enlaces de «he olvidado mi contraseña» (`token_hash`, `usuario_id`, `tipo`, `caduca`). Solo se guarda el **sha256**: quien lea la BD no puede usarlos.

| `tipo` | Qué es | Dura |
|---|---|---|
| `app` | sesión de la app | 90 días |
| `web` | sesión del panel | 12 h |
| `recuperar` | enlace para crear una contraseña nueva | 1 h |

Al usar un enlace de recuperar se borra, y también las sesiones abiertas de esa cuenta.

## 3. Mediciones y préstamos

**medicion**: cada dato de un sensor.

| Campo | Para qué |
|---|---|
| `id_nodo` FK | el sensor (`nodo.codigo`) |
| `tipo` | `o3`, `temp` o `ruido` |
| `valor` | o3 en ppm, temperatura en °C, ruido en dB |
| `fecha_medicion` | cuándo lo oyó el móvil (UTC) |
| `lat`, `lng` | ubicación del móvil |
| `zona_id` FK | la calcula el servidor (punto en polígono) |
| `portador_id` FK | quien llevaba el sensor. **Lo decide el servidor** con la sesión: solo si tiene ese sensor prestado (o es de su servicio). Nunca se publica |
| `origen` | `real` o `simulado` (ver `doc/servidor.md` › Datos simulados); los simulados se borran con `DELETE FROM medicion WHERE origen = 'simulado'` |

Solo se guardan mediciones **válidas**. El servidor rechaza (400) las que no traen ubicación, las que caen fuera de todos los parques y barrios, y las que están fuera del rango del sensor.

**Repetidas:** una medición del mismo sensor, tipo y valor con menos de 10 s de diferencia es la misma lectura (dos móviles oyendo el mismo anuncio): se guarda una vez (409). La app sigue mandando `contador`, `precision` y `rssi`, pero no se guardan.

**prestamo**: un sensor prestado a un vecino.

| Campo | Para qué |
|---|---|
| `nodo` FK, `usuario_id` FK, `punto_reparto_id` FK | qué sensor, a quién y dónde se recogió |
| `inicio` | hora exacta de la recogida |
| `fecha_limite` | `inicio` + **10 días**: si se recoge el 10 de enero a las 12:00, hay que devolverlo antes del 20 de enero a las 12:00, en cualquier punto de reparto |
| `estado_devolucion` | `No devuelto` (en curso; si ya pasó la fecha límite, el panel lo marca «Fuera de plazo»), `Correcto` o `Defectuoso` (no se ha entregado en buenas condiciones: el sensor pasa a mantenimiento) |

## 4. Puntos, regalos y canjes

**reglas_puntos**: historial de reglas (`min_por_punto`, `mult_parque`, `max_dia`, `desde`). Vale la última con `desde` ≤ el día. Los cambios del panel se aplican **desde mañana**.

**Los puntos de cada día no se guardan.** El servidor los calcula cada vez con las mediciones que tienen a esa persona como portador, con las reglas de ese día:

- **Qué no cuenta:** entre dos datos seguidos, si hay más de 15 min sin datos, si va a más de 25 km/h o si está parado (menos de 20 m) más de 15 min, esos minutos no cuentan.
- **Cálculo:** puntos = ⌊min / N⌋ + ⌊min en parques / N⌋ · (mult − 1), con un máximo diario.

**revision**: solo los días sospechosos, que revisa administración. Un día se marca cuando llega al 90 % del máximo o el sensor pasa 1 h o más parado. Campos: `usuario_id`, `fecha` (UNIQUE juntos), `motivo` (por qué se marcó), `estado` y `razon`.

| `estado` | Qué significa | `razon` |
|---|---|---|
| `pend` | sin revisar (los puntos cuentan mientras tanto) | «Sin revisar» |
| `ok` | aprobado | «Aprobado» |
| `anulado` | no suma puntos | la explicación del administrador, que ve el voluntario |

**regalo**: catálogo (`coste`, `stock`, `activo`: retirar = 0).

**canje**: un regalo canjeado.
- `codigo` (`CJ-XXXX`), `usuario_id`, `regalo_id`.
- `coste`: se copia al canjear (si luego cambia el precio, el canje no cambia).
- `creado`, `caduca` (7 días).
- `estado`: `pendiente`, `entregado` o `caducado`.

Al canjear se resta una unidad del stock. Si el canje caduca sin recogerse, vuelven la unidad y los puntos.

**Saldo** = puntos de los días acabados que no están anulados − coste de los canjes no caducados. Se calcula cada vez, así nunca se descuadra.

## 5. Comunidad

**percepcion**: lo que notan los vecinos.
- `tipo`: `ruido`, `temp` o `aire`.
- `nivel`: de 1 a 3.
- `lat`, `lng`, `direccion`, `creada`.

Se ven 24 h y cada cuenta puede publicar como máximo 3 por hora. Se muestran **sin el autor**: solo se dice si es tuya. Solo quien la publicó puede cambiar el tipo y el nivel, o borrarla.

## 6. Seguimiento de parques (app)

**favorito**: los parques (o barrios) que sigue cada cuenta.
- Clave: `usuario_id` + `zona_id` (las dos son FK).
- `creado`: desde cuándo lo sigue (el orden de la lista en la app).

**alerta**: como mucho una por parque seguido.
- Clave y FK: `usuario_id` + `zona_id` → `favorito` (si dejas de seguir el parque, se borra su alerta).
- `magnitud`: `o3`, `temp` o `ruido`.
- `condicion`: `bajo`, `medio`, `alto` o `custom` (un límite propio).
- `valor`: el límite; solo cuenta si `condicion` = `custom` (si no, 0, para no usar NULL).

La app manda siempre la lista completa (`PUT /yo/seguimiento`) y el servidor la sustituye. Sin sesión, los parques se guardan en el móvil y se suman a la cuenta al iniciar sesión. Las alertas las sigue comprobando el móvil (sin notificaciones push el servidor no podría avisar).

## Lo que NO está en la BD (y por qué)

- **Quién hizo cada gestión** (quién prestó o recogió un sensor, entregó un regalo o revisó un día): el panel no lo enseña. Si el ayuntamiento lo pide, se añadiría una tabla de historial (`quien`, `que`, `cuando`).
- **Estadísticas de cada recorrido** (distancia, duración, parques por los que pasó): se calculan con las mediciones del día al pedirlas (`Puntos::calcularDia`), igual que los puntos.
- **Recorrido en curso** («Mi sensor»): es el estado del móvil. Lo que importa, las mediciones, sí va al servidor.

## Datos de demostración

Están al final de `esquema.sql` (entre `@@DEMO:INICIO` y `@@DEMO:FIN`). Si no los quieres, bórralos antes de importar. Las fechas son relativas al momento en que se importa.

| Cuenta | Contraseña | Dónde |
|---|---|---|
| lucia@correo.es (U-48213, 340 puntos, sensor RSP-4F7K) | Respira2026 | app |
| marcos@correo.es (U-11902) | Marcos2026 | app |
| ana.v@correo.es (U-30577) | AnaV2026 | app |
| psanz@ayto.es (T-0001) | Trabajo2026! | app (trabajador) |
| admin@ayto.es / tecnico@ayto.es / reparto@ayto.es | Admin2026! / Tecnico2026! / Reparto2026! | panel web |

Además incluye:
- 11 sensores y 3 préstamos (el de Marcos, fuera de plazo).
- Elena (T-0002): trabajadora invitada con el enlace caducado.
- Recorridos de días pasados (una lectura por minuto) que dan los puntos: Lucía 340, Marcos 120, Ana 127.
- 3 días en revisión: 1 aprobado y 2 pendientes.
- 3 canjes: CJ-8R2T (válido), CJ-2K9L (caducado) y CJ-5P7Q (entregado).
- 8 percepciones, que desaparecen del mapa a las 24 h de importar.
- Lucía sigue el Parque de la Ribera y el Parque Central, con una alerta de ruido en nivel Alto en el Central.

**Antes de usarlo de verdad, cambia esas contraseñas**: son públicas porque están en el repositorio.
