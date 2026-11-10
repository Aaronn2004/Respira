-- ==============================================================
-- Fichero:     actualizar_v2.sql  (MariaDB - Plesk)
-- Descripción: Pasa la BD de la versión 1 (zona, nodo, medicion) a la
--              versión 2 COMPLETA SIN BORRAR las zonas, el nodo RSP-4F7K
--              ni las mediciones ya guardadas.
--              Se ejecuta UNA sola vez (si se repite, falla al crear tablas
--              que ya existen; no pasa nada, pero no lo repitas).
--              Si prefieres empezar de cero, importa esquema.sql en su lugar.
-- Diseño:      doc/base_de_datos.md
-- Autor:       Aaron Blasco Blay
-- Fecha:       09/11/2026
-- Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
-- ==============================================================
SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ---- tablas nuevas ----
-- Ayuntamientos que usan Respira (portada de la web)
CREATE TABLE ayuntamiento (
  id       VARCHAR(32)  NOT NULL,
  nombre   VARCHAR(80)  NOT NULL,
  comarca  VARCHAR(120) NOT NULL DEFAULT '' COMMENT 'texto bajo el nombre en la portada',
  desde    DATE         NOT NULL,
  activo   TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id)
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

-- ---- cambios en las tablas que ya existían ----
ALTER TABLE nodo
  ADD COLUMN motivo_mant VARCHAR(120) NULL AFTER ultima_medicion,
  ADD COLUMN baja DATETIME NULL AFTER alta;

-- portador_id pasa a ser el ID de una cuenta (antes texto libre)
UPDATE medicion SET portador_id = NULL WHERE portador_id IS NOT NULL AND portador_id NOT REGEXP '^(U-[0-9]{5}|T-[0-9]{4}|A-[0-9]{4})$';
ALTER TABLE medicion
  MODIFY portador_id VARCHAR(8) NULL COMMENT 'quien llevaba el sensor (puntos). Nunca se publica',
  ADD KEY medicion_portador (portador_id, medida_en);

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

-- Las mediciones antiguas con un portador que no existe se quedan sin portador
UPDATE medicion SET portador_id = NULL WHERE portador_id IS NOT NULL AND portador_id NOT IN (SELECT id FROM usuario);
ALTER TABLE medicion
  ADD CONSTRAINT medicion_portador_fk FOREIGN KEY (portador_id) REFERENCES usuario (id) ON DELETE SET NULL;
