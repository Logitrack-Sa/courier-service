# courier-service

## Entrega integrada del Sprint 2

Incluye las funciones de Pablo (actualización, estado, disponibilidad y validación de DNI único), conserva JWT/UUID/Flyway de develop y usa Config Client compatible con Boot 3.5.16. Dev/prod requieren CONFIG_SERVER_URL; test se mantiene aislado. DB_URL, DB_USERNAME, DB_PASSWORD y JWT_PUBLIC_KEY_LOCATION se suministran al arrancar. La imagen atiende en 0.0.0.0:8082 con UID 10001.

La suite comprueba persistencia real, UUID conservado, actualización, unicidad de DNI, courier inactivo, roles y bootstrap remoto dev/prod. La orquestación y publicación de las cuatro imágenes están en shipment-service, issues #64/#65.

Extracción del CRUD de repartidores del Sprint 1 sobre la rama de Damián, issue #2. Java 25 y Spring Boot 3.5.16, Maven Wrapper, JPA y Flyway. Base exclusiva: courier_db. Conserva entidades, DTOs, mapper, servicio, controller y errores del trabajo previo.

## Arrancar y probar

Definir DB_URL, DB_USERNAME y DB_PASSWORD de una base MySQL propia. `./mvnw spring-boot:run`; SERVER_PORT opcional (8082). Flyway crea el esquema y Hibernate lo valida. Sin usuarios/passwords por defecto. Auditoría técnica system mientras no exista la integración de identidad.

`./mvnw clean verify` usa H2 aislado. CI repite con MySQL 8.4, Flyway, persistencia y API reales. Para tests MySQL propios: TEST_DB_URL, TEST_DB_USERNAME y TEST_DB_PASSWORD, siempre sobre una base descartable.

## Contrato conservado y ampliado

- POST /api/v1/couriers: `name`, `dni` (7 a 8 dígitos numéricos, único) y `vehicleType`; 201 y Location. Estado inicial ACTIVO, UUID generado.
- PUT /api/v1/couriers/{id}: actualiza `name`, `dni` y `vehicleType` validando unicidad del DNI.
- GET /api/v1/couriers: página con content/page, filtro status opcional.
- GET /api/v1/couriers/{id}: DTO `id`, `name`, `dni`, `vehicleType`, `status`, `createdAt`.
- GET /api/v1/couriers/{id}/availability: devuelve `courierId`, `status` y `available`.
- PATCH /api/v1/couriers/{id}/status: ACTIVO o INACTIVO; implementación existente conservada.

400 para campos o UUID inválidos (incluyendo formato de DNI inválido), 404 si no existe y 422 (`DUPLICATE_DNI`) si el DNI ya está registrado. La migración Flyway `V2__add_dni_to_couriers.sql` agrega la columna `dni` con restricción única (`uk_couriers_dni`). Los requests no aceptan id ni auditoría. [Traslado de UUID y datos](docs/MIGRACION.md).

## Seguridad JWT (S2-15)

El servicio funciona como Resource Server: valida localmente firma RS256, `exp`, `issuer`, `audience`, `kid`, roles y sujeto; no consulta a security-service por cada petición. El CRUD, el listado, el cambio de estado y la disponibilidad quedan reservados a ADMIN. `GET /api/v1/internal/couriers/{id}/status` permite ADMIN o el REPARTIDOR cuyo `courierId` firmado coincide con la ruta y devuelve solo `courierId` y `status`. Cualquier endpoint no declarado responde 403.

Variables requeridas: `JWT_PUBLIC_KEY_LOCATION` (PEM público), `JWT_KEY_ID`, `JWT_ISSUER` y `JWT_AUDIENCE`. Las claves de prueba son efímeras. Ver [detalle de S2-15](docs/S2-15-SEGURIDAD.md).

Disponibilidad, estado interno, seguridad, Config Client y Docker están integrados. Los perfiles dev/prod reciben de Config Server el issuer, audience y kid mediante variables obligatorias, sin valores implícitos. El servicio escucha solo en 127.0.0.1 por defecto; la imagen configura `SERVER_ADDRESS=0.0.0.0` para su red Docker. No agregar entidades ni dependencias de shipment.

[Contrato del equipo](https://github.com/Logitrack-Sa/shipment-service/blob/main/docs/sprint2/CONTRATOS.md) · [Tablero](https://github.com/orgs/Logitrack-Sa/projects/2). La revisión IA no sustituye tests ni revisión humana.
