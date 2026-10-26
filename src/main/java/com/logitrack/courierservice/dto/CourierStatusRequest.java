package com.logitrack.courierservice.dto;

import com.logitrack.courierservice.entity.CourierStatus;
import jakarta.validation.constraints.NotNull;

public record CourierStatusRequest(

        @NotNull
        CourierStatus status
) {
}
