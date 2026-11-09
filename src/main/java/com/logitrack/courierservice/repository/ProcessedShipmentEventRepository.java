package com.logitrack.courierservice.repository;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ProcessedShipmentEventRepository extends Repository<com.logitrack.courierservice.entity.Courier, UUID> {
    /**
     * Claim atómico mediante INSERT IGNORE. MySQL también puede convertir ciertas condiciones
     * inválidas en advertencias, no solo ignorar duplicados. La validación previa reduce ese riesgo
     * y, con el esquema y las restricciones actuales, el resultado cero se interpreta como una PK
     * event_id ya confirmada. Toda excepción SQL que sí se propague provoca rollback completo.
     */
    @Modifying
    @Query(value = "insert ignore into processed_shipment_events(event_id, processed_at) "
            + "values (:eventId, :processedAt)", nativeQuery = true)
    int claim(@Param("eventId") String eventId, @Param("processedAt") Instant processedAt);
}
