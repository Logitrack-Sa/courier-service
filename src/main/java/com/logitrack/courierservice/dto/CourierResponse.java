package com.logitrack.courierservice.dto;

import com.logitrack.courierservice.entity.CourierStatus;
import java.time.Instant;
import java.util.UUID;

public record CourierResponse(
        UUID id,
        String name,
        String dni,
        String vehicleType,
        CourierStatus status,
        Instant createdAt,
        boolean available
) {
    public CourierResponse(UUID id, String name, String dni, String vehicleType,
            CourierStatus status, Instant createdAt) {
        this(id, name, dni, vehicleType, status, createdAt, status == CourierStatus.ACTIVO);
    }
}
