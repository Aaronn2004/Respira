-- ==============================================================
-- Fichero:     actualizar_desde_v1.sql  (MariaDB - Plesk)
-- Descripción: Pasa la BD de la versión 1 (solo zona, nodo y medicion) a
--              la versión actual COMPLETA sin borrar las zonas, el nodo
--              RSP-4F7K ni las mediciones válidas ya guardadas.
--              OJO: se borran las mediciones que la versión actual no
--              admite (sin ubicación, fuera de las zonas o fuera de rango).
--              Se ejecuta UNA sola vez (si se repite, falla al crear tablas
--              que ya existen; no pasa nada, pero no lo repitas).
--              Si prefieres empezar de cero, importa esquema.sql en su lugar.
-- Diseño:      doc/base_de_datos.md
-- Autor:       Aaron Blasco Blay
-- Fecha:       10/11/2026
-- Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
-- ==============================================================
SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ---- tablas nuevas ----
CREATE TABLE ayuntamiento (
  id       VARCHAR(32)  NOT NULL,
  nombre   VARCHAR(80)  NOT NULL,
  comarca  VARCHAR(120) NOT NULL COMMENT 'texto bajo el nombre en la portada',
  desde    DATE         NOT NULL,
  activo   TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE punto_reparto (
  id        SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre    VARCHAR(80)  NOT NULL,
  direccion VARCHAR(120) NOT NULL,
  horario   VARCHAR(80)  NOT NULL,
  activo    TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

CREATE TABLE empleado (
  usuario_id    VARCHAR(8)  NOT NULL,
  num_empleado  CHAR(6)     NOT NULL,
  servicio      VARCHAR(60) NOT NULL,
  PRIMARY KEY (usuario_id),
  UNIQUE KEY empleado_num (num_empleado),
  CONSTRAINT empleado_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

CREATE TABLE token (
  token_hash  CHAR(64)    NOT NULL,
  usuario_id  VARCHAR(8)  NOT NULL,
  tipo        ENUM('app','web','recuperar') NOT NULL,
  caduca      DATETIME(3) NOT NULL,
  PRIMARY KEY (token_hash),
  KEY token_usuario (usuario_id, tipo),
  CONSTRAINT token_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

CREATE TABLE regalo (
  id      VARCHAR(20)  NOT NULL,
  nombre  VARCHAR(80)  NOT NULL,
  coste   SMALLINT UNSIGNED NOT NULL,
  stock   SMALLINT UNSIGNED NOT NULL,
  activo  TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  CONSTRAINT regalo_valores CHECK (coste BETWEEN 1 AND 5000 AND stock <= 999)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

-- ---- cambios en las tablas de la versión 1 ----
-- zona: el municipio no lo usa nadie (todas son de Gandia)
ALTER TABLE zona DROP COLUMN municipio;

-- nodo: sin UUID (es 'RESPIRA-' + codigo), sin batería y sin última medición
-- (sale de medicion); asignado_a y motivo nunca vacíos
ALTER TABLE nodo DROP CONSTRAINT nodo_bateria_valida;
UPDATE nodo SET asignado_a = 'No asignado' WHERE asignado_a IS NULL;
UPDATE nodo SET motivo_baja = 'Sin incidencias' WHERE motivo_baja IS NULL;
ALTER TABLE nodo
  DROP INDEX nodo_uuid,
  DROP COLUMN uuid_beacon,
  DROP COLUMN bateria,
  DROP COLUMN ultima_medicion,
  DROP COLUMN alta,
  MODIFY asignado_a VARCHAR(80) NOT NULL DEFAULT 'No asignado' COMMENT 'servicio o vehiculo (trab, bici, bus); nunca el nombre de una persona',
  CHANGE motivo_baja motivo VARCHAR(120) NOT NULL DEFAULT 'Sin incidencias' COMMENT 'por que esta en mant o de baja';

-- medicion: solo se quedan las válidas (con ubicación, dentro de una zona y
-- dentro del rango); nombres nuevos y sin contador
DELETE FROM medicion WHERE valida = 0 OR lat IS NULL OR lng IS NULL OR zona_id IS NULL;
-- portador_id pasa a ser el ID de una cuenta (antes texto libre)
UPDATE medicion SET portador_id = NULL WHERE portador_id IS NOT NULL AND portador_id NOT REGEXP '^(U-[0-9]{5}|T-[0-9]{4}|A-[0-9]{4})$';
ALTER TABLE medicion
  DROP FOREIGN KEY medicion_nodo_fk,
  DROP FOREIGN KEY medicion_zona_fk,
  DROP CONSTRAINT medicion_lat,
  DROP CONSTRAINT medicion_lng;
ALTER TABLE medicion
  DROP INDEX medicion_zona,
  DROP INDEX medicion_nodo,
  DROP COLUMN contador,
  DROP COLUMN recibida_en,
  DROP COLUMN precision_m,
  DROP COLUMN rssi,
  DROP COLUMN valida,
  DROP COLUMN motivo_descarte,
  CHANGE nodo id_nodo CHAR(8) NOT NULL,
  CHANGE magnitud tipo ENUM('o3','temp','ruido') NOT NULL,
  CHANGE medida_en fecha_medicion DATETIME(3) NOT NULL COMMENT 'UTC: cuando el movil oyo el beacon',
  MODIFY lat DECIMAL(9,6) NOT NULL,
  MODIFY lng DECIMAL(9,6) NOT NULL,
  MODIFY zona_id VARCHAR(32) NOT NULL COMMENT 'la calcula el servidor (punto en poligono)',
  MODIFY portador_id VARCHAR(8) NULL COMMENT 'quien llevaba el sensor (puntos). NULL: nadie con sesion lo llevaba. Nunca se publica';
ALTER TABLE medicion
  ADD KEY medicion_zona (zona_id, tipo, fecha_medicion),
  ADD KEY medicion_nodo (id_nodo, tipo, fecha_medicion),
  ADD KEY medicion_portador (portador_id, fecha_medicion),
  ADD CONSTRAINT medicion_nodo_fk FOREIGN KEY (id_nodo) REFERENCES nodo (codigo),
  ADD CONSTRAINT medicion_zona_fk FOREIGN KEY (zona_id) REFERENCES zona (id),
  ADD CONSTRAINT medicion_lat CHECK (lat BETWEEN -90 AND 90),
  ADD CONSTRAINT medicion_lng CHECK (lng BETWEEN -180 AND 180);

-- ---- datos base ----
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

-- Las mediciones antiguas con un portador que no existe se quedan sin portador
UPDATE medicion SET portador_id = NULL WHERE portador_id IS NOT NULL AND portador_id NOT IN (SELECT id FROM usuario);
ALTER TABLE medicion
  ADD CONSTRAINT medicion_portador_fk FOREIGN KEY (portador_id) REFERENCES usuario (id) ON DELETE SET NULL;
