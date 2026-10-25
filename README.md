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

Seguridad JWT, endpoints de disponibilidad/estado interno, Config Client y Docker pertenecen a otras issues. Esta base CRUD **no tiene autorización**: escucha solo en 127.0.0.1 por defecto. No exponerla en una red compartida antes de integrar seguridad; SERVER_ADDRESS permite preparar el despliegue posterior. No agregar entidades ni dependencias de shipment.

[Contrato del equipo](https://github.com/Logitrack-Sa/shipment-service/blob/main/docs/sprint2/CONTRATOS.md) · [Tablero](https://github.com/orgs/Logitrack-Sa/projects/2). La revisión IA no sustituye tests ni revisión humana.
