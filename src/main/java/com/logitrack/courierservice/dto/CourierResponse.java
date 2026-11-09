package com.logitrack.courierservice.dto;

import com.logitrack.courierservice.entity.CourierStatus;
import com.logitrack.courierservice.entity.CourierAvailability;
import java.time.Instant;
import java.util.UUID;

public record CourierResponse(
        UUID id,
        String name,
        String dni,
        String vehicleType,
        CourierStatus status,
        CourierAvailability availability,
        Instant createdAt
) {
    public CourierResponse(UUID id, String name, String dni, String vehicleType,
            CourierStatus status, Instant createdAt) {
        this(id, name, dni, vehicleType, status, CourierAvailability.AVAILABLE, createdAt);
    }
}
