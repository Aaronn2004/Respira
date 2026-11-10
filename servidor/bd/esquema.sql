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
--              y medicion) SIN perder mediciones, usa actualizar_v2.sql.
--
--              Al final carga los DATOS DE DEMOSTRACIÓN (las mismas
--              cuentas y contraseñas de prueba que la app y la web).
--              Las contraseñas se guardan con hash bcrypt, nunca en claro.
-- Diseño:      doc/base_de_datos.md
-- Autor:       Aaron Blasco Blay
-- Fecha:       09/11/2026
-- Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
-- ==============================================================
SET NAMES utf8mb4;
SET time_zone = '+00:00';   -- todas las fechas se guardan en UTC
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS canje;
DROP TABLE IF EXISTS regalo;
DROP TABLE IF EXISTS jornada;
DROP TABLE IF EXISTS reglas_puntos;
DROP TABLE IF EXISTS percepcion;
DROP TABLE IF EXISTS prestamo;
DROP TABLE IF EXISTS medicion;
DROP TABLE IF EXISTS token_cuenta;
DROP TABLE IF EXISTS sesion;
DROP TABLE IF EXISTS usuario;
DROP TABLE IF EXISTS punto_reparto;
DROP TABLE IF EXISTS ayuntamiento;
DROP TABLE IF EXISTS nodo;
DROP TABLE IF EXISTS zona;

SET FOREIGN_KEY_CHECKS = 1;

-- @@TABLAS:INICIO
-- ==============================================================
-- 1. MAPA Y SENSORES
-- ==============================================================

-- Ayuntamientos que usan Respira (portada de la web)
CREATE TABLE ayuntamiento (
  id       VARCHAR(32)  NOT NULL,
  nombre   VARCHAR(80)  NOT NULL,
  comarca  VARCHAR(120) NOT NULL DEFAULT '' COMMENT 'texto bajo el nombre en la portada',
  desde    DATE         NOT NULL,
  activo   TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Parques y barrios. El contorno va en JSON: [[lat,lng], ...]
CREATE TABLE zona (
  id           VARCHAR(32)  NOT NULL,
  nombre       VARCHAR(80)  NOT NULL,
  tipo         ENUM('parque','barrio') NOT NULL,
  municipio    VARCHAR(60)  NOT NULL DEFAULT 'Gandia',
  poligono     TEXT         NOT NULL COMMENT 'JSON [[lat,lng],...] (minimo 3 puntos)',
  etiqueta_lat DECIMAL(9,6) NOT NULL COMMENT 'donde se escribe el nombre en el mapa',
  etiqueta_lng DECIMAL(9,6) NOT NULL,
  orden        SMALLINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT zona_poligono_json CHECK (JSON_VALID(poligono))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Cada sensor fisico. uuid_beacon = 'RESPIRA-' + codigo (16 caracteres), igual que en el Arduino.
-- estado: disponible/prestado (voluntariado), activo (fijo en un servicio o vehiculo),
--         mant (mantenimiento), baja (retirado; sus mediciones se conservan)
CREATE TABLE nodo (
  codigo          CHAR(8)     NOT NULL COMMENT 'RSP-XXXX',
  uuid_beacon     CHAR(16)    NOT NULL,
  tipo            ENUM('vol','trab','bici','bus') NOT NULL DEFAULT 'vol',
  magnitudes      SET('o3','temp','ruido') NOT NULL DEFAULT 'o3,temp,ruido',
  estado          ENUM('disponible','prestado','activo','mant','baja') NOT NULL DEFAULT 'disponible',
  asignado_a      VARCHAR(80) NULL COMMENT 'servicio o vehiculo; nunca el nombre de una persona',
  bateria         TINYINT UNSIGNED NULL COMMENT 'NULL = conectado a corriente',
  ultima_medicion DATETIME(3) NULL,
  motivo_mant     VARCHAR(120) NULL,
  alta            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  baja            DATETIME    NULL,
  motivo_baja     VARCHAR(120) NULL,
  PRIMARY KEY (codigo),
  UNIQUE KEY nodo_uuid (uuid_beacon),
  CONSTRAINT nodo_codigo_valido CHECK (codigo REGEXP '^RSP-[A-Z0-9]{4}$'),
  CONSTRAINT nodo_bateria_valida CHECK (bateria IS NULL OR bateria <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Puntos de reparto donde se prestan y devuelven sensores y se entregan regalos
CREATE TABLE punto_reparto (
  id              SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
  ayuntamiento_id VARCHAR(32)  NOT NULL,
  nombre          VARCHAR(80)  NOT NULL,
  direccion       VARCHAR(120) NOT NULL,
  horario         VARCHAR(80)  NOT NULL,
  activo          TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  CONSTRAINT punto_ayto_fk FOREIGN KEY (ayuntamiento_id) REFERENCES ayuntamiento (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================================================
-- 2. CUENTAS
-- ==============================================================

-- Una sola tabla para todas las personas:
--   vecino      app (voluntariado). id U-XXXXX. nombre = nombre de usuario
--   trabajador  app (personal municipal que lleva un sensor). id T-XXXX. No entra al panel
--   admin, tecnico, reparto  panel de la web. id A-XXXX
CREATE TABLE usuario (
  id                 VARCHAR(8)   NOT NULL,
  rol                ENUM('vecino','trabajador','admin','tecnico','reparto') NOT NULL,
  nombre             VARCHAR(80)  NOT NULL,
  email              VARCHAR(120) NOT NULL,
  clave_hash         VARCHAR(255) NULL COMMENT 'bcrypt (password_hash). NULL = pendiente de activar',
  estado             ENUM('activo','pendiente') NOT NULL DEFAULT 'activo',
  num_empleado       CHAR(6)      NULL COMMENT 'solo trabajadores',
  servicio           VARCHAR(60)  NULL COMMENT 'solo trabajadores',
  punto_reparto_id   SMALLINT UNSIGNED NULL COMMENT 'solo rol reparto',
  intentos_fallidos  TINYINT UNSIGNED NOT NULL DEFAULT 0,
  bloqueado_hasta    DATETIME(3)  NULL,
  acepta_condiciones DATETIME     NULL COMMENT 'cuando acepto los terminos (vecinos)',
  alta               DATETIME(3)  NOT NULL,
  alta_por           VARCHAR(8)   NULL COMMENT 'quien dio de alta al trabajador',
  PRIMARY KEY (id),
  UNIQUE KEY usuario_email (email),
  UNIQUE KEY usuario_empleado (num_empleado),
  KEY usuario_nombre (nombre),
  CONSTRAINT usuario_punto_fk FOREIGN KEY (punto_reparto_id) REFERENCES punto_reparto (id) ON DELETE SET NULL,
  CONSTRAINT usuario_id_valido CHECK (id REGEXP '^(U-[0-9]{5}|T-[0-9]{4}|A-[0-9]{4})$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Sesiones abiertas (app y web). Solo se guarda el HASH del token.
CREATE TABLE sesion (
  token_hash  CHAR(64)    NOT NULL COMMENT 'sha256 del token que lleva el cliente',
  usuario_id  VARCHAR(8)  NOT NULL,
  cliente     ENUM('app','web') NOT NULL,
  creada      DATETIME(3) NOT NULL,
  caduca      DATETIME(3) NOT NULL,
  PRIMARY KEY (token_hash),
  KEY sesion_usuario (usuario_id),
  CONSTRAINT sesion_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Enlaces de un solo uso: activar la cuenta (trabajadores) o recuperar la contrasena
CREATE TABLE token_cuenta (
  token_hash  CHAR(64)    NOT NULL,
  usuario_id  VARCHAR(8)  NOT NULL,
  tipo        ENUM('activar','recuperar') NOT NULL,
  creado      DATETIME(3) NOT NULL,
  caduca      DATETIME(3) NOT NULL,
  usado       DATETIME(3) NULL,
  PRIMARY KEY (token_hash),
  KEY token_usuario (usuario_id),
  CONSTRAINT token_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================================================
-- 3. MEDICIONES Y PRESTAMOS
-- ==============================================================

-- Cada dato recibido de un sensor (a traves del movil de quien lo lleva)
CREATE TABLE medicion (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nodo            CHAR(8)     NOT NULL,
  magnitud        ENUM('o3','temp','ruido') NOT NULL,
  valor           DOUBLE      NOT NULL COMMENT 'o3 en ppm, temp en grados C, ruido en dB',
  contador        TINYINT UNSIGNED NOT NULL COMMENT 'byte bajo del major del iBeacon',
  medida_en       DATETIME(3) NOT NULL COMMENT 'UTC: cuando el movil oyo el beacon',
  recibida_en     DATETIME(3) NOT NULL COMMENT 'UTC: cuando llego al servidor',
  lat             DECIMAL(9,6) NULL,
  lng             DECIMAL(9,6) NULL,
  precision_m     SMALLINT UNSIGNED NULL,
  zona_id         VARCHAR(32) NULL COMMENT 'la calcula el servidor; NULL = fuera de todas las zonas',
  portador_id     VARCHAR(8)  NULL COMMENT 'quien llevaba el sensor (puntos). Nunca se publica',
  rssi            SMALLINT    NULL,
  origen          ENUM('real','simulado') NOT NULL DEFAULT 'real',
  valida          TINYINT(1)  NOT NULL DEFAULT 1,
  motivo_descarte VARCHAR(120) NULL,
  PRIMARY KEY (id),
  KEY medicion_zona (zona_id, magnitud, medida_en),
  KEY medicion_nodo (nodo, magnitud, medida_en),
  KEY medicion_portador (portador_id, medida_en),
  CONSTRAINT medicion_nodo_fk FOREIGN KEY (nodo) REFERENCES nodo (codigo),
  CONSTRAINT medicion_zona_fk FOREIGN KEY (zona_id) REFERENCES zona (id) ON DELETE SET NULL,
  CONSTRAINT medicion_portador_fk FOREIGN KEY (portador_id) REFERENCES usuario (id) ON DELETE SET NULL,
  CONSTRAINT medicion_lat CHECK (lat IS NULL OR lat BETWEEN -90 AND 90),
  CONSTRAINT medicion_lng CHECK (lng IS NULL OR lng BETWEEN -180 AND 180)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Prestamos de sensores de voluntariado (fin NULL = en curso)
CREATE TABLE prestamo (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nodo              CHAR(8)     NOT NULL,
  usuario_id        VARCHAR(8)  NULL COMMENT 'NULL si el vecino borro su cuenta',
  punto_reparto_id  SMALLINT UNSIGNED NULL,
  inicio            DATETIME(3) NOT NULL,
  fin               DATETIME(3) NULL,
  estado_devolucion ENUM('Correcto','Batería baja','Dañado') NULL,
  prestado_por      VARCHAR(8)  NULL,
  recibido_por      VARCHAR(8)  NULL,
  PRIMARY KEY (id),
  KEY prestamo_nodo (nodo, fin),
  KEY prestamo_usuario (usuario_id, fin),
  CONSTRAINT prestamo_nodo_fk FOREIGN KEY (nodo) REFERENCES nodo (codigo),
  CONSTRAINT prestamo_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE SET NULL,
  CONSTRAINT prestamo_punto_fk FOREIGN KEY (punto_reparto_id) REFERENCES punto_reparto (id) ON DELETE SET NULL,
  CONSTRAINT prestamo_prestado_fk FOREIGN KEY (prestado_por) REFERENCES usuario (id) ON DELETE SET NULL,
  CONSTRAINT prestamo_recibido_fk FOREIGN KEY (recibido_por) REFERENCES usuario (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==============================================================
-- 4. PUNTOS, REGALOS Y CANJES
-- ==============================================================

-- Reglas de calculo. Vale la ultima con desde <= el dia que se calcula
-- (los cambios del panel se aplican desde el dia siguiente).
CREATE TABLE reglas_puntos (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
  min_por_punto TINYINT UNSIGNED NOT NULL COMMENT '1 punto cada N minutos validos',
  mult_parque   TINYINT UNSIGNED NOT NULL COMMENT 'los minutos en parques cuentan x N',
  max_dia       SMALLINT UNSIGNED NOT NULL COMMENT 'maximo de puntos al dia',
  desde         DATE         NOT NULL,
  creada_por    VARCHAR(8)   NULL,
  creada        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY reglas_desde (desde),
  CONSTRAINT reglas_rango CHECK (min_por_punto BETWEEN 1 AND 60 AND mult_parque BETWEEN 1 AND 5 AND max_dia BETWEEN 10 AND 500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Un dia con sensor de un usuario: lo calcula el servidor con sus mediciones.
-- revision: auto (normal), pend (sospechoso: lo revisa administracion),
--           ok (revisado y aprobado), anulado (no suma puntos)
CREATE TABLE jornada (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id      VARCHAR(8)  NOT NULL,
  fecha           DATE        NOT NULL COMMENT 'dia en hora de Espana',
  minutos         SMALLINT UNSIGNED NOT NULL,
  minutos_parque  SMALLINT UNSIGNED NOT NULL,
  descartados     SMALLINT UNSIGNED NOT NULL COMMENT 'minutos que no cuentan (parado, sin datos, vehiculo)',
  puntos          SMALLINT UNSIGNED NOT NULL,
  revision        ENUM('auto','pend','ok','anulado') NOT NULL DEFAULT 'auto',
  motivo_revision VARCHAR(160) NULL,
  razon_anulacion VARCHAR(200) NULL COMMENT 'la ve el voluntario',
  revisada_por    VARCHAR(8)  NULL,
  calculada       DATETIME(3) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY jornada_dia (usuario_id, fecha),
  KEY jornada_revision (revision),
  CONSTRAINT jornada_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
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
  codigo        CHAR(7)     NOT NULL COMMENT 'CJ-XXXX',
  usuario_id    VARCHAR(8)  NULL,
  regalo_id     VARCHAR(20) NOT NULL,
  coste         SMALLINT UNSIGNED NOT NULL,
  creado        DATETIME(3) NOT NULL,
  caduca        DATE        NOT NULL,
  estado        ENUM('pendiente','entregado','caducado') NOT NULL DEFAULT 'pendiente',
  entregado_en  DATETIME(3) NULL,
  entregado_por VARCHAR(8)  NULL,
  PRIMARY KEY (codigo),
  KEY canje_usuario (usuario_id),
  CONSTRAINT canje_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE SET NULL,
  CONSTRAINT canje_regalo_fk FOREIGN KEY (regalo_id) REFERENCES regalo (id),
  CONSTRAINT canje_entregado_fk FOREIGN KEY (entregado_por) REFERENCES usuario (id) ON DELETE SET NULL,
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
  editada    DATETIME(3)  NULL,
  PRIMARY KEY (id),
  KEY percepcion_creada (creada),
  KEY percepcion_usuario (usuario_id, creada),
  CONSTRAINT percepcion_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT percepcion_nivel CHECK (nivel BETWEEN 1 AND 3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- @@TABLAS:FIN

-- ==============================================================
-- DATOS BASE (necesarios para que funcione)
-- ==============================================================
-- @@BASE:INICIO
INSERT INTO ayuntamiento (id, nombre, comarca, desde, activo) VALUES
  ('gandia', 'Gandia', 'La Safor · Comunitat Valenciana', '2026-06-01', 1);

INSERT INTO punto_reparto (id, ayuntamiento_id, nombre, direccion, horario) VALUES
  (1, 'gandia', 'Biblioteca Central', 'Pl. Mayor, 2', 'L–V 9:00–20:00'),
  (2, 'gandia', 'Centro Cívico Norte', 'Av. Norte, 15', 'L–S 10:00–14:00'),
  (3, 'gandia', 'Oficina de Turismo', 'Pl. de la Estación, 1', 'Todos los días 9:00–19:00');

-- Reglas iniciales: 1 punto cada 5 min, parques x2, máximo 100 al día
INSERT INTO reglas_puntos (min_por_punto, mult_parque, max_dia, desde) VALUES (5, 2, 100, '2026-06-01');

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
INSERT INTO zona (id, nombre, tipo, municipio, poligono, etiqueta_lat, etiqueta_lng, orden) VALUES
  ('ribera', 'Parque de la Ribera', 'parque', 'Gandia', '[[38.976098,-0.201089],[38.976661,-0.200198],[38.977112,-0.199246],[38.97744,-0.198246],[38.977645,-0.197214],[38.977731,-0.196165],[38.977708,-0.195113],[38.97759,-0.194067],[38.977388,-0.193034],[38.976789,-0.192198],[38.975876,-0.191683],[38.97483,-0.191592],[38.97381,-0.191859],[38.973085,-0.19256],[38.972654,-0.19352],[38.97237,-0.194533],[38.972221,-0.195575],[38.972191,-0.196627],[38.97226,-0.197678],[38.972412,-0.198719],[38.972632,-0.199749],[38.973198,-0.200627],[38.974048,-0.201239],[38.975085,-0.201376]]', 38.975509, -0.195222, 1),
  ('molino', 'Jardín del Molino', 'parque', 'Gandia', '[[38.978063,-0.177133],[38.978339,-0.1762],[38.978544,-0.175249],[38.978665,-0.174284],[38.978691,-0.173311],[38.978613,-0.172342],[38.978424,-0.171388],[38.978124,-0.170462],[38.977718,-0.169577],[38.976922,-0.169139],[38.97595,-0.169077],[38.975012,-0.169332],[38.974235,-0.169914],[38.973669,-0.170703],[38.973343,-0.171611],[38.973233,-0.172577],[38.973197,-0.173549],[38.973259,-0.17452],[38.973453,-0.175473],[38.973821,-0.176372],[38.974394,-0.177157],[38.975253,-0.177571],[38.97622,-0.177692],[38.977184,-0.177551]]', 38.976589, -0.173222, 2),
  ('central', 'Parque Central', 'parque', 'Gandia', '[[38.967259,-0.186911],[38.967584,-0.18593],[38.967827,-0.184925],[38.967985,-0.183903],[38.968057,-0.182872],[38.968046,-0.181839],[38.967955,-0.180809],[38.967789,-0.179789],[38.967554,-0.178782],[38.966887,-0.178023],[38.96594,-0.17763],[38.964944,-0.177857],[38.964146,-0.178512],[38.963823,-0.179483],[38.963621,-0.180496],[38.963498,-0.181523],[38.963453,-0.182555],[38.963485,-0.183588],[38.963589,-0.184617],[38.963761,-0.185636],[38.963994,-0.186643],[38.96458,-0.187467],[38.96551,-0.187886],[38.966488,-0.187599]]', 38.965884, -0.182756, 3),
  ('estacion', 'Jardines de la Estación', 'parque', 'Gandia', '[[38.961612,-0.199622],[38.96184,-0.198902],[38.962011,-0.198167],[38.962122,-0.197421],[38.962168,-0.196667],[38.962149,-0.195913],[38.962065,-0.195163],[38.961918,-0.194422],[38.961713,-0.193696],[38.961453,-0.192987],[38.960851,-0.192653],[38.960099,-0.192692],[38.959457,-0.193078],[38.959036,-0.193702],[38.958785,-0.194413],[38.958655,-0.195155],[38.958631,-0.195909],[38.958645,-0.196664],[38.958711,-0.197416],[38.95885,-0.198158],[38.959094,-0.198871],[38.959492,-0.199511],[38.96011,-0.199836],[38.960861,-0.199729]]', 38.960482, -0.194978, 4),
  ('oeste', 'Parque del Oeste', 'parque', 'Gandia', '[[38.961857,-0.173222],[38.962022,-0.172473],[38.96214,-0.171716],[38.962204,-0.170952],[38.962209,-0.170185],[38.962148,-0.169421],[38.962019,-0.168666],[38.961818,-0.167926],[38.961545,-0.167209],[38.961203,-0.166523],[38.960522,-0.166317],[38.95977,-0.166457],[38.959137,-0.166886],[38.958695,-0.16751],[38.95841,-0.168221],[38.95823,-0.168966],[38.95821,-0.169727],[38.958316,-0.170486],[38.958484,-0.171234],[38.95874,-0.171956],[38.959116,-0.172624],[38.959645,-0.173178],[38.960332,-0.173439],[38.961095,-0.173331]]', 38.960384, -0.169702, 5),
  ('huerto', 'Huerto Urbano Norte', 'parque', 'Gandia', '[[38.979536,-0.188378],[38.97978,-0.187778],[38.979935,-0.187149],[38.980012,-0.186506],[38.980024,-0.185858],[38.979982,-0.185212],[38.979896,-0.18457],[38.979773,-0.183934],[38.97962,-0.183304],[38.97923,-0.182866],[38.978597,-0.182724],[38.977958,-0.182816],[38.977412,-0.183161],[38.976999,-0.18366],[38.97669,-0.18423],[38.976532,-0.184853],[38.976474,-0.185498],[38.976476,-0.186146],[38.976561,-0.186788],[38.976758,-0.187404],[38.977098,-0.187954],[38.977583,-0.188378],[38.978234,-0.188378],[38.978885,-0.188378]]', 38.978652, -0.185689, 6),
  ('norte', 'Barrio Norte', 'barrio', 'Gandia', '[[38.9815,-0.205],[38.9815,-0.161],[38.971679,-0.161],[38.971679,-0.205]]', 38.973054, -0.191018, 7),
  ('centro', 'Centro', 'barrio', 'Gandia', '[[38.968634,-0.205],[38.968634,-0.161],[38.962643,-0.161],[38.962643,-0.205]]', 38.966768, -0.175667, 8),
  ('sur', 'Barrio Sur', 'barrio', 'Gandia', '[[38.96225,-0.205],[38.96225,-0.161],[38.954,-0.161],[38.954,-0.205]]', 38.95621, -0.1874, 9),
  ('campus', 'Campus UPV de Gandia', 'parque', 'Gandia', '[[38.9978,-0.1679],[38.9978,-0.1639],[38.9948,-0.1639],[38.9948,-0.1679]]', 38.9963, -0.1659, 10);

-- Nodo real: el sensor montado (SPEC O3 + DHT11 + KY-037)
INSERT INTO nodo (codigo, uuid_beacon, tipo, magnitudes, estado, asignado_a, bateria) VALUES
  ('RSP-4F7K', 'RESPIRA-RSP-4F7K', 'vol', 'o3,temp,ruido', 'disponible', NULL, NULL);
-- @@BASE:FIN

-- ==============================================================
-- DATOS DE DEMOSTRACIÓN (los del prototipo). Para empezar sin ellos,
-- borra desde aquí hasta el final antes de importar.
-- Contraseñas de prueba (en la BD solo está su hash):
--   app:  lucia@correo.es / Respira2026 · marcos@correo.es / Marcos2026 · ana.v@correo.es / AnaV2026
--   web:  admin@ayto.es / Admin2026! · tecnico@ayto.es / Tecnico2026! · reparto@ayto.es / Reparto2026!
--   app (trabajador): psanz@ayto.es / Trabajo2026!
-- Las fechas son relativas a cuando se importa (así la demo parece de hoy).
-- ==============================================================
-- @@DEMO:INICIO
INSERT INTO usuario (id, rol, nombre, email, clave_hash, estado, num_empleado, servicio, punto_reparto_id, acepta_condiciones, alta, alta_por) VALUES
  ('U-48213', 'vecino', 'lucia',  'lucia@correo.es',  '$2y$10$4gDx13oXo01D3obsCD15oOw2sGbT9sc1u2zgQSpodrwnUFzTIYNru', 'activo', NULL, NULL, NULL, UTC_TIMESTAMP() - INTERVAL 40 DAY, UTC_TIMESTAMP(3) - INTERVAL 40 DAY, NULL),
  ('U-11902', 'vecino', 'marcos', 'marcos@correo.es', '$2y$10$WgYmGWK2.DlhnK8UsAURLe0DmhcmllP.Zgmh67HoZKCikw0Vift/q', 'activo', NULL, NULL, NULL, UTC_TIMESTAMP() - INTERVAL 30 DAY, UTC_TIMESTAMP(3) - INTERVAL 30 DAY, NULL),
  ('U-30577', 'vecino', 'ana.v',  'ana.v@correo.es',  '$2y$10$IQ9mp7BBShFcW2IiIPyC2ey378xrI3S0LqJZJ.GI3kEQlyHXzGcYy', 'activo', NULL, NULL, NULL, UTC_TIMESTAMP() - INTERVAL 20 DAY, UTC_TIMESTAMP(3) - INTERVAL 20 DAY, NULL),
  ('A-0001', 'admin',   'Ana García', 'admin@ayto.es',   '$2y$10$3Bff0nqlTMCb7MlmwvhBoOFQC5nz9UN2Qoe.cS2YBl4rGc//v1iLS', 'activo', NULL, NULL, NULL, NULL, UTC_TIMESTAMP(3) - INTERVAL 60 DAY, NULL),
  ('A-0002', 'tecnico', 'Luis Pérez', 'tecnico@ayto.es', '$2y$10$.9Xda9NzdGTt9WXh4/s00eZ3hBcy/wqRAlThw/Q0xWEG/eUPwHKQe', 'activo', NULL, NULL, NULL, NULL, UTC_TIMESTAMP(3) - INTERVAL 60 DAY, NULL),
  ('A-0003', 'reparto', 'Marta Ruiz', 'reparto@ayto.es', '$2y$10$A4i52694GXCpx3/c/9kWd.U.BHzvhBQ6vAkAz2IcFtJdCJU5spVtS', 'activo', NULL, NULL, 1,    NULL, UTC_TIMESTAMP(3) - INTERVAL 60 DAY, NULL),
  ('T-0001', 'trabajador', 'Pablo Sanz Gil',   'psanz@ayto.es', '$2y$10$oW0XDYbcPJHbs6ZJcV0YOuu.nNdQc5m/r0/Hc53hxuYbHhgbOeVyW', 'activo', '104233', 'Limpieza viaria', NULL, NULL, UTC_TIMESTAMP(3) - INTERVAL 12 DAY, 'A-0001'),
  ('T-0002', 'trabajador', 'Elena Mora Vidal', 'emora@ayto.es', NULL, 'pendiente', '104587', 'Parques y jardines', NULL, NULL, UTC_TIMESTAMP(3) - INTERVAL 6 DAY, 'A-0001');

-- Enlace de activación de Elena ya caducado (su token real no se conoce: solo el hash)
INSERT INTO token_cuenta (token_hash, usuario_id, tipo, creado, caduca) VALUES
  (SHA2('demo-caducado-emora', 256), 'T-0002', 'activar', UTC_TIMESTAMP(3) - INTERVAL 6 DAY, UTC_TIMESTAMP(3) - INTERVAL 4 DAY);

-- La flota del prototipo (solo RSP-4F7K manda datos de verdad; el resto lleva
-- su «último dato» y su batería fijados para que el panel se vea completo)
UPDATE nodo SET estado = 'prestado' WHERE codigo = 'RSP-4F7K';
INSERT INTO nodo (codigo, uuid_beacon, tipo, magnitudes, estado, asignado_a, bateria, ultima_medicion, motivo_mant) VALUES
  ('RSP-2B9Q', 'RESPIRA-RSP-2B9Q', 'vol',  'o3,temp,ruido', 'prestado',   NULL, 22,  UTC_TIMESTAMP(3) - INTERVAL 1530 MINUTE, NULL),
  ('RSP-7H3M', 'RESPIRA-RSP-7H3M', 'vol',  'o3,temp,ruido', 'disponible', NULL, 95,  UTC_TIMESTAMP(3) - INTERVAL 1300 MINUTE, NULL),
  ('RSP-5K8P', 'RESPIRA-RSP-5K8P', 'vol',  'o3,temp',       'disponible', NULL, 100, NULL, NULL),
  ('RSP-9C2D', 'RESPIRA-RSP-9C2D', 'vol',  'o3,temp,ruido', 'disponible', NULL, 64,  UTC_TIMESTAMP(3) - INTERVAL 2900 MINUTE, NULL),
  ('RSP-X3QR', 'RESPIRA-RSP-X3QR', 'vol',  'o3,temp',       'mant',       NULL, NULL, UTC_TIMESTAMP(3) - INTERVAL 4300 MINUTE, 'Carcasa rota al devolverlo'),
  ('RSP-L1MP', 'RESPIRA-RSP-L1MP', 'trab', 'o3,temp,ruido', 'activo', 'Limpieza · Brigada de parques 2', 55, UTC_TIMESTAMP(3) - INTERVAL 3 MINUTE, NULL),
  ('RSP-M7NT', 'RESPIRA-RSP-M7NT', 'trab', 'o3,temp',       'activo', 'Mantenimiento de jardines', 18, UTC_TIMESTAMP(3) - INTERVAL 45 MINUTE, NULL),
  ('RSP-B12A', 'RESPIRA-RSP-B12A', 'bus',  'o3,temp,ruido', 'activo', 'Línea 2 · Bus 214', NULL, UTC_TIMESTAMP(3) - INTERVAL 1 MINUTE, NULL),
  ('RSP-B05C', 'RESPIRA-RSP-B05C', 'bus',  'o3,temp,ruido', 'activo', 'Línea 5 · Bus 231', NULL, UTC_TIMESTAMP(3) - INTERVAL 1700 MINUTE, NULL),
  ('RSP-V08E', 'RESPIRA-RSP-V08E', 'bici', 'o3,temp',       'activo', 'Bici compartida · Estación 8', 41, UTC_TIMESTAMP(3) - INTERVAL 30 MINUTE, NULL);

INSERT INTO prestamo (nodo, usuario_id, punto_reparto_id, inicio, fin, estado_devolucion, prestado_por, recibido_por) VALUES
  ('RSP-7H3M', 'U-48213', 1, UTC_TIMESTAMP(3) - INTERVAL 3 DAY, UTC_TIMESTAMP(3) - INTERVAL 2 DAY, 'Correcto', 'A-0003', 'A-0003'),
  ('RSP-2B9Q', 'U-11902', 2, UTC_TIMESTAMP(3) - INTERVAL 26 HOUR, NULL, NULL, 'A-0001', NULL),
  ('RSP-4F7K', 'U-48213', 1, UTC_TIMESTAMP(3) - INTERVAL 2 HOUR, NULL, NULL, 'A-0003', NULL);

-- Días con sensor (puntos). Saldo de Lucía = 340 y de Marcos = 120, como en la app
INSERT INTO jornada (usuario_id, fecha, minutos, minutos_parque, descartados, puntos, revision, motivo_revision, calculada) VALUES
  ('U-48213', CURDATE() - INTERVAL 20 DAY, 300, 100, 0, 80, 'auto', NULL, UTC_TIMESTAMP(3)),
  ('U-48213', CURDATE() - INTERVAL 18 DAY, 360, 120, 0, 96, 'auto', NULL, UTC_TIMESTAMP(3)),
  ('U-48213', CURDATE() - INTERVAL 15 DAY, 380, 120, 0, 100, 'auto', NULL, UTC_TIMESTAMP(3)),
  ('U-48213', CURDATE() - INTERVAL 12 DAY, 270, 70, 0, 68, 'auto', NULL, UTC_TIMESTAMP(3)),
  ('U-48213', CURDATE() - INTERVAL 5 DAY, 95, 20, 30, 23, 'auto', NULL, UTC_TIMESTAMP(3)),
  ('U-48213', CURDATE() - INTERVAL 2 DAY, 310, 150, 0, 91, 'ok', 'Llega al 90 % del máximo diario', UTC_TIMESTAMP(3)),
  ('U-48213', CURDATE() - INTERVAL 1 DAY, 120, 40, 12, 32, 'auto', NULL, UTC_TIMESTAMP(3)),
  ('U-11902', CURDATE() - INTERVAL 2 DAY, 240, 0, 0, 72, 'auto', NULL, UTC_TIMESTAMP(3)),
  ('U-11902', CURDATE() - INTERVAL 1 DAY, 180, 60, 180, 48, 'pend', 'Sensor 3 h sin moverse en el mismo punto (descontado)', UTC_TIMESTAMP(3)),
  ('U-30577', CURDATE() - INTERVAL 1 DAY, 520, 0, 0, 100, 'pend', 'Llega al máximo diario', UTC_TIMESTAMP(3));
UPDATE jornada SET revisada_por = 'A-0001' WHERE revision = 'ok';

INSERT INTO canje (codigo, usuario_id, regalo_id, coste, creado, caduca, estado, entregado_en, entregado_por) VALUES
  ('CJ-8R2T', 'U-48213', 'g2', 150, UTC_TIMESTAMP(3) - INTERVAL 3 DAY, CURDATE() + INTERVAL 4 DAY, 'pendiente', NULL, NULL),
  ('CJ-2K9L', 'U-11902', 'g3', 250, UTC_TIMESTAMP(3) - INTERVAL 10 DAY, CURDATE() - INTERVAL 3 DAY, 'caducado', NULL, NULL),
  ('CJ-5P7Q', 'U-30577', 'g2', 150, UTC_TIMESTAMP(3) - INTERVAL 6 DAY, CURDATE() + INTERVAL 1 DAY, 'entregado', UTC_TIMESTAMP(3) - INTERVAL 5 DAY, 'A-0003');
-- Saldo de Ana: necesita puntos para el canje entregado
INSERT INTO jornada (usuario_id, fecha, minutos, minutos_parque, descartados, puntos, revision, calculada) VALUES
  ('U-30577', CURDATE() - INTERVAL 9 DAY, 400, 100, 0, 100, 'auto', UTC_TIMESTAMP(3)),
  ('U-30577', CURDATE() - INTERVAL 8 DAY, 380, 60, 0, 88, 'auto', UTC_TIMESTAMP(3));

-- Percepciones de las últimas horas (desaparecen del mapa a las 24 h de importar)
INSERT INTO percepcion (usuario_id, tipo, nivel, lat, lng, direccion, creada) VALUES
  ('U-11902', 'ruido', 3, 38.968978, -0.197667, 'Av. del Río, 40',   UTC_TIMESTAMP(3) - INTERVAL 20 MINUTE),
  ('U-30577', 'temp',  2, 38.971433, -0.177622, 'C/ del Molino, 18', UTC_TIMESTAMP(3) - INTERVAL 120 MINUTE),
  ('U-11902', 'aire',  1, 38.962839, -0.188867, 'C/ Mayor, 61',      UTC_TIMESTAMP(3) - INTERVAL 300 MINUTE),
  ('U-30577', 'ruido', 3, 38.964804, -0.178600, 'Pl. Mayor, 4',      UTC_TIMESTAMP(3) - INTERVAL 12 MINUTE),
  ('U-11902', 'temp',  3, 38.972661, -0.165400, 'Av. Norte, 7',      UTC_TIMESTAMP(3) - INTERVAL 540 MINUTE),
  ('U-30577', 'aire',  2, 38.977080, -0.192289, 'C/ Alta, 30',       UTC_TIMESTAMP(3) - INTERVAL 900 MINUTE),
  ('U-11902', 'ruido', 1, 38.958420, -0.182022, 'C/ Poniente, 12',   UTC_TIMESTAMP(3) - INTERVAL 60 MINUTE),
  ('U-48213', 'ruido', 2, 38.963700, -0.186500, 'C/ Mayor, 43',      UTC_TIMESTAMP(3) - INTERVAL 40 MINUTE);
-- @@DEMO:FIN
