# Trasladar couriers sin cambiar UUID

Hacerlo durante mantenimiento sin escrituras concurrentes, con respaldo completo y restauración previamente comprobada. El servicio solo usa courier_db; no establece conexión con shipment_db.

1. Inicializar la base destino vacía ejecutando esta aplicación con Flyway. El V1 conserva las nueve columnas de couriers del Sprint 1, incluyendo BINARY(16), auditoría y versión. Detener la aplicación antes de importar.
2. Exportar desde una copia verificada del origen con una cuenta de solo lectura. El siguiente comando produce únicamente datos y UUID en hexadecimal; guardar el archivo fuera de Git:

```shell
mysqldump --host=HOST_ORIGEN --user=USUARIO_LECTURA --password --single-transaction --no-create-info --skip-add-drop-table --skip-add-locks --skip-triggers --complete-insert --hex-blob --no-tablespaces --set-gtid-purged=OFF shipment_db couriers > couriers-data.sql
```

3. Inspeccionar el SQL: solo INSERT de couriers, sin CREATE/DROP, USE, otras tablas o UUID generados. Importar en la base vacía, sin opciones --force, IGNORE ni REPLACE. Si falla, detener y restaurar la base descartable desde un estado conocido; no continuar parcialmente.

```shell
mysql --host=HOST_DESTINO --user=USUARIO_DESTINO --password courier_db < couriers-data.sql
```

4. Comparar COUNT y todas las columnas ordenadas por HEX(id) entre copia y destino; verificar que cada shipments.courier_id asignado exista. No reinsertar por POST: produciría UUID nuevos. Conservar respaldo y tabla original hasta completar la integración.
5. Ejecutar consultas por ID y estado, y luego la validación de shipment con su V2. Esta extracción no borra datos ni migra la base del usuario automáticamente.

La prueba importsExistingUuidDataAndAuditWithoutRegeneration inserta un registro con el formato de exportación, lo relee mediante JPA y HTTP y comprueba UUID, datos, timestamps, autores y versión. CI repite esa prueba con MySQL real.
