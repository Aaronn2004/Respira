-- ==============================================================
-- Fichero:     actualizar_seguimiento.sql  (MariaDB - Plesk)
-- Descripción: Añade a la BD actual (la de esquema.sql del 10/11/2026)
--              las tablas «favorito» y «alerta»: los parques que sigue
--              cada cuenta y sus alertas, que antes vivían solo en el móvil.
--              NO borra nada. Se ejecuta UNA vez desde phpMyAdmin
--              (pestaña «Importar»); si se repite, no hace nada.
-- Diseño:      doc/base_de_datos.md
-- Autor:       Aaron Blasco Blay
-- Fecha:       13/11/2026
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
