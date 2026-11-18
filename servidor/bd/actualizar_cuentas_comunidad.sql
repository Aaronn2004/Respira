-- Aplicar después de actualizar_plesk.sql si la base todavía usa el esquema anterior.
SET NAMES utf8mb4;
SET time_zone = '+00:00';

ALTER TABLE usuario ADD COLUMN IF NOT EXISTS requiere_cambio_clave TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS nombre_vecino VARCHAR(20)
  GENERATED ALWAYS AS (CASE WHEN rol = 'vecino' THEN LOWER(nombre) ELSE NULL END) STORED;
-- Si falla por nombres duplicados existentes, resolver las cuentas afectadas
-- con sus titulares antes de repetir. La migración no renombra cuentas.
ALTER TABLE usuario ADD UNIQUE KEY IF NOT EXISTS usuario_nombre_vecino (nombre_vecino);

CREATE TABLE IF NOT EXISTS acceso_intento (
  correo_hash CHAR(64) NOT NULL,
  fallos TINYINT UNSIGNED NOT NULL,
  vence DATETIME(3) NOT NULL,
  PRIMARY KEY (correo_hash),
  KEY acceso_intento_vence (vence)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS percepcion_envio (
  percepcion_id BIGINT UNSIGNED NOT NULL,
  usuario_id VARCHAR(8) NOT NULL,
  creada DATETIME(3) NOT NULL,
  PRIMARY KEY (percepcion_id),
  KEY percepcion_envio_usuario (usuario_id, creada),
  CONSTRAINT percepcion_envio_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO percepcion_envio (percepcion_id, usuario_id, creada)
SELECT id, usuario_id, creada FROM percepcion WHERE creada > UTC_TIMESTAMP(3) - INTERVAL 1 HOUR;

ALTER TABLE alerta DROP PRIMARY KEY, ADD PRIMARY KEY (usuario_id, zona_id, magnitud);

CREATE TABLE IF NOT EXISTS alerta_emitida (
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
