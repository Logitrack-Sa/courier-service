package com.logitrack.courierservice.dto;

import com.logitrack.courierservice.entity.CourierStatus;
import java.util.UUID;

/**
 * Respuesta mínima del endpoint interno consumido por shipment-service.
 *
 * <p>No incluye nombre, vehículo, auditoría ni datos de contacto para evitar
 * que un repartidor reciba información ajena a su propia asignación.</p>
 *
 * @param courierId identificador del repartidor consultado.
 * @param status estado operativo actual.
 */
public record CourierStatusResponse(UUID courierId, CourierStatus status) {
}
