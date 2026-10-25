# S2-15 · Proteger courier-service por JWT y separar consultas internas

## Contexto

Esta entrega implementa el Resource Server de `courier-service`, la matriz de
permisos del contrato del Sprint 2 y el endpoint interno mínimo de estado. No
agrega API Gateway ni valida tokens contra `security-service` en cada petición.

La rama de trabajo parte de `origin/codex/s2-21-completar-base` (PR #10 de la
issue S2-21) porque `main` todavía no contenía la aplicación Java. De esa forma
la seguridad se prueba contra el CRUD real sin reescribirlo. Cuando S2-21 se
integre a `main`, esta rama solo debe aportar los archivos de seguridad, el DTO
y el controller interno. No se modificaron entidades, repositorios ni reglas de
negocio de repartidores.

## Alcance implementado

| Archivo | Responsabilidad |
|---|---|
| `security/JwtProperties.java` | Clave pública, `kid`, issuer y audience configurables. |
| `security/JwtContractValidator.java` | Sujeto UUID, roles conocidos, `kid`, audience y ventana temporal. |
| `security/SecurityConfig.java` | Decoder RS256 local, cadena stateless y matriz de permisos. |
| `security/SecurityErrors.java` | Respuestas JSON homogéneas para 401 y 403. |
| `security/InternalCourierAuthorization.java` | ADMIN o REPARTIDOR dueño del `courierId` firmado. |
| `dto/CourierStatusResponse.java` | Respuesta mínima `courierId`, `status`. |
| `service/CourierStatusQueryService.java` | Consulta de solo lectura separada del CRUD. |
| `controller/InternalCourierController.java` | `GET /api/v1/internal/couriers/{id}/status`. |

## Validación local del JWT

El decodificador se crea con la clave pública montada por entorno y la
verificación se realiza en el proceso:

- firma RS256 y algoritmo permitido;
- `exp`, `iat` y `nbf` mediante los validadores estándar de Spring Security;
- `iss` igual a `JWT_ISSUER`;
- `aud` contiene `JWT_AUDIENCE` (`courier-service`);
- `kid` igual a `JWT_KEY_ID`;
- `sub` es un UUID;
- `roles` es una lista no vacía y solo contiene `ADMIN`, `CLIENTE` o
  `REPARTIDOR`.

No existe llamada HTTP al emisor durante la autenticación.

## Matriz de permisos

| Operación | ADMIN | CLIENTE | REPARTIDOR | Sin token |
|---|---|---|---|---|
| `POST /api/v1/couriers` | 201 | 403 | 403 | 401 |
| `GET /api/v1/couriers` y `/{id}` | 200 | 403 | 403 | 401 |
| `PATCH /api/v1/couriers/{id}/status` | 200 | 403 | 403 | 401 |
| `GET /api/v1/couriers/{id}/availability` | habilitado por patrón | 403 | 403 | 401 |
| `GET /api/v1/internal/couriers/{id}/status` | 200 de cualquier id | 403 | 200 solo si `id == courierId` | 401 |
| Cualquier otra ruta | 403 | 403 | 403 | 401 |

La disponibilidad se protege desde ya con el patrón `/api/v1/couriers/**`
aunque su implementación HTTP pertenezca a S2-23. Cualquier endpoint nuevo que
no se declare explícitamente queda denegado por `anyRequest().denyAll()`.

## Contrato del endpoint interno

```http
GET /api/v1/internal/couriers/20000000-0000-0000-0000-000000000001/status
Authorization: Bearer <jwt>
```

```json
{
  "courierId": "20000000-0000-0000-0000-000000000001",
  "status": "ACTIVO"
}
```

Respuestas:

- 200 con únicamente `courierId` y `status`;
- 401 si falta el token o es inválido;
- 403 si el rol no corresponde, falta `courierId` o el UUID de la ruta es
  distinto al firmado;
- 404 si ADMIN consulta un repartidor inexistente.

El controller no decide la pertenencia: la autorización se resuelve en la
cadena de filtros antes de invocar el método y así los errores de seguridad no
se mezclan con el manejo global de excepciones.

## Configuración

```properties
logitrack.security.public-key-location=${JWT_PUBLIC_KEY_LOCATION}
logitrack.security.key-id=${JWT_KEY_ID:logitrack-main}
logitrack.security.issuer=${JWT_ISSUER:https://security-service}
logitrack.security.audience=${JWT_AUDIENCE:courier-service}
```

`JWT_PUBLIC_KEY_LOCATION` es obligatoria y apunta a la clave pública PEM. No se
versionan claves privadas ni se reutilizan claves reales en tests.

## Pruebas

La suite agrega `JwtTestSupport`, que genera un par RSA efímero por ejecución y
firma tokens locales con fixtures de identidad. No depende de
`security-service`.

Escenarios cubiertos en `CourierSecurityIntegrationTest`:

- 401 estructurado con `WWW-Authenticate` para token ausente;
- rechazo de firma manipulada, token vencido, `iss`, `aud`, `kid`, algoritmo,
  `sub`, `roles`, `iat` y `exp` inválidos;
- ADMIN puede crear, listar, consultar, cambiar estado y usar el endpoint
  interno;
- CLIENTE y REPARTIDOR reciben 403 en CRUD y disponibilidad;
- REPARTIDOR solo consulta su propio `courierId`; un id ajeno, un claim ausente
  o malformado devuelve 403;
- rutas internas no declaradas y rutas desconocidas devuelven 403;
- ADMIN recibe 404 para un courier inexistente;
- métodos distintos de GET sobre la ruta interna quedan denegados.

Comando ejecutado:

```bash
./mvnw -q test
```

Resultado local: **25 pruebas, 0 fallos, 0 errores**. De ellas, 9 corresponden
a `CourierSecurityIntegrationTest`. Los tests de CRUD previos conservan su
cobertura; los que prueban persistencia pura desactivan los filtros HTTP para
no mezclar responsabilidades.

## Criterios de aceptación

- [x] JWT verificado localmente: firma, `exp`, issuer y audience; sin validación
  remota por petición.
- [x] CRUD, listado, actualización, estado y disponibilidad protegidos para
  ADMIN mediante patrones de ruta.
- [x] Endpoint interno mínimo permite ADMIN o REPARTIDOR con `courierId`
  firmado igual a la ruta y no expone datos de otro repartidor.
- [x] 401/403 estructurados, pruebas de acceso cruzado y denegación por defecto
  de endpoints nuevos.

## Integración y revisión

Al abrir el PR debe indicarse que la rama parte de S2-21/PR #10. La revisión
humana debe comprobar que el merge final no reintroduce reglas de negocio ni
modifica migraciones. La comprobación con `security-service` real queda para la
integración final; las pruebas de esta entrega usan únicamente JWT locales y
H2 aislado.
