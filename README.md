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
