package com.logitrack.courierservice.messaging;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/** Contrato local del evento; no depende de clases de shipment-service. */
public record ShipmentStatusChangedEvent(
        @NotNull UUID eventId,
        @NotBlank @Size(max = 36) String shipmentId,
        @NotBlank @Size(max = 13) String trackingCode,
        @Size(max = 36)
        String courierId,
        @NotNull ShipmentStatus previousStatus,
        @NotNull ShipmentStatus currentStatus,
        @NotNull Instant occurredAt
) {
}
