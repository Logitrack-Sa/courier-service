package com.logitrack.courierservice.repository;

import com.logitrack.courierservice.entity.ShipmentCourierProjection;
import com.logitrack.courierservice.messaging.ShipmentStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentCourierProjectionRepository
        extends JpaRepository<ShipmentCourierProjection, String> {

    @Modifying
    /*
     * MySQL puede convertir determinadas condiciones inválidas de INSERT IGNORE en advertencias.
     * La validación previa reduce ese riesgo y, según el esquema y las restricciones actuales, un
     * resultado cero se interpreta como una proyección shipment_id concurrente ya existente. Toda
     * excepción SQL que sí se propague revierte también el claim de eventId.
     */
    @Query(value = "insert ignore into shipment_courier_projection "
            + "(shipment_id, courier_id, tracking_code, status, occurred_at, version) "
            + "values (:shipmentId, :courierId, :trackingCode, :status, :occurredAt, 0)",
            nativeQuery = true)
    int initialize(@Param("shipmentId") String shipmentId,
            @Param("courierId") String courierId,
            @Param("trackingCode") String trackingCode,
            @Param("status") String status,
            @Param("occurredAt") Instant occurredAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select projection from ShipmentCourierProjection projection "
            + "where projection.shipmentId = :shipmentId")
    Optional<ShipmentCourierProjection> findByShipmentIdForUpdate(
            @Param("shipmentId") String shipmentId);

    long countByCourierIdAndStatusAndShipmentIdNot(
            String courierId, ShipmentStatus status, String shipmentId);
}
