-- =====================================================================
-- HU3 - Agregar autenticación al sistema
-- - Cada usuario necesita una clave (guardada cifrada con BCrypt) y un rol.
-- - El documento de identidad pasa a ser obligatorio: identifica al cliente
--   para que solo pueda crear solicitudes de préstamo a su nombre.
-- - Se crea un administrador inicial para poder registrar a los demás usuarios.
--
-- Ejecutar sobre la base mi_db, por ejemplo:
--   docker exec -i postgres_local psql -U postgres -d mi_db -1 < deployment/database/02_hu3_autenticacion.sql
-- =====================================================================

-- Permite cifrar la clave del administrador inicial con BCrypt desde SQL
CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE usuario ADD COLUMN IF NOT EXISTS clave VARCHAR(100);

-- Roles del sistema (los nombres se usan en los permisos de los endpoints)
INSERT INTO rol (nombre, descripcion) VALUES
    ('ADMIN',   'Administrador del sistema'),
    ('ASESOR',  'Asesor que registra usuarios y revisa solicitudes'),
    ('CLIENTE', 'Cliente que solicita préstamos')
ON CONFLICT (nombre) DO NOTHING;

-- Administrador inicial. SOLO PARA DESARROLLO: cambiar la clave en otros ambientes.
--   correo: admin@crediya.com   clave: Admin123*
INSERT INTO usuario (nombre, apellido, email, documento_identidad, salario_base, id_rol, clave)
SELECT 'Administrador', 'CrediYa', 'admin@crediya.com', '1000000000', 0, r.id_rol,
       crypt('Admin123*', gen_salt('bf', 10))
FROM rol r
WHERE r.nombre = 'ADMIN'
ON CONFLICT (email) DO NOTHING;

-- Desde esta HU todo usuario debe tener clave, rol y documento
ALTER TABLE usuario ALTER COLUMN clave SET NOT NULL;
ALTER TABLE usuario ALTER COLUMN id_rol SET NOT NULL;
ALTER TABLE usuario ALTER COLUMN documento_identidad SET NOT NULL;
