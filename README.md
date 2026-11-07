# CrediYa

Plataforma de solicitudes de préstamo organizada en microservicios.
Cada microservicio es una aplicación Spring Boot independiente (Spring MVC, arquitectura hexagonal
generada con el plugin de arquitectura limpia de Bancolombia), con su propio `gradlew`, puerto y base de datos.

| Microservicio | Carpeta | Puerto | Base de datos | Historias |
|---|---|---|---|---|
| Autenticación | [`autenticacion/`](autenticacion/) | 8080 | `mi_db` | HU1 Registrar usuarios, HU3 Autenticación |
| Solicitudes | [`solicitudes/`](solicitudes/) | 8081 | `solicitudes_db` | HU2 Registrar solicitud de préstamo, HU3 Autenticación |

## Requisitos

- Java 25 (Gradle lo descarga con toolchains si no está instalado)
- PostgreSQL 17 en `localhost:5432` (contenedor Docker `postgres_local` dentro de Ubuntu/WSL)

## Base de datos

Los scripts de cada microservicio están en `<microservicio>/deployment/database/` y se ejecutan en orden:

```bash
# Autenticación (base mi_db)
docker exec -i postgres_local psql -U postgres -d mi_db -1 < autenticacion/deployment/database/01_hu1_registro_usuarios.sql
docker exec -i postgres_local psql -U postgres -d mi_db -1 < autenticacion/deployment/database/02_hu3_autenticacion.sql

# Solicitudes (crear la base una sola vez y luego aplicar el script)
docker exec postgres_local psql -U postgres -c "CREATE DATABASE solicitudes_db"
docker exec -i postgres_local psql -U postgres -d solicitudes_db -1 < solicitudes/deployment/database/01_hu2_registro_solicitud.sql
```

Usuario, contraseña y URL se pueden cambiar con las variables de entorno `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`.

## Ejecutar

Cada microservicio se levanta desde su carpeta:

```bash
cd autenticacion && ./gradlew :app-service:bootRun   # http://localhost:8080
cd solicitudes   && ./gradlew :app-service:bootRun   # http://localhost:8081
```

## Pruebas

```bash
cd autenticacion && ./gradlew test
cd solicitudes   && ./gradlew test
```

## Endpoints

| Método | Ruta | Microservicio | Quién puede usarlo | Descripción |
|---|---|---|---|---|
| POST | `/api/v1/login` | Autenticación | Cualquiera | Inicia sesión con correo y clave; devuelve un token JWT |
| POST | `/api/v1/usuarios` | Autenticación | ADMIN, ASESOR | Registra un usuario con su clave y su rol |
| POST | `/api/v1/solicitud` | Solicitudes | CLIENTE | Registra una solicitud de préstamo a nombre del propio cliente, en estado "Pendiente de revisión" |

## Autenticación (HU3)

1. Iniciar sesión:
   ```http
   POST http://localhost:8080/api/v1/login
   { "correo_electronico": "admin@crediya.com", "clave": "Admin123*" }
   ```
2. Enviar el token recibido en las demás peticiones:
   ```http
   Authorization: Bearer <token>
   ```

- Roles: `ADMIN` (1), `ASESOR` (2), `CLIENTE` (3). Al registrar un usuario se indica `id_rol`.
- Administrador inicial **solo para desarrollo**: `admin@crediya.com` / `Admin123*` (lo crea el script `02_hu3_autenticacion.sql`).
- Los tokens duran 1 hora y los firma autenticación con la clave `JWT_SECRET`. Solicitudes valida la firma con **la misma** clave,
  así que ambos microservicios deben usar el mismo valor. El valor por defecto de `application.yaml` es solo para desarrollo.
- Las claves se guardan cifradas con BCrypt. Los intentos de inicio de sesión son ilimitados.

## Flujo de ramas

- `main`: versión estable.
- `develop`: integración de las historias terminadas.
- `feature/huN-...`: una rama por historia de usuario; se une a `develop` y luego `develop` a `main`.
