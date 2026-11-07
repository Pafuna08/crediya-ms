# CrediYa

Plataforma de solicitudes de préstamo organizada en microservicios.
Cada microservicio es una aplicación Spring Boot independiente (Spring MVC, arquitectura hexagonal
generada con el plugin de arquitectura limpia de Bancolombia), con su propio `gradlew`, puerto y base de datos.

| Microservicio | Carpeta | Puerto | Base de datos | Historias |
|---|---|---|---|---|
| Autenticación | [`autenticacion/`](autenticacion/) | 8080 | `mi_db` | HU1 Registrar usuarios |
| Solicitudes | [`solicitudes/`](solicitudes/) | 8081 | `solicitudes_db` | HU2 Registrar solicitud de préstamo |

## Requisitos

- Java 25 (Gradle lo descarga con toolchains si no está instalado)
- PostgreSQL 17 en `localhost:5432` (contenedor Docker `postgres_local` dentro de Ubuntu/WSL)

## Base de datos

Los scripts de cada microservicio están en `<microservicio>/deployment/database/` y se ejecutan en orden:

```bash
# Autenticación (base mi_db)
docker exec -i postgres_local psql -U postgres -d mi_db -1 < autenticacion/deployment/database/01_hu1_registro_usuarios.sql

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

| Método | Ruta | Microservicio | Descripción |
|---|---|---|---|
| POST | `/api/v1/usuarios` | Autenticación | Registra un usuario |
| POST | `/api/v1/solicitud` | Solicitudes | Registra una solicitud de préstamo en estado "Pendiente de revisión" |

## Flujo de ramas

- `main`: versión estable.
- `develop`: integración de las historias terminadas.
- `feature/huN-...`: una rama por historia de usuario; se une a `develop` y luego `develop` a `main`.
