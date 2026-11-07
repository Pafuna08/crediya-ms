-- =====================================================================
-- HU2 - Registrar una solicitud de préstamo
-- Tablas del microservicio SOLICITUDES (base de datos solicitudes_db).
--
-- 1) Crear la base (una sola vez, conectado a la base "postgres"):
--      CREATE DATABASE solicitudes_db;
-- 2) Ejecutar este script sobre solicitudes_db, por ejemplo:
--      docker exec -i postgres_local psql -U postgres -d solicitudes_db -1 < deployment/database/01_hu2_registro_solicitud.sql
-- =====================================================================

-- Estados por los que pasa una solicitud
CREATE TABLE IF NOT EXISTS estados (
    id_estado   SERIAL       PRIMARY KEY,
    nombre      VARCHAR(50)  NOT NULL UNIQUE,
    descripcion VARCHAR(255)
);

-- Tipos de préstamo que ofrece CrediYa
CREATE TABLE IF NOT EXISTS tipo_prestamo (
    id_tipo_prestamo SERIAL        PRIMARY KEY,
    nombre           VARCHAR(100)  NOT NULL UNIQUE,
    tasa_interes     NUMERIC(5, 2) NOT NULL CHECK (tasa_interes >= 0) -- % mensual
);

-- Solicitudes de préstamo enviadas por los clientes
CREATE TABLE IF NOT EXISTS solicitud (
    id_solicitud        SERIAL         PRIMARY KEY,
    documento_identidad VARCHAR(20)    NOT NULL,
    monto               NUMERIC(15, 2) NOT NULL CHECK (monto > 0),
    plazo               INTEGER        NOT NULL CHECK (plazo > 0),     -- en meses
    id_tipo_prestamo    INTEGER        NOT NULL REFERENCES tipo_prestamo (id_tipo_prestamo),
    id_estado           INTEGER        NOT NULL REFERENCES estados (id_estado),
    fecha_creacion      TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_solicitud_documento ON solicitud (documento_identidad);
CREATE INDEX IF NOT EXISTS idx_solicitud_estado ON solicitud (id_estado);

-- Estados (los nombres se usan en la HU2 y la HU4)
INSERT INTO estados (nombre, descripcion) VALUES
    ('Pendiente de revisión', 'La solicitud fue registrada y espera ser evaluada'),
    ('Revision manual',       'La solicitud requiere la revisión de un asesor'),
    ('Rechazada',             'La solicitud fue rechazada'),
    ('Aprobada',              'La solicitud fue aprobada')
ON CONFLICT (nombre) DO NOTHING;

-- Tipos de préstamo de EJEMPLO: ajustar nombres y tasas según lo defina el negocio
INSERT INTO tipo_prestamo (nombre, tasa_interes) VALUES
    ('Libre inversión', 1.80),
    ('Vehículo',        1.30),
    ('Vivienda',        1.00),
    ('Educación',       1.10)
ON CONFLICT (nombre) DO NOTHING;
