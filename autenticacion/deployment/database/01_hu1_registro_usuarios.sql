-- =====================================================================
-- HU1 - Registrar usuarios en el sistema
-- Ajusta la tabla "usuario" a los datos que pide la historia de usuario:
--   nombres, apellidos, fecha_nacimiento, direccion, telefono,
--   correo_electronico y salario_base.
--
-- Ejecutar sobre la base mi_db (PostgreSQL), por ejemplo:
--   docker exec -i postgres_local psql -U postgres -d mi_db -1 < deployment/database/01_hu1_registro_usuarios.sql
-- =====================================================================

-- Datos que la HU exige y que la tabla no tenía
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS fecha_nacimiento DATE;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS direccion VARCHAR(200);

-- La HU exige salario_base: se vuelve obligatorio también en la base
ALTER TABLE usuario ALTER COLUMN salario_base SET NOT NULL;

-- La HU no pide estos datos al registrar; las columnas se conservan
-- para historias futuras (roles, documento) pero dejan de ser obligatorias
ALTER TABLE usuario ALTER COLUMN documento_identidad DROP NOT NULL;
ALTER TABLE usuario ALTER COLUMN id_rol DROP NOT NULL;
