# courier-service

Repositorio inicial del Sprint 2 de LogiTrack. **Todavía no implementa el servicio ni contiene configuración productiva.**

[Tablero del Sprint 2](https://github.com/orgs/Logitrack-Sa/projects/2) · [Contratos y puertos](https://github.com/Logitrack-Sa/shipment-service/blob/main/docs/sprint2/CONTRATOS.md) · [Workflow](https://github.com/Logitrack-Sa/shipment-service/blob/main/docs/sprint2/WORKFLOW.md)

Las issues del repositorio definen responsable, puntos, criterios y pruebas independientes. Usar fixtures y dobles para avanzar sin esperar servicios de otros integrantes. Se conserva el Sprint 1 en shipment-service.

Base actual: README, plantilla de PR, formulario de issues y revisor IA en GitHub Actions. No se agregan clases Java, POM, migraciones, Dockerfiles ni propiedades de aplicación hasta iniciar las issues.

La IA devuelve hallazgos sobre PR y sugerencias de pruebas; no planifica, no aprueba ni integra. CI de esta base verifica automatización. Las pruebas Maven y Docker se agregan en sus issues cuando exista la aplicación.

No incorporar credenciales, claves JWT privadas ni archivos .env. Java25 para aplicaciones; config-repo es únicamente Git de propiedades no sensibles dev/prod.

## Punto de partida de la extracción

Reutilizar el CRUD de repartidores existente en shipment-service: entidad Courier, repositorio, mapper, DTOs, servicio, controller y manejo de errores. Conservar los UUID al migrar los datos; los envíos los guardarán como referencias escalares.

Los endpoints de disponibilidad y estado interno mínimo están definidos en el contrato central. La base MySQL pertenece exclusivamente a este servicio. Probar HTTP y JWT con fixtures antes de integrar las aplicaciones reales; la seguridad y el CRUD tienen responsables separados en las issues.

# Courier Service - 

Microservicio independiente responsable de la administración de repartidores y el control de su disponibilidad operativa dentro de la arquitectura de LogiTrack[cite: 1, 2].

---

## 🚀 Tecnologías Utilizadas
* **Java 25**
* **Spring Boot**
* **Spring Data JPA / MySQL** (Base de datos exclusiva del microservicio)
* **Springdoc OpenAPI (Swagger UI)** para documentación interactiva de la API.

---

## Endpoints REST Principales

### Endpoints Públicos (`/api/v1/couriers`)
* **`POST /api/v1/couriers`**: Registra un nuevo repartidor en el sistema.
* **`GET /api/v1/couriers`**: Lista los repartidores de forma paginada (permite filtro opcional por `status`).
* **`GET /api/v1/couriers/{id}`**: Obtiene la información detallada de un repartidor por su ID.
* **`PUT /api/v1/couriers/{id}`**: Actualiza la información del repartidor.
* **`PATCH /api/v1/couriers/{id}/status`**: Cambia el estado operativo del repartidor (`ACTIVO` / `INACTIVO`).
* **`GET /api/v1/couriers/{id}/availability`** *(Issue S2-23)*: Consulta la disponibilidad para asignación. Devuelve `courierId`, `status` y `available` (`ACTIVO` $\rightarrow$ `true`, `INACTIVO` $\rightarrow$ `false`). Retorna `404` si no existe.

###Endpoints Internos (`/api/v1/internal/couriers`)
* **`GET /api/v1/internal/couriers/{id}/status`** *(Issue S2-23)*: Endpoint interno optimizado para la comunicación entre microservicios (consumido por `shipment-service`). Devuelve exclusivamente `courierId` y `status` sin exponer datos sensibles (nombre, vehículo, etc.).

---

## Pruebas
El proyecto cuenta con pruebas unitarias (Mockito) y de integración HTTP (MockMvc) aisladas mediante dobles y fixtures, permitiendo validar contratos sin depender de otros servicios:
```bash
mvn clean test
```

## Migración de Repartidores (Sprint 1 a Sprint 2)

Para cumplir con el desacoplamiento arquitectónico, `courier-service` opera con su propia base de datos MySQL exclusiva. 

**Reglas de Exportación e Integridad de Datos:**
1. **Conservación de UUIDs:** Al extraer y migrar los registros de repartidores desde el diseño del Sprint 1, se deben insertar conservando exactamente sus UUID originales. 
2. **Referencias Escalares:** Los envíos en `shipment-service` guardarán estos identificadores como referencias escalares, eliminando el acoplamiento de tablas.
3. **Aislamiento de Bases de Datos:** Queda estrictamente prohibido compartir bases de datos o realizar accesos directos a las bases de otros servicios. Cualquier validación de repartidores se realiza a través de los contratos REST expuestos.

## Ejecución con Docker

### Requisitos previos

- Docker Desktop instalado y en ejecución.
- Acceso a una instancia MySQL compatible con la configuración del servicio.

### Construir la imagen

Desde la raíz del proyecto, ejecutar:

```bash
docker build -t logitrack/courier-service:sprint2 .
```

La imagen se construye mediante un proceso multietapa con Java 25, separando la compilación de la ejecución. El contenedor se ejecuta con un usuario sin privilegios.

### Configuración

El servicio utiliza variables de entorno para conectarse a MySQL y, opcionalmente, al servidor de configuración:

| Variable | Descripción | Valor predeterminado |
|---|---|---|
| `DB_HOST` | Host de MySQL | `localhost` |
| `DB_PORT` | Puerto de MySQL | `3306` |
| `DB_NAME` | Nombre de la base de datos | `courier_db` |
| `DB_USER` | Usuario de MySQL | `logitrack` |
| `DB_PASSWORD` | Contraseña de MySQL | Sin valor predeterminado |
| `CONFIG_SERVER_URL` | URL del servidor de configuración | `http://localhost:8888` |

No incluir contraseñas ni otros secretos en la imagen Docker o en el repositorio.

### Ejecutar el contenedor

Si MySQL está disponible en una red Docker y el contenedor de base de datos se llama `courier-mysql`, se puede ejecutar:

```bash
docker run -d \
  --name courier-service \
  --network courier-network \
  -p 8082:8082 \
  -e DB_HOST=courier-mysql \
  -e DB_PORT=3306 \
  -e DB_NAME=courier_db \
  -e DB_USER=logitrack \
  -e DB_PASSWORD="<tu_contraseña>" \
  -e CONFIG_SERVER_URL=http://configserver:8888 \
  logitrack/courier-service:sprint2
```

La red `courier-network` y la base de datos deben existir previamente, y los nombres deben coincidir con la configuración del entorno.

### Verificar el estado

Consultar el