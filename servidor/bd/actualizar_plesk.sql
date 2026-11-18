-- ==============================================================
-- Fichero:     actualizar_plesk.sql  (MariaDB - Plesk)
-- Descripción: Pone al día la BD de Plesk (la del esquema.sql del 10/11/2026)
--              sin borrar mediciones ni cuentas. Cambios:
--                - favorito y alerta: parques que sigue cada cuenta.
--                - Fuera «invitacion»: administración pone la contraseña
--                  del trabajador al darlo de alta (las invitaciones sin
--                  activar se pierden: hay que volver a darlas de alta).
--                - zona: «poligono» pasa a llamarse «area» y sobra «orden»
--                  (las zonas se listan por orden alfabético).
--                - nodo: «tipo» con palabras (voluntariado, trabajador, bici,
--                  autobus) y «motivo» pasa a llamarse «incidencia».
--                - Fuera «reglas_puntos» (las reglas van en el código) y
--                  «revision» (ya no se revisan días). Nueva «puntos_dia»:
--                  los puntos de cada usuario y día. Se rellena sola la
--                  primera vez que cada usuario abre su cuenta.
--                - zona, nodo, punto_reparto y empleado: «ayuntamiento_id»
--                  (FK a ayuntamiento). Lo que ya existe pasa a ser de
--                  'gandia'; si tu ayuntamiento tiene otro id, cámbialo abajo.
--              Se ejecuta desde phpMyAdmin (pestaña «Importar»). Se puede
--              repetir: si algo ya está hecho, no lo vuelve a hacer.
-- Diseño:      doc/base_de_datos.md
-- Autor:       Aaron Blasco Blay
-- Fecha:       18/11/2026
-- Copyright:   (c) 2026 Aaron Blasco Blay - GTI, UPV
-- ==============================================================
SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ---- Seguimiento de parques (app): favoritos y alertas de la cuenta ----
-- Antes vivían solo en el móvil; ahora se guardan en la cuenta y se
-- recuperan al iniciar sesión en otro móvil.
CREATE TABLE IF NOT EXISTS favorito (
  usuario_id VARCHAR(8)  NOT NULL,
  zona_id    VARCHAR(32) NOT NULL,
  creado     DATETIME(3) NOT NULL,
  PRIMARY KEY (usuario_id, zona_id),
  CONSTRAINT favorito_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
  CONSTRAINT favorito_zona_fk FOREIGN KEY (zona_id) REFERENCES zona (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Una alerta por parque seguido (el móvil la comprueba y avisa)
CREATE TABLE IF NOT EXISTS alerta (
  usuario_id VARCHAR(8)  NOT NULL,
  zona_id    VARCHAR(32) NOT NULL,
  magnitud   ENUM('o3','temp','ruido') NOT NULL,
  condicion  ENUM('bajo','medio','alto','custom') NOT NULL,
  valor      DECIMAL(8,3) NOT NULL DEFAULT 0 COMMENT 'límite; solo cuenta si condicion = custom',
  PRIMARY KEY (usuario_id, zona_id),
  CONSTRAINT alerta_favorito_fk FOREIGN KEY (usuario_id, zona_id) REFERENCES favorito (usuario_id, zona_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---- Trabajadores: sin invitaciones ----
DROP TABLE IF EXISTS invitacion;

-- Demo: Elena (antes una invitación caducada) pasa a cuenta activa.
-- Contraseña de prueba: Parques2026!
INSERT IGNORE INTO usuario (id, rol, nombre, email, clave_hash, alta) VALUES
  ('T-0002', 'trabajador', 'Elena Mora Vidal', 'emora@ayto.es', '$2y$10$4hHqhCfJu666HTs/oRb8z.k9eDu3vmE4rnq1ois6B2dkzLCzI6dJi', UTC_TIMESTAMP(3) - INTERVAL 4 DAY);
INSERT IGNORE INTO empleado (usuario_id, num_empleado, servicio) VALUES
  ('T-0002', '104587', 'Parques y jardines');

-- ---- zona: «area» en vez de «poligono», sin «orden» ----
ALTER TABLE zona DROP CONSTRAINT IF EXISTS zona_poligono_json;
ALTER TABLE zona CHANGE COLUMN IF EXISTS poligono area TEXT NOT NULL COMMENT 'contorno: JSON [[lat,lng],...] (minimo 3 puntos)';
ALTER TABLE zona DROP COLUMN IF EXISTS orden;
ALTER TABLE zona DROP CONSTRAINT IF EXISTS zona_area_json;
ALTER TABLE zona ADD CONSTRAINT zona_area_json CHECK (JSON_VALID(area));

-- ---- nodo: tipo con palabras y «incidencia» en vez de «motivo» ----
ALTER TABLE nodo MODIFY tipo ENUM('vol','trab','bus','voluntariado','trabajador','bici','autobus') NOT NULL;
UPDATE nodo SET tipo = CASE tipo WHEN 'vol' THEN 'voluntariado' WHEN 'trab' THEN 'trabajador' WHEN 'bus' THEN 'autobus' ELSE tipo END;
ALTER TABLE nodo MODIFY tipo ENUM('voluntariado','trabajador','bici','autobus') NOT NULL DEFAULT 'voluntariado';
ALTER TABLE nodo MODIFY asignado_a VARCHAR(80) NOT NULL DEFAULT 'No asignado' COMMENT 'servicio o vehiculo (trabajador, bici, autobus); nunca el nombre de una persona';
ALTER TABLE nodo CHANGE COLUMN IF EXISTS motivo incidencia VARCHAR(120) NOT NULL DEFAULT 'Sin incidencias' COMMENT 'que le pasa (por que esta en mant o de baja)';

-- ---- puntos: reglas en el código, sin revisiones, puntos de cada día ----
DROP TABLE IF EXISTS revision;
DROP TABLE IF EXISTS reglas_puntos;
CREATE TABLE IF NOT EXISTS puntos_dia (
  usuario_id     VARCHAR(8)  NOT NULL,
  fecha          DATE        NOT NULL COMMENT 'dia en hora de Espana',
  minutos        SMALLINT UNSIGNED NOT NULL COMMENT 'minutos validos con el sensor',
  minutos_parque SMALLINT UNSIGNED NOT NULL COMMENT 'de esos, los que fueron dentro de parques',
  descartados    SMALLINT UNSIGNED NOT NULL COMMENT 'minutos que no cuentan (huecos, vehiculo, parado)',
  puntos         SMALLINT UNSIGNED NOT NULL,
  PRIMARY KEY (usuario_id, fecha),
  CONSTRAINT puntos_dia_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---- A qué ayuntamiento pertenece cada zona, sensor, punto de reparto y trabajador ----
-- Se añade con 'gandia' por defecto (para rellenar lo que ya hay) y luego se quita
-- el valor por defecto: lo nuevo tiene que decir siempre su ayuntamiento.
ALTER TABLE zona ADD COLUMN IF NOT EXISTS ayuntamiento_id VARCHAR(32) NOT NULL DEFAULT 'gandia' COMMENT 'a que ayuntamiento pertenece' AFTER id;
ALTER TABLE zona ALTER COLUMN ayuntamiento_id DROP DEFAULT;
ALTER TABLE zona ADD CONSTRAINT zona_ayuntamiento_fk FOREIGN KEY IF NOT EXISTS (ayuntamiento_id) REFERENCES ayuntamiento (id);

ALTER TABLE nodo ADD COLUMN IF NOT EXISTS ayuntamiento_id VARCHAR(32) NOT NULL DEFAULT 'gandia' COMMENT 'a que ayuntamiento pertenece' AFTER codigo;
ALTER TABLE nodo ALTER COLUMN ayuntamiento_id DROP DEFAULT;
ALTER TABLE nodo ADD CONSTRAINT nodo_ayuntamiento_fk FOREIGN KEY IF NOT EXISTS (ayuntamiento_id) REFERENCES ayuntamiento (id);

ALTER TABLE punto_reparto ADD COLUMN IF NOT EXISTS ayuntamiento_id VARCHAR(32) NOT NULL DEFAULT 'gandia' COMMENT 'a que ayuntamiento pertenece' AFTER id;
ALTER TABLE punto_reparto ALTER COLUMN ayuntamiento_id DROP DEFAULT;
ALTER TABLE punto_reparto ADD CONSTRAINT punto_reparto_ayuntamiento_fk FOREIGN KEY IF NOT EXISTS (ayuntamiento_id) REFERENCES ayuntamiento (id);

ALTER TABLE empleado ADD COLUMN IF NOT EXISTS ayuntamiento_id VARCHAR(32) NOT NULL DEFAULT 'gandia' COMMENT 'para que ayuntamiento trabaja' AFTER usuario_id;
ALTER TABLE empleado ALTER COLUMN ayuntamiento_id DROP DEFAULT;
ALTER TABLE empleado ADD CONSTRAINT empleado_ayuntamiento_fk FOREIGN KEY IF NOT EXISTS (ayuntamiento_id) REFERENCES ayuntamiento (id);
