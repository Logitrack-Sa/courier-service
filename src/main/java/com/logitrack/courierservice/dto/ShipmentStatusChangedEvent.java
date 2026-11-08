package com.logitrack.courierservice.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de dominio consumido desde RabbitMQ cuando un envío cambia de estado operativo en shipment-service.
 */
public record ShipmentStatusChangedEvent(
        UUID eventId,
        String shipmentId,
        String trackingCode,
        String courierId,
        String previousStatus,
        String currentStatus,
        Instant occurredAt
) {
}
