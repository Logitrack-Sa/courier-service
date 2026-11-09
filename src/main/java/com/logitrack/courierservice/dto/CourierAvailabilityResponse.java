package com.logitrack.courierservice.dto;

import com.logitrack.courierservice.entity.CourierStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record CourierAvailabilityResponse(
        @Schema(description = "ID único del repartidor", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID courierId,

        @Schema(description = "Proyección ACTIVO/INACTIVO compatible con shipment-service; "
                + "no es el estado administrativo persistido", example = "ACTIVO")
        CourierStatus status,

        @Schema(description = "Disponibilidad efectiva: habilitación ACTIVO y disponibilidad AVAILABLE", example = "true")
        boolean available
) {}
