# Consumo de estados de envío

`courier-service` consume `shipment.status.changed` desde el exchange topic durable
`shipment.topic.exchange`. La cola durable `courier.shipment-status.queue` usa el DLX direct
`courier.shipment-status.dlx` y la DLQ `courier.shipment-status.dlq`, sin retorno automático.

La habilitación administrativa sigue persistida en `couriers.status` como `ACTIVO/INACTIVO`.
La ocupación se persiste separadamente en `couriers.availability` como `AVAILABLE/BUSY`.
Los eventos nunca cambian el campo administrativo. El endpoint `/availability` proyecta
`ACTIVO` solo si ambas condiciones permiten asignar; de lo contrario proyecta `INACTIVO`, para
mantener el contrato Feign existente. El recurso Courier expone `availability` explícitamente.

La migración inicializa repartidores existentes como `AVAILABLE`. Antes de habilitar el listener
en un ambiente con envíos ya `EN_RUTA` se requiere una reconciliación inicial: no hay evidencia de
que shipment-service reproduzca automáticamente eventos históricos.

La tabla `processed_shipment_events` conserva indefinidamente el horizonte de replay por
`event_id`. Su claim atómico, la proyección por envío y el cambio de Courier confirman en una misma
transacción. Los eventos con `courierId: null` son la excepción deliberada: se confirman como no-op
repetible sin registrar deduplicación. La entrega total es al menos una vez, no exactly-once.

Los dos `INSERT IGNORE` se ejecutan después de validar UUID canónicos, longitudes, transición y
rango `DATETIME(6)`. Esta validación reduce el riesgo de que MySQL convierta condiciones inválidas
en advertencias, pero `INSERT IGNORE` no se limita a ignorar duplicados. Con el esquema y las
restricciones actuales, el servicio interpreta un resultado cero como el conflicto concurrente de
la clave correspondiente (`event_id` o `shipment_id`). Toda excepción SQL que sí se propague
provoca rollback de la transacción completa, incluido el claim de deduplicación.

Tras tres intentos configurables, un fallo rechazado se dead-letterea según las garantías del
broker. Los errores permanentes usan rechazo sin requeue. Después de corregir la causa, el replay
desde la DLQ es manual y debe conservar `eventId`; no existe ciclo DLQ → cola principal.

Una cola RabbitMQ preexistente con argumentos distintos no puede redeclararse. La migración
operativa debe recrearla de forma controlada; la aplicación nunca la borra automáticamente.
El listener se puede apagar con `SHIPMENT_STATUS_CONSUMER_ENABLED=false` y su arranque con
`SHIPMENT_STATUS_LISTENER_AUTO_STARTUP=false`.

La caché `couriers_available` de #108 no está presente en esta base. Cuando se integre, el cambio
de disponibilidad deberá invalidarla después del commit mediante el mecanismo definido por #108.
