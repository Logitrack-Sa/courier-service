
package com.logitrack.courierservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourierRequest(

        @Schema(
                description = "Nombre del repartidor",
                example = "Juan Pérez"
        )
        @NotBlank
        @Size(max = 120)
        String name,

        @Schema(
                description = "Tipo de vehículo del repartidor",
                example = "MOTOVEHÍCULO"
        )
        @NotBlank
        @Size(max = 50)
        String vehicleType
) {
}
