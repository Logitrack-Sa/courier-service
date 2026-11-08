package com.logitrack.courierservice.listener;

import com.logitrack.courierservice.config.RabbitMQConfig;
import com.logitrack.courierservice.dto.ShipmentStatusChangedEvent;
import com.logitrack.courierservice.entity.CourierStatus;
import com.logitrack.courierservice.exception.ResourceNotFoundException;
import com.logitrack.courierservice.service.CourierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumidor asíncrono de eventos de RabbitMQ para actualizar la disponibilidad
 * del repartidor (AVAILABLE / BUSY) cuando cambia el estado de un envío en shipment-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentStatusListener {

    private final CourierService courierService;

    @RabbitListener(queues = RabbitMQConfig.COURIER_SHIPMENT_STATUS_QUEUE)
    public void handleShipmentStatusChanged(ShipmentStatusChangedEvent event) {
        log.info("Evento recibido en {}: eventId={}, shipmentId={}, trackingCode={}, courierId={}, status={} -> {}",
                RabbitMQConfig.COURIER_SHIPMENT_STATUS_QUEUE,
                event.eventId(), event.shipmentId(), event.trackingCode(), event.courierId(),
                event.previousStatus(), event.currentStatus());

        if (event.courierId() == null || event.courierId().isBlank()) {
            log.info("Evento descartado de forma segura: no posee repartidor asignado (courierId=null) para envío {}",
                    event.shipmentId());
            return;
        }

        UUID courierId;
        try {
            courierId = UUID.fromString(event.courierId());
        } catch (IllegalArgumentException ex) {
            log.warn("Identificador de repartidor inválido en evento: courierId={}", event.courierId());
            return;
        }

        String currentStatus = event.currentStatus();
        if (currentStatus == null) {
            return;
        }

        try {
            if ("EN_RUTA".equalsIgnoreCase(currentStatus)) {
                log.info("Actualizando repartidor {} a estado BUSY por despacho de envío {}", courierId, event.trackingCode());
                courierService.changeStatus(courierId, CourierStatus.BUSY);
            } else if ("ENTREGADO".equalsIgnoreCase(currentStatus) || "CANCELADO".equalsIgnoreCase(currentStatus)) {
                log.info("Liberando repartidor {} a estado AVAILABLE por finalización/cancelación de envío {}", courierId, event.trackingCode());
                courierService.changeStatus(courierId, CourierStatus.AVAILABLE);
            } else {
                log.debug("Estado de envío {} no requiere mutación de disponibilidad del repartidor {}", currentStatus, courierId);
            }
        } catch (ResourceNotFoundException ex) {
            log.warn("No se encontró el repartidor {} para actualizar disponibilidad ante evento de envío {}",
                    courierId, event.shipmentId());
        }
    }
}
