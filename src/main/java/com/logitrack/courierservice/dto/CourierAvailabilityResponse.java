package com.logitrack.courierservice.dto;

import com.logitrack.courierservice.entity.CourierStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record CourierAvailabilityResponse(
        @Schema(description = "ID único del repartidor", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID courierId,

        @Schema(description = "Estado actual del repartidor", example = "ACTIVO")
        CourierStatus status,

        @Schema(description = "Disponibilidad del repartidor (ACTIVO -> true, INACTIVO -> false)", example = "true")
        boolean available
) {}