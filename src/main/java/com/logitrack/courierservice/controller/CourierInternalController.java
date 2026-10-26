package com.logitrack.courierservice.controller;

import com.logitrack.courierservice.dto.CourierInternalStatusResponse;
import com.logitrack.courierservice.service.implementation.CourierServiceImp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/internal/couriers")
@Tag(name = "Couriers Internal", description = "Endpoints internos de repartidores para comunicación entre microservicios")
public class CourierInternalController {

    private static final Logger log = LoggerFactory.getLogger(CourierInternalController.class);

    private final CourierServiceImp courierService;

    @GetMapping("/{id}/status")
    @Operation(
            summary = "Obtener estado interno mínimo de un repartidor",
            description = "Endpoint interno que devuelve exclusivamente courierId y status. No expone datos sensibles como nombre o vehículo."
    )
    public CourierInternalStatusResponse getInternalStatus(@PathVariable UUID id) {
        log.debug("GET /api/v1/internal/couriers/{}/status", id);
        return courierService.getInternalStatus(id);
    }
}