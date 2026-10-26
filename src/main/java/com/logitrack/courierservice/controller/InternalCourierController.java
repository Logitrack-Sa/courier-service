package com.logitrack.courierservice.controller;

import com.logitrack.courierservice.dto.CourierStatusResponse;
import com.logitrack.courierservice.service.CourierStatusQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone el contrato interno mínimo que shipment-service usa al asignar y
 * avanzar un envío.
 *
 * <p>La autorización se aplica en la cadena de filtros: ADMIN puede consultar
 * cualquier repartidor y REPARTIDOR solo el {@code courierId} firmado en su
 * token. La respuesta se limita a identificador y estado.</p>
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/internal/couriers")
@Tag(name = "Couriers internos", description = "Estado mínimo para integración entre servicios")
public class InternalCourierController {

    private final CourierStatusQueryService courierStatusQueryService;

    /**
     * Consulta el estado operativo mínimo de un repartidor.
     *
     * @param id identificador del repartidor solicitado.
     * @return {@code courierId} y {@code status} sin datos adicionales.
     */
    @GetMapping("/{id}/status")
    @Operation(summary = "Consultar el estado mínimo interno de un repartidor")
    public CourierStatusResponse findStatus(@PathVariable UUID id) {
        return courierStatusQueryService.findStatus(id);
    }
}
