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
-- Fecha:       10/11/2026
-- Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
-- ==============================================================
SET NAMES utf8mb4;
SET time_zone = '+00:00';   -- todas las fechas se guardan en UTC
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS canje;
DROP TABLE IF EXISTS regalo;
DROP TABLE IF EXISTS revision;
DROP TABLE IF EXISTS jornada;
DROP TABLE IF EXISTS reglas_puntos;
DROP TABLE IF EXISTS percepcion;
DROP TABLE IF EXISTS prestamo;
DROP TABLE IF EXISTS medicion;
DROP TABLE IF EXISTS token;
DROP TABLE IF EXISTS invitacion;
DROP TABLE IF EXISTS empleado;
DROP TABLE IF EXISTS token_cuenta;
DROP TABLE IF EXISTS sesion;
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

-- Ayuntamientos que usan Respira (portada de la web)
CREATE TABLE ayuntamiento (
  id       VARCHAR(32)  NOT NULL,
  nombre   VARCHAR(80)  NOT NULL,
  comarca  VARCHAR(120) NOT NULL COMMENT 'texto bajo el nombre en la portada',
  desde    DATE         NOT NULL,
  activo   TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Parques y barrios. El contorno va en JSON: [[lat,lng], ...]
CREATE TABLE zona (
  id           VARCHAR(32)  NOT NULL,
  nombre       VARCHAR(80)  NOT NULL,
  tipo         ENUM('parque','barrio') NOT NULL,
  poligono     TEXT         NOT NULL COMMENT 'JSON [[lat,lng],...] (minimo 3 puntos)',
  etiqueta_lat DECIMAL(9,6) NOT NULL COMMENT 'donde se escribe el nombre en el mapa',
  etiqueta_lng DECIMAL(9,6) NOT NULL,
  orden        SMALLINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT zona_poligono_json CHECK (JSON_VALID(poligono))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Cada sensor fisico. El UUID del iBeacon es 'RESPIRA-' + codigo (no se guarda).
-- estado: disponible/prestado (voluntariado), activo (fijo en un servicio o vehiculo),
--         mant (mantenimiento), baja (retirado; sus mediciones se conservan)
CREATE TABLE nodo (
  codigo      CHAR(8)      NOT NULL COMMENT 'RSP-XXXX',
  tipo        ENUM('vol','trab','bici','bus') NOT NULL DEFAULT 'vol',
  magnitudes  SET('o3','temp','ruido') NOT NULL DEFAULT 'o3,temp,ruido',
  estado      ENUM('disponible','prestado','activo','mant','baja') NOT NULL DEFAULT 'disponible',
  asignado_a  VARCHAR(80)  NOT NULL DEFAULT 'No asignado' COMMENT 'servicio o vehiculo (trab, bici, bus); nunca el nombre de una persona',
  motivo      VARCHAR(120) NOT NULL DEFAULT 'Sin incidencias' COMMENT 'por que esta en mant o de baja',
  PRIMARY KEY (codigo),
  CONSTRAINT nodo_codigo_valido CHECK (codigo REGEXP '^RSP-[A-Z0-9]{4}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Puntos de reparto donde se prestan y devuelven sensores y se entregan regalos
CREATE TABLE punto_reparto (
  id        SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre    VARCHAR(80)  NOT NULL,
  direccion VARCHAR(120) NOT NULL,
  horario   VARCHAR(80)  NOT NULL,
  activo    TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id)
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
  PRIMARY KEY (id),
  UNIQUE KEY usuario_email (email),
  KEY usuario_nombre (nombre),
  CONSTRAINT usuario_id_valido CHECK (id REGEXP '^(U-[0-9]{5}|T-[0-9]{4}|A-[0-9]{4})$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Datos de empleado de los trabajadores municipales (una fila por trabajador)
CREATE TABLE empleado (
  usuario_id    VARCHAR(8)  NOT NULL,
  num_empleado  CHAR(6)     NOT NULL,
  servicio      VARCHAR(60) NOT NULL,
  PRIMARY KEY (usuario_id),
  UNIQUE KEY empleado_num (num_empleado),
  CONSTRAINT empleado_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Trabajadores dados de alta por administracion que aun no han activado su
-- cuenta. Al poner su contrasena con el enlace (48 h) pasan a usuario + empleado
-- y la invitacion se borra.
CREATE TABLE invitacion (
  id            VARCHAR(8)   NOT NULL COMMENT 'el T-XXXX que tendra la cuenta',
  nombre        VARCHAR(80)  NOT NULL,
  email         VARCHAR(120) NOT NULL,
  num_empleado  CHAR(6)      NOT NULL,
  servicio      VARCHAR(60)  NOT NULL,
  token_hash    CHAR(64)     NOT NULL COMMENT 'sha256 del enlace de activacion',
  caduca        DATETIME(3)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY invitacion_email (email),
  UNIQUE KEY invitacion_num (num_empleado),
  UNIQUE KEY invitacion_token (token_hash),
  CONSTRAINT invitacion_id_valido CHECK (id REGEXP '^T-[0-9]{4}$')
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
  zona_id         VARCHAR(32)  NOT NULL COMMENT 'la calcula el servidor (punto en poligono)',
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

-- Reglas de calculo. Vale la ultima con desde <= el dia que se calcula
-- (los cambios del panel se aplican desde el dia siguiente).
CREATE TABLE reglas_puntos (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
  min_por_punto TINYINT UNSIGNED NOT NULL COMMENT '1 punto cada N minutos validos',
  mult_parque   TINYINT UNSIGNED NOT NULL COMMENT 'los minutos en parques cuentan x N',
  max_dia       SMALLINT UNSIGNED NOT NULL COMMENT 'maximo de puntos al dia',
  desde         DATE         NOT NULL,
  PRIMARY KEY (id),
  KEY reglas_desde (desde),
  CONSTRAINT reglas_rango CHECK (min_por_punto BETWEEN 1 AND 60 AND mult_parque BETWEEN 1 AND 5 AND max_dia BETWEEN 10 AND 500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Los puntos de cada dia NO se guardan: el servidor los calcula con las
-- mediciones del portador. Solo se guardan los dias sospechosos que revisa
-- administracion (llegan al 90 % del maximo o el sensor estuvo 1 h parado).
--   pend     sin revisar (los puntos cuentan mientras tanto)
--   ok       aprobado
--   anulado  no suma puntos; «razon» la ve el voluntario
CREATE TABLE revision (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  usuario_id  VARCHAR(8)   NOT NULL,
  fecha       DATE         NOT NULL COMMENT 'dia en hora de Espana',
  motivo      VARCHAR(160) NOT NULL COMMENT 'por que se marco',
  estado      ENUM('pend','ok','anulado') NOT NULL DEFAULT 'pend',
  razon       VARCHAR(200) NOT NULL DEFAULT 'Sin revisar' COMMENT 'lo que decide administracion',
  PRIMARY KEY (id),
  UNIQUE KEY revision_dia (usuario_id, fecha),
  KEY revision_estado (estado),
  CONSTRAINT revision_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
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
-- @@TABLAS:FIN

-- ==============================================================
-- DATOS BASE (necesarios para que funcione)
-- ==============================================================
-- @@BASE:INICIO
INSERT INTO ayuntamiento (id, nombre, comarca, desde, activo) VALUES
  ('gandia', 'Gandia', 'La Safor · Comunitat Valenciana', '2026-06-01', 1);

INSERT INTO punto_reparto (id, nombre, direccion, horario) VALUES
  (1, 'Biblioteca Central', 'Pl. Mayor, 2', 'L–V 9:00–20:00'),
  (2, 'Centro Cívico Norte', 'Av. Norte, 15', 'L–S 10:00–14:00'),
  (3, 'Oficina de Turismo', 'Pl. de la Estación, 1', 'Todos los días 9:00–19:00');

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
INSERT INTO zona (id, nombre, tipo, poligono, etiqueta_lat, etiqueta_lng, orden) VALUES
  ('ribera', 'Parque de la Ribera', 'parque', '[[38.976098,-0.201089],[38.976661,-0.200198],[38.977112,-0.199246],[38.97744,-0.198246],[38.977645,-0.197214],[38.977731,-0.196165],[38.977708,-0.195113],[38.97759,-0.194067],[38.977388,-0.193034],[38.976789,-0.192198],[38.975876,-0.191683],[38.97483,-0.191592],[38.97381,-0.191859],[38.973085,-0.19256],[38.972654,-0.19352],[38.97237,-0.194533],[38.972221,-0.195575],[38.972191,-0.196627],[38.97226,-0.197678],[38.972412,-0.198719],[38.972632,-0.199749],[38.973198,-0.200627],[38.974048,-0.201239],[38.975085,-0.201376]]', 38.975509, -0.195222, 1),
  ('molino', 'Jardín del Molino', 'parque', '[[38.978063,-0.177133],[38.978339,-0.1762],[38.978544,-0.175249],[38.978665,-0.174284],[38.978691,-0.173311],[38.978613,-0.172342],[38.978424,-0.171388],[38.978124,-0.170462],[38.977718,-0.169577],[38.976922,-0.169139],[38.97595,-0.169077],[38.975012,-0.169332],[38.974235,-0.169914],[38.973669,-0.170703],[38.973343,-0.171611],[38.973233,-0.172577],[38.973197,-0.173549],[38.973259,-0.17452],[38.973453,-0.175473],[38.973821,-0.176372],[38.974394,-0.177157],[38.975253,-0.177571],[38.97622,-0.177692],[38.977184,-0.177551]]', 38.976589, -0.173222, 2),
  ('central', 'Parque Central', 'parque', '[[38.967259,-0.186911],[38.967584,-0.18593],[38.967827,-0.184925],[38.967985,-0.183903],[38.968057,-0.182872],[38.968046,-0.181839],[38.967955,-0.180809],[38.967789,-0.179789],[38.967554,-0.178782],[38.966887,-0.178023],[38.96594,-0.17763],[38.964944,-0.177857],[38.964146,-0.178512],[38.963823,-0.179483],[38.963621,-0.180496],[38.963498,-0.181523],[38.963453,-0.182555],[38.963485,-0.183588],[38.963589,-0.184617],[38.963761,-0.185636],[38.963994,-0.186643],[38.96458,-0.187467],[38.96551,-0.187886],[38.966488,-0.187599]]', 38.965884, -0.182756, 3),
  ('estacion', 'Jardines de la Estación', 'parque', '[[38.961612,-0.199622],[38.96184,-0.198902],[38.962011,-0.198167],[38.962122,-0.197421],[38.962168,-0.196667],[38.962149,-0.195913],[38.962065,-0.195163],[38.961918,-0.194422],[38.961713,-0.193696],[38.961453,-0.192987],[38.960851,-0.192653],[38.960099,-0.192692],[38.959457,-0.193078],[38.959036,-0.193702],[38.958785,-0.194413],[38.958655,-0.195155],[38.958631,-0.195909],[38.958645,-0.196664],[38.958711,-0.197416],[38.95885,-0.198158],[38.959094,-0.198871],[38.959492,-0.199511],[38.96011,-0.199836],[38.960861,-0.199729]]', 38.960482, -0.194978, 4),
  ('oeste', 'Parque del Oeste', 'parque', '[[38.961857,-0.173222],[38.962022,-0.172473],[38.96214,-0.171716],[38.962204,-0.170952],[38.962209,-0.170185],[38.962148,-0.169421],[38.962019,-0.168666],[38.961818,-0.167926],[38.961545,-0.167209],[38.961203,-0.166523],[38.960522,-0.166317],[38.95977,-0.166457],[38.959137,-0.166886],[38.958695,-0.16751],[38.95841,-0.168221],[38.95823,-0.168966],[38.95821,-0.169727],[38.958316,-0.170486],[38.958484,-0.171234],[38.95874,-0.171956],[38.959116,-0.172624],[38.959645,-0.173178],[38.960332,-0.173439],[38.961095,-0.173331]]', 38.960384, -0.169702, 5),
  ('huerto', 'Huerto Urbano Norte', 'parque', '[[38.979536,-0.188378],[38.97978,-0.187778],[38.979935,-0.187149],[38.980012,-0.186506],[38.980024,-0.185858],[38.979982,-0.185212],[38.979896,-0.18457],[38.979773,-0.183934],[38.97962,-0.183304],[38.97923,-0.182866],[38.978597,-0.182724],[38.977958,-0.182816],[38.977412,-0.183161],[38.976999,-0.18366],[38.97669,-0.18423],[38.976532,-0.184853],[38.976474,-0.185498],[38.976476,-0.186146],[38.976561,-0.186788],[38.976758,-0.187404],[38.977098,-0.187954],[38.977583,-0.188378],[38.978234,-0.188378],[38.978885,-0.188378]]', 38.978652, -0.185689, 6),
  ('norte', 'Barrio Norte', 'barrio', '[[38.9815,-0.205],[38.9815,-0.161],[38.971679,-0.161],[38.971679,-0.205]]', 38.973054, -0.191018, 7),
  ('centro', 'Centro', 'barrio', '[[38.968634,-0.205],[38.968634,-0.161],[38.962643,-0.161],[38.962643,-0.205]]', 38.966768, -0.175667, 8),
  ('sur', 'Barrio Sur', 'barrio', '[[38.96225,-0.205],[38.96225,-0.161],[38.954,-0.161],[38.954,-0.205]]', 38.95621, -0.1874, 9),
  ('campus', 'Campus UPV de Gandia', 'parque', '[[38.9978,-0.1679],[38.9978,-0.1639],[38.9948,-0.1639],[38.9948,-0.1679]]', 38.9963, -0.1659, 10);

-- Nodo real: el sensor montado (SPEC O3 + DHT11 + KY-037)
INSERT INTO nodo (codigo, tipo, magnitudes, estado) VALUES
  ('RSP-4F7K', 'vol', 'o3,temp,ruido', 'disponible');
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
INSERT INTO usuario (id, rol, nombre, email, clave_hash, alta) VALUES
  ('U-48213', 'vecino', 'lucia', 'lucia@correo.es', '$2y$10$4gDx13oXo01D3obsCD15oOw2sGbT9sc1u2zgQSpodrwnUFzTIYNru', UTC_TIMESTAMP(3) - INTERVAL 40 DAY),
  ('U-11902', 'vecino', 'marcos', 'marcos@correo.es', '$2y$10$WgYmGWK2.DlhnK8UsAURLe0DmhcmllP.Zgmh67HoZKCikw0Vift/q', UTC_TIMESTAMP(3) - INTERVAL 30 DAY),
  ('U-30577', 'vecino', 'ana.v', 'ana.v@correo.es', '$2y$10$IQ9mp7BBShFcW2IiIPyC2ey378xrI3S0LqJZJ.GI3kEQlyHXzGcYy', UTC_TIMESTAMP(3) - INTERVAL 20 DAY),
  ('A-0001', 'admin', 'Ana García', 'admin@ayto.es', '$2y$10$3Bff0nqlTMCb7MlmwvhBoOFQC5nz9UN2Qoe.cS2YBl4rGc//v1iLS', UTC_TIMESTAMP(3) - INTERVAL 60 DAY),
  ('A-0002', 'tecnico', 'Luis Pérez', 'tecnico@ayto.es', '$2y$10$.9Xda9NzdGTt9WXh4/s00eZ3hBcy/wqRAlThw/Q0xWEG/eUPwHKQe', UTC_TIMESTAMP(3) - INTERVAL 60 DAY),
  ('A-0003', 'reparto', 'Marta Ruiz', 'reparto@ayto.es', '$2y$10$A4i52694GXCpx3/c/9kWd.U.BHzvhBQ6vAkAz2IcFtJdCJU5spVtS', UTC_TIMESTAMP(3) - INTERVAL 60 DAY),
  ('T-0001', 'trabajador', 'Pablo Sanz Gil', 'psanz@ayto.es', '$2y$10$oW0XDYbcPJHbs6ZJcV0YOuu.nNdQc5m/r0/Hc53hxuYbHhgbOeVyW', UTC_TIMESTAMP(3) - INTERVAL 12 DAY);

INSERT INTO empleado (usuario_id, num_empleado, servicio) VALUES
  ('T-0001', '104233', 'Limpieza viaria');

-- Elena, dada de alta por administración: aún no ha activado su cuenta y su
-- enlace ya caducó (su token real no se conoce: solo el hash)
INSERT INTO invitacion (id, nombre, email, num_empleado, servicio, token_hash, caduca) VALUES
  ('T-0002', 'Elena Mora Vidal', 'emora@ayto.es', '104587', 'Parques y jardines',
   SHA2('demo-caducado-emora', 256), UTC_TIMESTAMP(3) - INTERVAL 4 DAY);

-- La flota del prototipo. Solo RSP-4F7K manda datos de verdad; los sensores de
-- servicio (L1MP, M7NT, B12A, V08E) los alimenta la simulación del servidor.
UPDATE nodo SET estado = 'prestado' WHERE codigo = 'RSP-4F7K';
INSERT INTO nodo (codigo, tipo, magnitudes, estado, asignado_a, motivo) VALUES
  ('RSP-2B9Q', 'vol',  'o3,temp,ruido', 'prestado',   'No asignado', 'Sin incidencias'),
  ('RSP-7H3M', 'vol',  'o3,temp,ruido', 'disponible', 'No asignado', 'Sin incidencias'),
  ('RSP-5K8P', 'vol',  'o3,temp',       'disponible', 'No asignado', 'Sin incidencias'),
  ('RSP-9C2D', 'vol',  'o3,temp,ruido', 'disponible', 'No asignado', 'Sin incidencias'),
  ('RSP-X3QR', 'vol',  'o3,temp',       'mant',       'No asignado', 'Carcasa rota al devolverlo'),
  ('RSP-L1MP', 'trab', 'o3,temp,ruido', 'activo', 'Limpieza · Brigada de parques 2', 'Sin incidencias'),
  ('RSP-M7NT', 'trab', 'o3,temp',       'activo', 'Mantenimiento de jardines', 'Sin incidencias'),
  ('RSP-B12A', 'bus',  'o3,temp,ruido', 'activo', 'Línea 2 · Bus 214', 'Sin incidencias'),
  ('RSP-B05C', 'bus',  'o3,temp,ruido', 'activo', 'Línea 5 · Bus 231', 'Sin incidencias'),
  ('RSP-V08E', 'bici', 'o3,temp',       'activo', 'Bici compartida · Estación 8', 'Sin incidencias');

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

-- Días que revisa administración
INSERT INTO revision (usuario_id, fecha, motivo, estado, razon) VALUES
  ('U-48213', CURDATE() - INTERVAL 2 DAY, 'Llega al 90 % del máximo diario', 'ok', 'Aprobado'),
  ('U-11902', CURDATE() - INTERVAL 1 DAY, 'Sensor 3 h sin moverse en el mismo punto (descontado)', 'pend', 'Sin revisar'),
  ('U-30577', CURDATE() - INTERVAL 1 DAY, 'Llega al máximo diario', 'pend', 'Sin revisar');

INSERT INTO canje (codigo, usuario_id, regalo_id, coste, creado, caduca, estado) VALUES
  ('CJ-8R2T', 'U-48213', 'g2', 150, UTC_TIMESTAMP(3) - INTERVAL 3 DAY, CURDATE() + INTERVAL 4 DAY, 'pendiente'),
  ('CJ-2K9L', 'U-11902', 'g3', 250, UTC_TIMESTAMP(3) - INTERVAL 10 DAY, CURDATE() - INTERVAL 3 DAY, 'caducado'),
  ('CJ-5P7Q', 'U-30577', 'g2', 150, UTC_TIMESTAMP(3) - INTERVAL 6 DAY, CURDATE() + INTERVAL 1 DAY, 'entregado');

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
