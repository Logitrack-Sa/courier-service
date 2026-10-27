package com.logitrack.courierservice.dto;

import com.logitrack.courierservice.entity.CourierStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CourierStatusRequest(

        @Schema(
                description = "Nuevo estado del repartidor",
                example = "ACTIVo"
        )
        @NotNull
        CourierStatus status
) {
}