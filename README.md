# courier-service

Extracción del CRUD de repartidores del Sprint 1 sobre la rama de Damián, issue #2. Java 25 y Spring Boot 3.5.16, Maven Wrapper, JPA y Flyway. Base exclusiva: courier_db. Conserva entidades, DTOs, mapper, servicio, controller y errores del trabajo previo.

## Arrancar y probar

Definir DB_URL, DB_USERNAME y DB_PASSWORD de una base MySQL propia. `./mvnw spring-boot:run`; SERVER_PORT opcional (8082). Flyway crea el esquema y Hibernate lo valida. Sin usuarios/passwords por defecto. Auditoría técnica system mientras no exista la integración de identidad.

`./mvnw clean verify` usa H2 aislado. CI repite con MySQL 8.4, Flyway, persistencia y API reales. Para tests MySQL propios: TEST_DB_URL, TEST_DB_USERNAME y TEST_DB_PASSWORD, siempre sobre una base descartable.

## Contrato conservado

- POST /api/v1/couriers: name y vehicleType; 201 y Location. Estado inicial ACTIVO, UUID generado.
- GET /api/v1/couriers: página con content/page, filtro status opcional.
- GET /api/v1/couriers/{id}: DTO id, name, vehicleType, status, createdAt.
- PATCH /api/v1/couriers/{id}/status: ACTIVO o INACTIVO; implementación existente conservada.

400 para campos o UUID inválidos, 404 si no existe. Los requests no aceptan id ni auditoría. [Traslado de UUID y datos](docs/MIGRACION.md).

## Seguridad JWT (S2-15)

El servicio funciona como Resource Server: valida localmente firma RS256, `exp`, `issuer`, `audience`, `kid`, roles y sujeto; no consulta a security-service por cada petición. El CRUD, el listado, el cambio de estado y la disponibilidad quedan reservados a ADMIN. `GET /api/v1/internal/couriers/{id}/status` permite ADMIN o el REPARTIDOR cuyo `courierId` firmado coincide con la ruta y devuelve solo `courierId` y `status`. Cualquier endpoint no declarado responde 403.

Variables requeridas: `JWT_PUBLIC_KEY_LOCATION` (PEM público), `JWT_KEY_ID`, `JWT_ISSUER` y `JWT_AUDIENCE`. Las claves de prueba son efímeras. Ver [detalle de S2-15](docs/S2-15-SEGURIDAD.md).

Disponibilidad, estado interno y seguridad se integran en esta rama. Config Client y Docker pertenecen a otras issues. El servicio escucha solo en 127.0.0.1 por defecto; `SERVER_ADDRESS` permite preparar el despliegue posterior. No agregar entidades ni dependencias de shipment.

[Contrato del equipo](https://github.com/Logitrack-Sa/shipment-service/blob/main/docs/sprint2/CONTRATOS.md) · [Tablero](https://github.com/orgs/Logitrack-Sa/projects/2). La revisión IA no sustituye tests ni revisión humana.
