-- ==============================================================
-- Fichero:     esquema.sql  (MariaDB 10.11 / MySQL 8 - Plesk)
-- Descripción: Base de datos COMPLETA de Respira: todo lo que usan la
--              app y la web (zonas, sensores, mediciones, cuentas,
--              sesiones, préstamos, puntos, regalos, canjes,
--              percepciones de los vecinos y trabajadores municipales).
--              Se ejecuta UNA vez desde phpMyAdmin (pestaña «Importar»)
--              en la base de datos «respira».
--
--              OJO: BORRA y vuelve a crear todas las tablas (también las
--              mediciones). Para pasar de la versión 1 (solo zona, nodo
--              y medicion) SIN perder mediciones, usa actualizar_desde_v1.sql.
--
--              Al final carga los DATOS DE DEMOSTRACIÓN (las mismas
--              cuentas y contraseñas de prueba que la app y la web).
--              Las contraseñas se guardan con hash bcrypt, nunca en claro.
-- Diseño:      doc/base_de_datos.md
-- Autor:       Aaron Blasco Blay
-- Fecha:       16/11/2026
-- Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
-- ==============================================================
SET NAMES utf8mb4;
SET time_zone = '+00:00';   -- todas las fechas se guardan en UTC
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS alerta_emitida;
DROP TABLE IF EXISTS alerta;
DROP TABLE IF EXISTS favorito;
DROP TABLE IF EXISTS canje;
DROP TABLE IF EXISTS regalo;
DROP TABLE IF EXISTS puntos_dia;
DROP TABLE IF EXISTS revision;
DROP TABLE IF EXISTS jornada;
DROP TABLE IF EXISTS reglas_puntos;
DROP TABLE IF EXISTS percepcion_envio;
DROP TABLE IF EXISTS percepcion;
DROP TABLE IF EXISTS prestamo;
DROP TABLE IF EXISTS medicion;
DROP TABLE IF EXISTS token;
DROP TABLE IF EXISTS invitacion;
DROP TABLE IF EXISTS empleado;
DROP TABLE IF EXISTS token_cuenta;
DROP TABLE IF EXISTS sesion;
DROP TABLE IF EXISTS acceso_intento;
DROP TABLE IF EXISTS usuario;
DROP TABLE IF EXISTS punto_reparto;
DROP TABLE IF EXISTS ayuntamiento;
DROP TABLE IF EXISTS nodo;
DROP TABLE IF EXISTS zona;

SET FOREIGN_KEY_CHECKS = 1;

-- @@TABLAS:INICIO
-- Reglas del diseño:
--   - Todo campo que apunta a otra tabla es una FOREIGN KEY.
--   - Ningún campo admite NULL salvo tres, que se vacían a propósito al
--     borrar una cuenta (RGPD) o cuando nadie lleva el sensor:
--     medicion.portador_id, prestamo.usuario_id y canje.usuario_id.
-- ==============================================================
-- 1. MAPA Y SENSORES
-- ==============================================================

-- Ayuntamientos que usan Respira (portada de la web). Zonas, sensores, puntos
-- de reparto y trabajadores dicen a cuál pertenecen (ayuntamiento_id).
CREATE TABLE ayuntamiento (
  id       VARCHAR(32)  NOT NULL,
  nombre   VARCHAR(80)  NOT NULL,
  comarca  VARCHAR(120) NOT NULL COMMENT 'texto bajo el nombre en la portada',
  desde    DATE         NOT NULL,
  activo   TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Parques, barrios y playas. «area» conserva el anillo heredado o la geometria GeoJSON.
-- Se listan por orden alfabetico (ORDER BY nombre): no hace falta un campo de orden.
CREATE TABLE zona (
  id           VARCHAR(32)  NOT NULL,
  ayuntamiento_id VARCHAR(32) NOT NULL COMMENT 'a que ayuntamiento pertenece',
  nombre       VARCHAR(80)  NOT NULL,
  tipo         ENUM('parque','barrio','playa') NOT NULL,
  area         TEXT         NOT NULL COMMENT 'contorno: anillo [lat,lng] heredado o geometria GeoJSON',
  etiqueta_lat DECIMAL(9,6) NOT NULL COMMENT 'donde se escribe el nombre en el mapa',
  etiqueta_lng DECIMAL(9,6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT zona_ayuntamiento_fk FOREIGN KEY (ayuntamiento_id) REFERENCES ayuntamiento (id),
  CONSTRAINT zona_area_json CHECK (JSON_VALID(area))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Cada sensor fisico. El UUID del iBeacon es 'RESPIRA-' + codigo (no se guarda).
-- tipo: voluntariado (se presta a vecinos), trabajador (lo lleva personal municipal),
--       bici (bici o moto compartida), autobus
-- estado: disponible/prestado (voluntariado), activo (fijo en un servicio o vehiculo),
--         mant (mantenimiento), baja (retirado; sus mediciones se conservan)
CREATE TABLE nodo (
  codigo      CHAR(8)      NOT NULL COMMENT 'RSP-XXXX',
  ayuntamiento_id VARCHAR(32) NOT NULL COMMENT 'a que ayuntamiento pertenece',
  tipo        ENUM('voluntariado','trabajador','bici','autobus') NOT NULL DEFAULT 'voluntariado',
  magnitudes  SET('o3','temp','ruido') NOT NULL DEFAULT 'o3,temp,ruido',
  estado      ENUM('disponible','prestado','activo','mant','baja') NOT NULL DEFAULT 'disponible',
  asignado_a  VARCHAR(80)  NOT NULL DEFAULT 'No asignado' COMMENT 'servicio o vehiculo (trabajador, bici, autobus); nunca el nombre de una persona',
  incidencia  VARCHAR(120) NOT NULL DEFAULT 'Sin incidencias' COMMENT 'que le pasa (por que esta en mant o de baja)',
  PRIMARY KEY (codigo),
  CONSTRAINT nodo_ayuntamiento_fk FOREIGN KEY (ayuntamiento_id) REFERENCES ayuntamiento (id),
  CONSTRAINT nodo_codigo_valido CHECK (codigo REGEXP '^RSP-[A-Z0-9]{4}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Puntos de reparto donde se prestan y devuelven sensores y se entregan regalos
CREATE TABLE punto_reparto (
  id        SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
  ayuntamiento_id VARCHAR(32) NOT NULL COMMENT 'a que ayuntamiento pertenece',
  nombre    VARCHAR(80)  NOT NULL,
  direccion VARCHAR(120) NOT NULL,
  horario   VARCHAR(80)  NOT NULL,
  activo    TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  CONSTRAINT punto_reparto_ayuntamiento_fk FOREIGN KEY (ayuntamiento_id) REFERENCES ayuntamiento (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================================================
-- 2. CUENTAS
-- ==============================================================

-- Todas las personas con cuenta:
--   vecino      app (voluntariado). id U-XXXXX. nombre = nombre de usuario.
--               No puede registrarse sin aceptar las condiciones: su «alta»
--               es el momento en que las acepto.
--   trabajador  app (personal municipal que lleva un sensor). id T-XXXX.
--               Sus datos de empleado van en «empleado».
--   admin, tecnico, reparto  panel de la web. id A-XXXX
CREATE TABLE usuario (
  id          VARCHAR(8)   NOT NULL,
  rol         ENUM('vecino','trabajador','admin','tecnico','reparto') NOT NULL,
  nombre      VARCHAR(80)  NOT NULL,
  email       VARCHAR(120) NOT NULL,
  clave_hash  VARCHAR(255) NOT NULL COMMENT 'bcrypt (password_hash)',
  alta        DATETIME(3)  NOT NULL,
  nombre_vecino VARCHAR(20) GENERATED ALWAYS AS (CASE WHEN rol = 'vecino' THEN LOWER(nombre) ELSE NULL END) STORED,
  PRIMARY KEY (id),
  UNIQUE KEY usuario_email (email),
  UNIQUE KEY usuario_nombre_vecino (nombre_vecino),
  KEY usuario_nombre (nombre),
  CONSTRAINT usuario_id_valido CHECK (id REGEXP '^(U-[0-9]{5}|T-[0-9]{4}|A-[0-9]{4})$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Datos de empleado de los trabajadores municipales (una fila por trabajador)
CREATE TABLE empleado (
  usuario_id    VARCHAR(8)  NOT NULL,
  ayuntamiento_id VARCHAR(32) NOT NULL COMMENT 'para que ayuntamiento trabaja',
  num_empleado  CHAR(6)     NOT NULL,
  servicio      VARCHAR(60) NOT NULL,
  PRIMARY KEY (usuario_id),
  UNIQUE KEY empleado_num (num_empleado),
  CONSTRAINT empleado_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT empleado_ayuntamiento_fk FOREIGN KEY (ayuntamiento_id) REFERENCES ayuntamiento (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Sesiones abiertas y enlaces de «he olvidado mi contrasena». Solo se guarda
-- el HASH (sha256): quien lea la BD no puede usarlos.
--   app        sesion de la app (90 dias)
--   web        sesion del panel (12 h)
--   recuperar  enlace para crear una contrasena nueva (1 h); se borra al usarlo
CREATE TABLE token (
  token_hash  CHAR(64)    NOT NULL,
  usuario_id  VARCHAR(8)  NOT NULL,
  tipo        ENUM('app','web','recuperar') NOT NULL,
  caduca      DATETIME(3) NOT NULL,
  PRIMARY KEY (token_hash),
  KEY token_usuario (usuario_id, tipo),
  CONSTRAINT token_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================================================
-- 3. MEDICIONES Y PRESTAMOS
-- ==============================================================

-- Cada dato de un sensor (a traves del movil de quien lo lleva). Solo se
-- guardan las validas: con ubicacion, dentro de una zona y dentro del rango
-- del sensor. Repetida = mismo sensor, tipo y valor con menos de 10 s.
CREATE TABLE medicion (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_nodo         CHAR(8)      NOT NULL,
  tipo            ENUM('o3','temp','ruido') NOT NULL,
  valor           DOUBLE       NOT NULL COMMENT 'o3 en ppm, temp en grados C, ruido en dB',
  fecha_medicion  DATETIME(3)  NOT NULL COMMENT 'UTC: cuando el movil oyo el beacon',
  lat             DECIMAL(9,6) NOT NULL,
  lng             DECIMAL(9,6) NOT NULL,
  zona_id         VARCHAR(32)  NOT NULL COMMENT 'la calcula el servidor (en que area cae el punto)',
  portador_id     VARCHAR(8)   NULL COMMENT 'quien llevaba el sensor (puntos). NULL: nadie con sesion lo llevaba. Nunca se publica',
  origen          ENUM('real','simulado') NOT NULL DEFAULT 'real',
  PRIMARY KEY (id),
  KEY medicion_zona (zona_id, tipo, fecha_medicion),
  KEY medicion_nodo (id_nodo, tipo, fecha_medicion),
  KEY medicion_portador (portador_id, fecha_medicion),
  CONSTRAINT medicion_nodo_fk FOREIGN KEY (id_nodo) REFERENCES nodo (codigo),
  CONSTRAINT medicion_zona_fk FOREIGN KEY (zona_id) REFERENCES zona (id),
  CONSTRAINT medicion_portador_fk FOREIGN KEY (portador_id) REFERENCES usuario (id) ON DELETE SET NULL,
  CONSTRAINT medicion_lat CHECK (lat BETWEEN -90 AND 90),
  CONSTRAINT medicion_lng CHECK (lng BETWEEN -180 AND 180)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Prestamos de sensores de voluntariado. Plazo: 10 dias desde la recogida,
-- y se puede devolver en cualquier punto de reparto.
--   No devuelto  en curso (o pasado de plazo si ya vencio fecha_limite)
--   Correcto     devuelto bien: se puede volver a prestar
--   Defectuoso   no se ha entregado en buenas condiciones: pasa a mantenimiento
CREATE TABLE prestamo (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nodo              CHAR(8)     NOT NULL,
  usuario_id        VARCHAR(8)  NULL COMMENT 'NULL solo si el vecino borro su cuenta',
  punto_reparto_id  SMALLINT UNSIGNED NOT NULL COMMENT 'donde se recogio',
  inicio            DATETIME(3) NOT NULL,
  fecha_limite      DATETIME(3) NOT NULL COMMENT 'inicio + 10 dias: fecha tope para devolverlo',
  estado_devolucion ENUM('No devuelto','Correcto','Defectuoso') NOT NULL DEFAULT 'No devuelto',
  PRIMARY KEY (id),
  KEY prestamo_nodo (nodo, estado_devolucion),
  KEY prestamo_usuario (usuario_id, estado_devolucion),
  CONSTRAINT prestamo_nodo_fk FOREIGN KEY (nodo) REFERENCES nodo (codigo),
  CONSTRAINT prestamo_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE SET NULL,
  CONSTRAINT prestamo_punto_fk FOREIGN KEY (punto_reparto_id) REFERENCES punto_reparto (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================================================
-- 4. PUNTOS, REGALOS Y CANJES
-- ==============================================================

-- Puntos de cada dia: una fila por usuario y dia (hora de Espana). Los
-- calcula el servidor con las mediciones del portador cuando el dia ya ha
-- acabado y los guarda aqui. Las reglas (1 punto cada 5 min, parques x2,
-- maximo 100 al dia) estan en el codigo (Puntos.php). El dia de hoy no se
-- guarda: se calcula en directo. Si llegan mediciones atrasadas de un dia
-- ya guardado, su fila se borra y se vuelve a calcular.
CREATE TABLE puntos_dia (
  usuario_id     VARCHAR(8)  NOT NULL,
  fecha          DATE        NOT NULL COMMENT 'dia en hora de Espana',
  minutos        SMALLINT UNSIGNED NOT NULL COMMENT 'minutos validos con el sensor',
  minutos_parque SMALLINT UNSIGNED NOT NULL COMMENT 'de esos, los que fueron dentro de parques',
  descartados    SMALLINT UNSIGNED NOT NULL COMMENT 'minutos que no cuentan (huecos, vehiculo, parado)',
  puntos         SMALLINT UNSIGNED NOT NULL,
  PRIMARY KEY (usuario_id, fecha),
  CONSTRAINT puntos_dia_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Catalogo de regalos (activo = 0: retirado; los canjes pendientes se siguen entregando)
CREATE TABLE regalo (
  id      VARCHAR(20)  NOT NULL,
  nombre  VARCHAR(80)  NOT NULL,
  coste   SMALLINT UNSIGNED NOT NULL,
  stock   SMALLINT UNSIGNED NOT NULL,
  activo  TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  CONSTRAINT regalo_valores CHECK (coste BETWEEN 1 AND 5000 AND stock <= 999)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Canjes. Los puntos se restan al canjear (coste se copia: si luego cambia el
-- precio, el canje no cambia). Si caduca sin recogerse, se devuelven.
CREATE TABLE canje (
  codigo     CHAR(7)     NOT NULL COMMENT 'CJ-XXXX',
  usuario_id VARCHAR(8)  NULL COMMENT 'NULL solo si el vecino borro su cuenta',
  regalo_id  VARCHAR(20) NOT NULL,
  coste      SMALLINT UNSIGNED NOT NULL,
  creado     DATETIME(3) NOT NULL,
  caduca     DATE        NOT NULL,
  estado     ENUM('pendiente','entregado','caducado') NOT NULL DEFAULT 'pendiente',
  PRIMARY KEY (codigo),
  KEY canje_usuario (usuario_id),
  CONSTRAINT canje_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE SET NULL,
  CONSTRAINT canje_regalo_fk FOREIGN KEY (regalo_id) REFERENCES regalo (id),
  CONSTRAINT canje_codigo_valido CHECK (codigo REGEXP '^CJ-[A-Z0-9]{4}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================================================
-- 5. COMUNIDAD
-- ==============================================================

-- Lo que notan los vecinos (capa «Comunidad»). Se ven 24 h. No son mediciones.
CREATE TABLE percepcion (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id VARCHAR(8)   NOT NULL COMMENT 'nunca se publica',
  tipo       ENUM('ruido','temp','aire') NOT NULL,
  nivel      TINYINT UNSIGNED NOT NULL COMMENT '1 bajo, 2 medio, 3 alto',
  lat        DECIMAL(9,6) NOT NULL,
  lng        DECIMAL(9,6) NOT NULL,
  direccion  VARCHAR(120) NOT NULL,
  creada     DATETIME(3)  NOT NULL,
  PRIMARY KEY (id),
  KEY percepcion_creada (creada),
  KEY percepcion_usuario (usuario_id, creada),
  CONSTRAINT percepcion_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT percepcion_nivel CHECK (nivel BETWEEN 1 AND 3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- El límite de publicación cuenta también las percepciones que se han borrado.
CREATE TABLE percepcion_envio (
  percepcion_id BIGINT UNSIGNED NOT NULL,
  usuario_id VARCHAR(8) NOT NULL,
  creada DATETIME(3) NOT NULL,
  PRIMARY KEY (percepcion_id),
  KEY percepcion_envio_usuario (usuario_id, creada),
  CONSTRAINT percepcion_envio_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---- Seguimiento de parques (app): favoritos y alertas de la cuenta ----
-- Antes vivían solo en el móvil; ahora se guardan en la cuenta y se
-- recuperan al iniciar sesión en otro móvil.
CREATE TABLE favorito (
  usuario_id VARCHAR(8)  NOT NULL,
  zona_id    VARCHAR(32) NOT NULL,
  creado     DATETIME(3) NOT NULL,
  PRIMARY KEY (usuario_id, zona_id),
  CONSTRAINT favorito_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT favorito_zona_fk FOREIGN KEY (zona_id) REFERENCES zona (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Una alerta por magnitud de cada parque seguido.
CREATE TABLE alerta (
  usuario_id VARCHAR(8)  NOT NULL,
  zona_id    VARCHAR(32) NOT NULL,
  magnitud   ENUM('o3','temp','ruido') NOT NULL,
  condicion  ENUM('bajo','medio','alto','custom') NOT NULL,
  valor      DECIMAL(8,3) NOT NULL DEFAULT 0 COMMENT 'límite; solo cuenta si condicion = custom',
  PRIMARY KEY (usuario_id, zona_id, magnitud),
  CONSTRAINT alerta_favorito_fk FOREIGN KEY (usuario_id, zona_id) REFERENCES favorito (usuario_id, zona_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE alerta_emitida (
  usuario_id VARCHAR(8) NOT NULL,
  zona_id VARCHAR(32) NOT NULL,
  magnitud ENUM('o3','temp','ruido') NOT NULL,
  momento DATETIME(3) NOT NULL,
  valor DECIMAL(8,3) NOT NULL,
  origen ENUM('real','simulado','estimado') NOT NULL,
  PRIMARY KEY (usuario_id, zona_id, magnitud, momento),
  KEY alerta_emitida_serie (zona_id, magnitud, momento),
  CONSTRAINT alerta_emitida_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT alerta_emitida_zona_fk FOREIGN KEY (zona_id) REFERENCES zona (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- @@TABLAS:FIN

-- ==============================================================
-- DATOS BASE (necesarios para que funcione)
-- ==============================================================
-- @@BASE:INICIO
INSERT INTO ayuntamiento (id, nombre, comarca, desde, activo) VALUES
  ('gandia', 'Gandia', 'La Safor · Comunitat Valenciana', '2026-06-01', 1);

INSERT INTO punto_reparto (id, nombre, direccion, horario, ayuntamiento_id) VALUES
  (1, 'Biblioteca Central', 'Pl. Mayor, 2', 'L–V 9:00–20:00', 'gandia'),
  (2, 'Centro Cívico Norte', 'Av. Norte, 15', 'L–S 10:00–14:00', 'gandia'),
  (3, 'Oficina de Turismo', 'Pl. de la Estación, 1', 'Todos los días 9:00–19:00', 'gandia');

INSERT INTO regalo (id, nombre, coste, stock, activo) VALUES
  ('g1', 'Bolsa de tela', 100, 0, 1),
  ('g2', 'Gorra Respira', 150, 12, 1),
  ('g3', 'Cantimplora', 250, 8, 1),
  ('g4', 'Mochila del ayuntamiento', 600, 3, 1);

-- --------------------------------------------------------------
-- Zonas. Las de Gandia son las del prototipo (aproximadas).
-- «campus» es el Campus de la UPV en Gandia: COMPRUEBA el rectangulo
-- en Google Maps y ajustalo si hace falta. Solo se ven en el mapa
-- las mediciones tomadas DENTRO de alguna zona.
-- --------------------------------------------------------------
INSERT INTO zona (id, nombre, tipo, area, etiqueta_lat, etiqueta_lng, ayuntamiento_id) VALUES
  ('ribera', 'Parque de la Ribera', 'parque', '[[38.976098,-0.201089],[38.976661,-0.200198],[38.977112,-0.199246],[38.97744,-0.198246],[38.977645,-0.197214],[38.977731,-0.196165],[38.977708,-0.195113],[38.97759,-0.194067],[38.977388,-0.193034],[38.976789,-0.192198],[38.975876,-0.191683],[38.97483,-0.191592],[38.97381,-0.191859],[38.973085,-0.19256],[38.972654,-0.19352],[38.97237,-0.194533],[38.972221,-0.195575],[38.972191,-0.196627],[38.97226,-0.197678],[38.972412,-0.198719],[38.972632,-0.199749],[38.973198,-0.200627],[38.974048,-0.201239],[38.975085,-0.201376]]', 38.975509, -0.195222, 'gandia'),
  ('molino', 'Jardín del Molino', 'parque', '[[38.978063,-0.177133],[38.978339,-0.1762],[38.978544,-0.175249],[38.978665,-0.174284],[38.978691,-0.173311],[38.978613,-0.172342],[38.978424,-0.171388],[38.978124,-0.170462],[38.977718,-0.169577],[38.976922,-0.169139],[38.97595,-0.169077],[38.975012,-0.169332],[38.974235,-0.169914],[38.973669,-0.170703],[38.973343,-0.171611],[38.973233,-0.172577],[38.973197,-0.173549],[38.973259,-0.17452],[38.973453,-0.175473],[38.973821,-0.176372],[38.974394,-0.177157],[38.975253,-0.177571],[38.97622,-0.177692],[38.977184,-0.177551]]', 38.976589, -0.173222, 'gandia'),
  ('central', 'Parque Central', 'parque', '[[38.967259,-0.186911],[38.967584,-0.18593],[38.967827,-0.184925],[38.967985,-0.183903],[38.968057,-0.182872],[38.968046,-0.181839],[38.967955,-0.180809],[38.967789,-0.179789],[38.967554,-0.178782],[38.966887,-0.178023],[38.96594,-0.17763],[38.964944,-0.177857],[38.964146,-0.178512],[38.963823,-0.179483],[38.963621,-0.180496],[38.963498,-0.181523],[38.963453,-0.182555],[38.963485,-0.183588],[38.963589,-0.184617],[38.963761,-0.185636],[38.963994,-0.186643],[38.96458,-0.187467],[38.96551,-0.187886],[38.966488,-0.187599]]', 38.965884, -0.182756, 'gandia'),
  ('estacion', 'Jardines de la Estación', 'parque', '[[38.961612,-0.199622],[38.96184,-0.198902],[38.962011,-0.198167],[38.962122,-0.197421],[38.962168,-0.196667],[38.962149,-0.195913],[38.962065,-0.195163],[38.961918,-0.194422],[38.961713,-0.193696],[38.961453,-0.192987],[38.960851,-0.192653],[38.960099,-0.192692],[38.959457,-0.193078],[38.959036,-0.193702],[38.958785,-0.194413],[38.958655,-0.195155],[38.958631,-0.195909],[38.958645,-0.196664],[38.958711,-0.197416],[38.95885,-0.198158],[38.959094,-0.198871],[38.959492,-0.199511],[38.96011,-0.199836],[38.960861,-0.199729]]', 38.960482, -0.194978, 'gandia'),
  ('oeste', 'Parque del Oeste', 'parque', '[[38.961857,-0.173222],[38.962022,-0.172473],[38.96214,-0.171716],[38.962204,-0.170952],[38.962209,-0.170185],[38.962148,-0.169421],[38.962019,-0.168666],[38.961818,-0.167926],[38.961545,-0.167209],[38.961203,-0.166523],[38.960522,-0.166317],[38.95977,-0.166457],[38.959137,-0.166886],[38.958695,-0.16751],[38.95841,-0.168221],[38.95823,-0.168966],[38.95821,-0.169727],[38.958316,-0.170486],[38.958484,-0.171234],[38.95874,-0.171956],[38.959116,-0.172624],[38.959645,-0.173178],[38.960332,-0.173439],[38.961095,-0.173331]]', 38.960384, -0.169702, 'gandia'),
  ('huerto', 'Huerto Urbano Norte', 'parque', '[[38.979536,-0.188378],[38.97978,-0.187778],[38.979935,-0.187149],[38.980012,-0.186506],[38.980024,-0.185858],[38.979982,-0.185212],[38.979896,-0.18457],[38.979773,-0.183934],[38.97962,-0.183304],[38.97923,-0.182866],[38.978597,-0.182724],[38.977958,-0.182816],[38.977412,-0.183161],[38.976999,-0.18366],[38.97669,-0.18423],[38.976532,-0.184853],[38.976474,-0.185498],[38.976476,-0.186146],[38.976561,-0.186788],[38.976758,-0.187404],[38.977098,-0.187954],[38.977583,-0.188378],[38.978234,-0.188378],[38.978885,-0.188378]]', 38.978652, -0.185689, 'gandia'),
  ('norte', 'Barrio Norte', 'barrio', '[[38.9815,-0.205],[38.9815,-0.161],[38.971679,-0.161],[38.971679,-0.205]]', 38.973054, -0.191018, 'gandia'),
  ('centro', 'Centro', 'barrio', '[[38.968634,-0.205],[38.968634,-0.161],[38.962643,-0.161],[38.962643,-0.205]]', 38.966768, -0.175667, 'gandia'),
  ('sur', 'Barrio Sur', 'barrio', '[[38.96225,-0.205],[38.96225,-0.161],[38.954,-0.161],[38.954,-0.205]]', 38.95621, -0.1874, 'gandia'),
  ('campus', 'Campus UPV de Gandia', 'parque', '[[38.9978,-0.1679],[38.9978,-0.1639],[38.9948,-0.1639],[38.9948,-0.1679]]', 38.9963, -0.1659, 'gandia');

-- Nodo real: el sensor montado (SPEC O3 + DHT11 + KY-037)
-- Grau i Platja. Contornos © colaboradores de OpenStreetMap, ODbL 1.0.
-- Fuente: https://www.openstreetmap.org/copyright (ver web/js/grau-platja.js).
INSERT INTO zona (id, nombre, tipo, area, etiqueta_lat, etiqueta_lng, ayuntamiento_id) VALUES
  ('clot', 'Parc del Clot de la Mota', 'parque', '{"type":"MultiPolygon","coordinates":[[[[-0.165423,39.002595],[-0.16651,39.003807],[-0.166647,39.003789],[-0.16788,39.00277],[-0.167887,39.002608],[-0.16775,39.002497],[-0.167404,39.002723],[-0.166798,39.002115],[-0.166856,39.001992],[-0.166825,39.001918],[-0.166798,39.001875],[-0.165423,39.002595]]],[[[-0.166385,39.003895],[-0.165285,39.002738],[-0.165114,39.002809],[-0.164706,39.003043],[-0.165794,39.004232],[-0.166344,39.003963],[-0.166385,39.003895]]]]}', 39.002841, -0.166719, 'gandia'),
  ('baladre', 'Parc del Baladre', 'parque', '{"type":"Polygon","coordinates":[[[-0.170647,39.007575],[-0.170262,39.007066],[-0.169702,39.006333],[-0.169619,39.006224],[-0.16898,39.006556],[-0.169062,39.006658],[-0.169585,39.00734],[-0.17,39.007879],[-0.170647,39.007575]]]}', 39.007052, -0.169807, 'gandia'),
  ('castell', 'Plaça del Castell', 'parque', '{"type":"Polygon","coordinates":[[[-0.172646,39.008985],[-0.17263,39.008884],[-0.172532,39.00882],[-0.172508,39.008732],[-0.1725,39.008667],[-0.172293,39.00868],[-0.172155,39.008733],[-0.17203,39.00877],[-0.171875,39.008816],[-0.171807,39.008852],[-0.171805,39.00888],[-0.17182,39.008906],[-0.171873,39.008937],[-0.171964,39.008982],[-0.172091,39.008993],[-0.172161,39.00902],[-0.172237,39.009068],[-0.172294,39.009132],[-0.172347,39.009138],[-0.172542,39.009046],[-0.172646,39.008985]]]}', 39.008903, -0.172225, 'gandia'),
  ('ermita', 'Plaça de l’Ermita', 'parque', '{"type":"Polygon","coordinates":[[[-0.160679,38.991806],[-0.160609,38.991766],[-0.159182,38.992204],[-0.159238,38.992294],[-0.159527,38.992193],[-0.159582,38.992285],[-0.15982,38.992175],[-0.159965,38.99221],[-0.160278,38.992109],[-0.160679,38.991806]]]}', 38.99203, -0.160066, 'gandia'),
  ('oller', 'Parc de l’Oller', 'parque', '{"type":"Polygon","coordinates":[[[-0.16137,38.989142],[-0.160862,38.988706],[-0.160353,38.988314],[-0.160117,38.988805],[-0.159997,38.989058],[-0.160216,38.989367],[-0.160306,38.989412],[-0.16137,38.989142]]]}', 38.988863, -0.160567, 'gandia'),
  ('platja-nord', 'Platja Nord de Gandia', 'playa', '{"type":"MultiPolygon","coordinates":[[[[-0.156262,38.99839],[-0.156708,38.999012],[-0.156725,38.999439],[-0.15739,39.000557],[-0.157772,39.000888],[-0.157921,39.001323],[-0.158069,39.001632],[-0.158433,39.00196],[-0.158621,39.002188],[-0.158753,39.002339],[-0.158965,39.002676],[-0.159238,39.003056],[-0.1598,39.003607],[-0.160165,39.003907],[-0.160337,39.004094],[-0.1605,39.00432],[-0.161203,39.005333],[-0.161526,39.005734],[-0.161972,39.006005],[-0.162148,39.006408],[-0.162754,39.007253],[-0.162951,39.007442],[-0.163164,39.007851],[-0.163579,39.008275],[-0.164113,39.008829],[-0.164495,39.009295],[-0.166069,39.008987],[-0.165812,39.008664],[-0.165446,39.008203],[-0.165065,39.007724],[-0.164642,39.007192],[-0.164068,39.00647],[-0.163402,39.005632],[-0.162882,39.004977],[-0.162288,39.00423],[-0.161861,39.003693],[-0.16148,39.003214],[-0.161062,39.002688],[-0.161009,39.00262],[-0.160536,39.002096],[-0.160023,39.001511],[-0.159584,39.001017],[-0.159155,39.000534],[-0.15901,39.000371],[-0.158907,39.000418],[-0.158619,39.000088],[-0.158524,38.999966],[-0.15866,38.999894],[-0.158345,38.99949],[-0.158208,38.999325],[-0.158122,38.999368],[-0.157882,38.999024],[-0.15781,38.998927],[-0.157795,38.998907],[-0.157429,38.998413],[-0.157416,38.998396],[-0.15705,38.997943],[-0.157043,38.997935],[-0.156662,38.998102],[-0.156426,38.998216],[-0.156354,38.998251],[-0.156262,38.99839]]],[[[-0.166069,39.008987],[-0.164495,39.009295],[-0.164958,39.009785],[-0.16527,39.010265],[-0.16565,39.010643],[-0.165841,39.010827],[-0.16601,39.011183],[-0.16631,39.011517],[-0.166514,39.01184],[-0.166876,39.012491],[-0.167241,39.012947],[-0.167471,39.013312],[-0.167867,39.013812],[-0.168131,39.014123],[-0.16847,39.014456],[-0.169067,39.015269],[-0.169404,39.01562],[-0.17012,39.016578],[-0.170443,39.016999],[-0.170989,39.017844],[-0.17186,39.018978],[-0.172306,39.019539],[-0.172651,39.01986],[-0.173146,39.020482],[-0.173815,39.021392],[-0.174702,39.021115],[-0.174656,39.02104],[-0.174432,39.020648],[-0.174338,39.02038],[-0.174396,39.02023],[-0.174479,39.020177],[-0.174419,39.020098],[-0.174038,39.020246],[-0.17374,39.019821],[-0.17362,39.019593],[-0.173586,39.019283],[-0.173705,39.019119],[-0.173607,39.018968],[-0.173258,39.019116],[-0.173047,39.018814],[-0.17274,39.018471],[-0.17248,39.018026],[-0.172518,39.01791],[-0.172687,39.017854],[-0.172701,39.017866],[-0.172798,39.017815],[-0.172816,39.017751],[-0.17271,39.017613],[-0.172319,39.017779],[-0.171334,39.016613],[-0.170687,39.015787],[-0.170781,39.015332],[-0.170764,39.015306],[-0.170668,39.015186],[-0.170165,39.014508],[-0.169708,39.01389],[-0.169342,39.013397],[-0.169262,39.013288],[-0.16925,39.013272],[-0.168738,39.012582],[-0.168287,39.011973],[-0.167748,39.011245],[-0.167695,39.011174],[-0.166969,39.01029],[-0.166356,39.009402],[-0.166069,39.008987]]]]}', 39.003615, -0.160804, 'gandia'),
  ('venecia', 'Platja de Venècia', 'playa', '{"type":"Polygon","coordinates":[[[-0.152638,38.991034],[-0.151749,38.991041],[-0.151579,38.99151],[-0.151472,38.991807],[-0.151975,38.991864],[-0.152022,38.991868],[-0.152081,38.991692],[-0.152149,38.991516],[-0.152181,38.991478],[-0.152274,38.991153],[-0.152442,38.991147],[-0.152608,38.991106],[-0.152638,38.991034]]]}', 38.991451, -0.151895, 'gandia'),
  ('marenys', 'Platja dels Marenys de Rafalcaïd', 'playa', '{"type":"Polygon","coordinates":[[[-0.151573,38.989722],[-0.151547,38.989636],[-0.151541,38.989573],[-0.151541,38.989515],[-0.151541,38.989504],[-0.1515,38.989353],[-0.151421,38.989216],[-0.151335,38.989074],[-0.151182,38.988862],[-0.150921,38.988421],[-0.150777,38.988131],[-0.15077,38.988117],[-0.150547,38.987771],[-0.150405,38.987518],[-0.15032,38.987389],[-0.150178,38.987177],[-0.150141,38.987122],[-0.149669,38.986381],[-0.149656,38.986364],[-0.149112,38.985624],[-0.148804,38.985206],[-0.148601,38.984983],[-0.148578,38.98493],[-0.14857,38.98483],[-0.148603,38.984801],[-0.148093,38.984192],[-0.147844,38.983845],[-0.147637,38.983912],[-0.147279,38.983486],[-0.147182,38.983325],[-0.146991,38.983355],[-0.146901,38.983349],[-0.147076,38.983703],[-0.147502,38.984125],[-0.147583,38.984241],[-0.148092,38.984899],[-0.148278,38.985193],[-0.148467,38.985387],[-0.148565,38.985493],[-0.148934,38.986029],[-0.148952,38.986056],[-0.149769,38.98715],[-0.149806,38.987221],[-0.150194,38.987971],[-0.150524,38.988693],[-0.150886,38.989289],[-0.150923,38.989412],[-0.150983,38.98959],[-0.150922,38.989782],[-0.151592,38.989912],[-0.151573,38.989722]]]}', 38.986618, -0.149596, 'gandia');

INSERT INTO nodo (codigo, tipo, magnitudes, estado, ayuntamiento_id) VALUES
  ('RSP-4F7K', 'voluntariado', 'o3,temp,ruido', 'disponible', 'gandia');
-- @@BASE:FIN

-- ==============================================================
-- DATOS DE DEMOSTRACIÓN (los del prototipo). Para empezar sin ellos,
-- borra desde aquí hasta el final antes de importar.
-- Contraseñas de prueba (en la BD solo está su hash):
--   app:  lucia@correo.es / Respira2026 · marcos@correo.es / Marcos2026 · ana.v@correo.es / AnaV2026
--   web:  admin@ayto.es / Admin2026! · tecnico@ayto.es / Tecnico2026! · reparto@ayto.es / Reparto2026!
--   app (trabajador): psanz@ayto.es / Trabajo2026! · emora@ayto.es / Parques2026!
-- Las fechas son relativas a cuando se importa (así la demo parece de hoy).
-- ==============================================================
-- @@DEMO:INICIO
INSERT INTO usuario (id, rol, nombre, email, clave_hash, alta) VALUES
  ('U-48213', 'vecino', 'lucia', 'lucia@correo.es', '$2y$10$4gDx13oXo01D3obsCD15oOw2sGbT9sc1u2zgQSpodrwnUFzTIYNru', UTC_TIMESTAMP(3) - INTERVAL 40 DAY),
  ('U-11902', 'vecino', 'marcos', 'marcos@correo.es', '$2y$10$WgYmGWK2.DlhnK8UsAURLe0DmhcmllP.Zgmh67HoZKCikw0Vift/q', UTC_TIMESTAMP(3) - INTERVAL 30 DAY),
  ('U-30577', 'vecino', 'ana.v', 'ana.v@correo.es', '$2y$10$IQ9mp7BBShFcW2IiIPyC2ey378xrI3S0LqJZJ.GI3kEQlyHXzGcYy', UTC_TIMESTAMP(3) - INTERVAL 20 DAY),
  ('A-0001', 'admin', 'Ana García', 'admin@ayto.es', '$2y$10$3Bff0nqlTMCb7MlmwvhBoOFQC5nz9UN2Qoe.cS2YBl4rGc//v1iLS', UTC_TIMESTAMP(3) - INTERVAL 60 DAY),
  ('A-0002', 'tecnico', 'Luis Pérez', 'tecnico@ayto.es', '$2y$10$.9Xda9NzdGTt9WXh4/s00eZ3hBcy/wqRAlThw/Q0xWEG/eUPwHKQe', UTC_TIMESTAMP(3) - INTERVAL 60 DAY),
  ('A-0003', 'reparto', 'Marta Ruiz', 'reparto@ayto.es', '$2y$10$A4i52694GXCpx3/c/9kWd.U.BHzvhBQ6vAkAz2IcFtJdCJU5spVtS', UTC_TIMESTAMP(3) - INTERVAL 60 DAY),
  ('T-0001', 'trabajador', 'Pablo Sanz Gil', 'psanz@ayto.es', '$2y$10$oW0XDYbcPJHbs6ZJcV0YOuu.nNdQc5m/r0/Hc53hxuYbHhgbOeVyW', UTC_TIMESTAMP(3) - INTERVAL 12 DAY),
  ('T-0002', 'trabajador', 'Elena Mora Vidal', 'emora@ayto.es', '$2y$10$4hHqhCfJu666HTs/oRb8z.k9eDu3vmE4rnq1ois6B2dkzLCzI6dJi', UTC_TIMESTAMP(3) - INTERVAL 4 DAY);

INSERT INTO empleado (usuario_id, num_empleado, servicio, ayuntamiento_id) VALUES
  ('T-0001', '104233', 'Limpieza viaria', 'gandia'),
  ('T-0002', '104587', 'Parques y jardines', 'gandia');


-- La flota del prototipo. Solo RSP-4F7K manda datos de verdad; los sensores de
-- servicio (L1MP, M7NT, B12A, V08E) los alimenta la simulación del servidor.
UPDATE nodo SET estado = 'prestado' WHERE codigo = 'RSP-4F7K';
INSERT INTO nodo (codigo, tipo, magnitudes, estado, asignado_a, incidencia, ayuntamiento_id) VALUES
  ('RSP-2B9Q', 'voluntariado', 'o3,temp,ruido', 'prestado',   'No asignado', 'Sin incidencias', 'gandia'),
  ('RSP-7H3M', 'voluntariado', 'o3,temp,ruido', 'disponible', 'No asignado', 'Sin incidencias', 'gandia'),
  ('RSP-5K8P', 'voluntariado', 'o3,temp',       'disponible', 'No asignado', 'Sin incidencias', 'gandia'),
  ('RSP-9C2D', 'voluntariado', 'o3,temp,ruido', 'disponible', 'No asignado', 'Sin incidencias', 'gandia'),
  ('RSP-X3QR', 'voluntariado', 'o3,temp',       'mant',       'No asignado', 'Carcasa rota al devolverlo', 'gandia'),
  ('RSP-L1MP', 'trabajador', 'o3,temp,ruido', 'activo', 'Limpieza viaria', 'Sin incidencias', 'gandia'),
  ('RSP-M7NT', 'trabajador', 'o3,temp',       'activo', 'Parques y jardines', 'Sin incidencias', 'gandia'),
  ('RSP-B12A', 'autobus', 'o3,temp,ruido', 'activo', 'Línea 2 · Bus 214', 'Sin incidencias', 'gandia'),
  ('RSP-B05C', 'autobus', 'o3,temp,ruido', 'activo', 'Línea 5 · Bus 231', 'Sin incidencias', 'gandia'),
  ('RSP-V08E', 'bici', 'o3,temp',       'activo', 'Bici compartida · Estación 8', 'Sin incidencias', 'gandia');

-- Último dato de los sensores que no alimenta la simulación (en Barrio Sur,
-- con más de 24 h: solo sirven para que el panel muestre «hace cuánto»)
INSERT INTO medicion (id_nodo, tipo, valor, fecha_medicion, lat, lng, zona_id, origen) VALUES
  ('RSP-2B9Q', 'temp', 18.5, UTC_TIMESTAMP(3) - INTERVAL 1530 MINUTE, 38.956210, -0.187400, 'sur', 'simulado'),
  ('RSP-7H3M', 'temp', 18.9, UTC_TIMESTAMP(3) - INTERVAL 1500 MINUTE, 38.956210, -0.187400, 'sur', 'simulado'),
  ('RSP-9C2D', 'temp', 19.3, UTC_TIMESTAMP(3) - INTERVAL 2900 MINUTE, 38.956210, -0.187400, 'sur', 'simulado'),
  ('RSP-X3QR', 'temp', 19.7, UTC_TIMESTAMP(3) - INTERVAL 4300 MINUTE, 38.956210, -0.187400, 'sur', 'simulado'),
  ('RSP-B05C', 'temp', 20.1, UTC_TIMESTAMP(3) - INTERVAL 1700 MINUTE, 38.956210, -0.187400, 'sur', 'simulado');

-- Préstamos: plazo de 10 días desde la recogida. Marcos se ha pasado del plazo.
INSERT INTO prestamo (nodo, usuario_id, punto_reparto_id, inicio, fecha_limite, estado_devolucion) VALUES
  ('RSP-7H3M', 'U-48213', 1, UTC_TIMESTAMP(3) - INTERVAL 3 DAY,  UTC_TIMESTAMP(3) + INTERVAL 7 DAY, 'Correcto'),
  ('RSP-2B9Q', 'U-11902', 2, UTC_TIMESTAMP(3) - INTERVAL 11 DAY, UTC_TIMESTAMP(3) - INTERVAL 1 DAY, 'No devuelto'),
  ('RSP-4F7K', 'U-48213', 1, UTC_TIMESTAMP(3) - INTERVAL 2 HOUR, UTC_TIMESTAMP(3) - INTERVAL 2 HOUR + INTERVAL 10 DAY, 'No devuelto');

-- Recorridos con sensor de días pasados (los puntos se calculan con ellos).
-- Una lectura por minuto desde las 9:00, andando a unos 3,6 km/h por el
-- Barrio Norte (fuera de los parques). Puntos con las reglas 5/2/100:
--   Lucía 89+89+80+86+23+91+32 = 490 (− canje de 150 = 340)
--   Marcos 72+48 = 120 · Ana 89+88+100 = 277 (− canje de 150 = 127)
INSERT INTO medicion (id_nodo, tipo, valor, fecha_medicion, lat, lng, zona_id, portador_id, origen)
WITH RECURSIVE n (k) AS (SELECT 0 UNION ALL SELECT k + 1 FROM n WHERE k < 500),
dias (usuario, nodo, atras, minutos) AS (
  SELECT 'U-48213', 'RSP-7H3M', 20, 445 UNION ALL SELECT 'U-48213', 'RSP-7H3M', 18, 445 UNION ALL
  SELECT 'U-48213', 'RSP-7H3M', 15, 400 UNION ALL SELECT 'U-48213', 'RSP-7H3M', 12, 430 UNION ALL
  SELECT 'U-48213', 'RSP-7H3M', 5, 115 UNION ALL SELECT 'U-48213', 'RSP-7H3M', 2, 455 UNION ALL
  SELECT 'U-48213', 'RSP-4F7K', 1, 160 UNION ALL
  SELECT 'U-11902', 'RSP-2B9Q', 2, 360 UNION ALL SELECT 'U-11902', 'RSP-2B9Q', 1, 240 UNION ALL
  SELECT 'U-30577', 'RSP-9C2D', 9, 445 UNION ALL SELECT 'U-30577', 'RSP-9C2D', 8, 440 UNION ALL
  SELECT 'U-30577', 'RSP-9C2D', 1, 500)
SELECT d.nodo, 'temp', 17 + (n.k MOD 30) / 10,
       CAST(CURDATE() AS DATETIME(3)) - INTERVAL d.atras DAY + INTERVAL 8 HOUR + INTERVAL n.k MINUTE,
       38.9805, ROUND(-0.204 + 0.000693 * IF(n.k MOD 120 < 60, n.k MOD 120, 120 - n.k MOD 120), 6),
       'norte', d.usuario, 'simulado'
FROM dias d JOIN n ON n.k <= d.minutos;

INSERT INTO canje (codigo, usuario_id, regalo_id, coste, creado, caduca, estado) VALUES
  ('CJ-8R2T', 'U-48213', 'g2', 150, UTC_TIMESTAMP(3) - INTERVAL 3 DAY, CURDATE() + INTERVAL 4 DAY, 'pendiente'),
  ('CJ-2K9L', 'U-11902', 'g3', 250, UTC_TIMESTAMP(3) - INTERVAL 10 DAY, CURDATE() - INTERVAL 3 DAY, 'caducado'),
  ('CJ-5P7Q', 'U-30577', 'g2', 150, UTC_TIMESTAMP(3) - INTERVAL 6 DAY, CURDATE() + INTERVAL 1 DAY, 'entregado');

-- Los ejemplos de Comunidad los devuelve la API sin atribuirlos a usuarios.
-- Lucía sigue dos parques y tiene una alerta de ruido en el Central
INSERT INTO favorito (usuario_id, zona_id, creado) VALUES
  ('U-48213', 'ribera',  UTC_TIMESTAMP(3) - INTERVAL 20 DAY),
  ('U-48213', 'central', UTC_TIMESTAMP(3) - INTERVAL 12 DAY);
INSERT INTO alerta (usuario_id, zona_id, magnitud, condicion, valor) VALUES
  ('U-48213', 'central', 'ruido', 'alto', 0);

-- @@DEMO:FIN
