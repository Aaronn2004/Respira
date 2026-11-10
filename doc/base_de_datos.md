# Base de datos de Respira

MariaDB 10.11 en Plesk. Todo lo que usan la app y la web está aquí:

| Fichero | Para qué |
|---|---|
| `servidor/bd/esquema.sql` | BD completa desde cero: **borra** las tablas y las vuelve a crear, con los datos base y los de demostración |
| `servidor/bd/actualizar_v2.sql` | Pasa de la versión 1 (solo `zona`, `nodo`, `medicion`) a la 2 **sin perder las mediciones** |

Todas las fechas se guardan en **UTC** (`DATETIME(3)`, con milisegundos). Los «días» de los puntos se cuentan en hora de España.

```
ayuntamiento ─< punto_reparto ─< usuario (reparto)
zona ─< medicion >─ nodo ─< prestamo >─ usuario
                    medicion >─ usuario (portador)
usuario ─< sesion · token_cuenta · jornada · canje >─ regalo · percepcion
reglas_puntos (historial de reglas)
```

## 1. Mapa y sensores

**ayuntamiento**: los municipios que usan Respira (portada de la web). Campos: `id`, `nombre`, `comarca`, `desde`, `activo`.

**zona**: parques y barrios. `poligono` es JSON `[[lat,lng],…]` con `CHECK (JSON_VALID)`. `etiqueta_lat/lng` es dónde se escribe el nombre. Se cargan las 9 zonas de la web y el **Campus UPV** (rectángulo **aproximado**: compruébalo). Si un punto cae en un parque y en un barrio, cuenta para el parque.

**nodo**: cada sensor físico.

| Campo | Para qué |
|---|---|
| `codigo` PK | `RSP-XXXX` (`CHECK REGEXP`) |
| `uuid_beacon` UNIQUE | `RESPIRA-` + código: lo que anuncia el Arduino |
| `tipo` | `vol` (se presta), `trab` (de un servicio), `bici`, `bus` |
| `magnitudes` | SET de `o3`, `temp`, `ruido` |
| `estado` | `disponible`, `prestado`, `activo`, `mant`, `baja` |
| `asignado_a` | servicio o vehículo, **nunca** una persona |
| `bateria` | 0–100; NULL = a corriente |
| `ultima_medicion` | la pone el servidor con cada dato |
| `motivo_mant`, `baja`, `motivo_baja` | mantenimiento y baja (las mediciones se conservan) |

**punto_reparto**: donde se prestan los sensores y se entregan los regalos.

## 2. Cuentas

**usuario**: una sola tabla para todas las personas:

| `rol` | Dónde entra | `id` |
|---|---|---|
| `vecino` | app (se registra solo) | `U-XXXXX` |
| `trabajador` | app (lo da de alta administración) | `T-XXXX` |
| `admin`, `tecnico`, `reparto` | panel web | `A-XXXX` |

Campos:
- `nombre`: en los vecinos, el nombre de usuario.
- `email`: UNIQUE.
- `clave_hash`: **bcrypt**; NULL = pendiente de activar.
- `estado`.
- `num_empleado` y `servicio`: solo trabajadores.
- `punto_reparto_id`: solo rol reparto.
- `intentos_fallidos` y `bloqueado_hasta`: tras 5 fallos, bloqueo de 15 min.
- `acepta_condiciones`, `alta`, `alta_por`.

**sesion**: sesiones abiertas. Solo se guarda el **sha256 del token**: quien lea la BD no puede usarlo. En la app duran 90 días; en la web, 12 h.

**token_cuenta**: enlaces de un solo uso. Pedir uno nuevo invalida el anterior. También se guardan con hash.
- `activar`: trabajadores, caduca en 48 h.
- `recuperar`: contraseña nueva, caduca en 1 h.

## 3. Mediciones y préstamos

**medicion**: cada dato de un sensor.

| Campo | Para qué |
|---|---|
| `nodo`, `magnitud`, `valor`, `contador` | el dato y el contador del iBeacon |
| `medida_en`, `recibida_en` | cuándo lo oyó el móvil y cuándo llegó (los de sin red llegan tarde) |
| `lat`, `lng`, `precision_m` | ubicación del móvil |
| `zona_id` | la calcula el servidor (punto en polígono) |
| `portador_id` | quien llevaba el sensor. **Lo decide el servidor** con la sesión: solo si tiene ese sensor prestado (o es de su servicio). Nunca se publica |
| `rssi`, `origen`, `valida`, `motivo_descarte` | diagnóstico; fuera de rango = `valida 0`. `origen = 'simulado'`: dato inventado por el servidor mientras no hay sensores (ver `doc/servidor.md` › Datos simulados); se borran con `DELETE FROM medicion WHERE origen = 'simulado'` |

**prestamo**: un préstamo de un sensor a un vecino. Más de 24 h sin devolver = «pendiente de devolución» en el panel.
- `nodo`, `usuario_id`, `punto_reparto_id`.
- `inicio`, `fin` (NULL = en curso).
- `estado_devolucion`: `Correcto`, `Batería baja` o `Dañado`.
- `prestado_por`, `recibido_por`.

## 4. Puntos, regalos y canjes

**reglas_puntos**: historial de reglas (`min_por_punto`, `mult_parque`, `max_dia`, `desde`). Vale la última con `desde` ≤ el día. Los cambios del panel se aplican **desde mañana**.

**jornada**: un día con sensor de una persona. **La calcula el servidor** con sus mediciones la primera vez que alguien pide datos después de acabar el día, así que no hace falta una tarea programada.

- **Qué no cuenta:** entre dos datos seguidos, si hay más de 15 min sin datos, si va a más de 25 km/h o si está parado (menos de 20 m) más de 15 min, esos minutos no cuentan.
- **Cálculo:** puntos = ⌊min / N⌋ + ⌊min en parques / N⌋ · (mult − 1), con un máximo diario.
- **Revisión:** cada día empieza como `auto`. Pasa a `pend` si es sospechoso (llega al 90 % del máximo o pasa 1 h o más parado). Administración lo **aprueba** (`ok`) o lo **anula** (`anulado`, con una `razon_anulacion` que ve el voluntario).

**regalo**: catálogo (`coste`, `stock`, `activo`: retirar = 0).

**canje**: un regalo canjeado.
- `codigo` (`CJ-XXXX`), `usuario_id`, `regalo_id`.
- `coste`: se copia al canjear.
- `creado`, `caduca` (7 días).
- `estado`: `pendiente`, `entregado` o `caducado`.
- `entregado_en`, `entregado_por`.

Al canjear se resta una unidad del stock. Si el canje caduca sin recogerse, vuelven la unidad y los puntos.

**Saldo** = puntos de las jornadas no anuladas − coste de los canjes no caducados. No se guarda: se calcula cada vez, así nunca se descuadra.

## 5. Comunidad

**percepcion**: lo que notan los vecinos.
- `tipo`: `ruido`, `temp` o `aire`.
- `nivel`: de 1 a 3.
- `lat`, `lng`, `direccion`, `creada`, `editada`.

Se ven 24 h y cada cuenta puede publicar como máximo 3 por hora. Se muestran **sin el autor**: solo se dice si es tuya. Solo quien la publicó puede cambiar el tipo y el nivel, o borrarla.

## Lo que NO está en la BD (y por qué)

- **Favoritos y alertas** de la app: se guardan en el móvil. Sin un servicio de notificaciones push (FCM), el servidor no podría avisar de nada. La app comprueba las alertas con los datos reales cada vez que lee las zonas.
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
- 11 sensores y 3 préstamos.
- Días con puntos, 2 de ellos para revisar.
- 3 canjes: CJ-8R2T (válido), CJ-2K9L (caducado) y CJ-5P7Q (entregado).
- 8 percepciones, que desaparecen del mapa a las 24 h de importar.

**Antes de usarlo de verdad, cambia esas contraseñas**: son públicas porque están en el repositorio.
