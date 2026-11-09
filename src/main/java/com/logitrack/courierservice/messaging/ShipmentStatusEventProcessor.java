package com.logitrack.courierservice.messaging;

import com.logitrack.courierservice.entity.Courier;
import com.logitrack.courierservice.entity.CourierAvailability;
import com.logitrack.courierservice.entity.ShipmentCourierProjection;
import com.logitrack.courierservice.repository.CourierRepository;
import com.logitrack.courierservice.repository.ProcessedShipmentEventRepository;
import com.logitrack.courierservice.repository.ShipmentCourierProjectionRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentStatusEventProcessor {
    private static final int TRACKING_CODE_MAX_LENGTH = 13;
    private static final Instant MYSQL_DATETIME_MIN = Instant.parse("1000-01-01T00:00:00Z");
    private static final Instant MYSQL_DATETIME_MAX =
            Instant.parse("9999-12-31T23:59:59.999999Z");

    private final ProcessedShipmentEventRepository processedEvents;
    private final ShipmentCourierProjectionRepository projections;
    private final CourierRepository couriers;

    /**
     * El claim de eventId, la proyección y Courier comparten una sola transacción.
     * El bloqueo pesimista de Courier serializa eventos de sus distintos envíos.
     */
    @Transactional
    public void process(ShipmentStatusChangedEvent event) {
        validate(event);
        UUID courierId = parseCanonicalUuid("courierId", event.courierId());

        if (processedEvents.claim(event.eventId().toString(), event.occurredAt()) == 0) {
            log.debug("Evento de envío duplicado ignorado eventId={}", event.eventId());
            return;
        }

        Courier courier = couriers.findByIdForUpdate(courierId)
                .orElseThrow(() -> new PermanentShipmentEventException(
                        "No existe el repartidor " + courierId));

        int initialized = projections.initialize(event.shipmentId(), event.courierId(),
                event.trackingCode(), event.currentStatus().name(), event.occurredAt());
        ShipmentCourierProjection projection = projections
                .findByShipmentIdForUpdate(event.shipmentId())
                .orElseThrow(() -> new IllegalStateException(
                        "No se pudo cargar la proyección del envío " + event.shipmentId()));

        validateIdentity(event, projection);
        if (initialized == 0 && !applyExistingTransition(event, projection)) {
            return;
        }

        if (event.currentStatus() == ShipmentStatus.EN_RUTA) {
            courier.changeAvailability(CourierAvailability.BUSY);
            return;
        }

        boolean anotherActiveShipment = projections
                .countByCourierIdAndStatusAndShipmentIdNot(event.courierId(),
                        ShipmentStatus.EN_RUTA, event.shipmentId()) > 0;
        courier.changeAvailability(anotherActiveShipment
                ? CourierAvailability.BUSY : CourierAvailability.AVAILABLE);
    }

    /** Valida el contrato sin acceder a persistencia; también se usa para el no-op sin courier. */
    public void validate(ShipmentStatusChangedEvent event) {
        validateContract(event);
        validateSupportedTransition(event);
    }

    private boolean applyExistingTransition(ShipmentStatusChangedEvent event,
            ShipmentCourierProjection projection) {
        ShipmentStatus stored = projection.getStatus();
        ShipmentStatus incoming = event.currentStatus();
        if (isTerminal(stored)) {
            if (incoming == ShipmentStatus.EN_RUTA) {
                log.info("Se ignora EN_RUTA tardío eventId={} shipmentId={}; prevalece {}",
                        event.eventId(), event.shipmentId(), stored);
                return false;
            }
            if (incoming != stored) {
                throw new PermanentShipmentEventException(
                        "Terminal incompatible para envío " + event.shipmentId()
                                + ": persistido=" + stored + ", recibido=" + incoming);
            }
            return false;
        }
        if (incoming == ShipmentStatus.EN_RUTA) {
            return false;
        }
        if (event.previousStatus() != ShipmentStatus.EN_RUTA) {
            throw new PermanentShipmentEventException(
                    "Transición terminal contradictoria para envío " + event.shipmentId());
        }
        projection.transitionTo(incoming, event.occurredAt());
        return true;
    }

    private void validateIdentity(ShipmentStatusChangedEvent event,
            ShipmentCourierProjection projection) {
        if (!projection.getCourierId().equals(event.courierId())
                || !projection.getTrackingCode().equals(event.trackingCode())) {
            throw new PermanentShipmentEventException(
                    "Identidad contradictoria para envío " + event.shipmentId());
        }
    }

    private void validateSupportedTransition(ShipmentStatusChangedEvent event) {
        ShipmentStatus current = event.currentStatus();
        if (current != ShipmentStatus.EN_RUTA && !isTerminal(current)) {
            throw new PermanentShipmentEventException(
                    "Estado no soportado: " + current);
        }
        if (current == ShipmentStatus.EN_RUTA
                && event.previousStatus() != ShipmentStatus.PENDIENTE) {
            throw new PermanentShipmentEventException(
                    "EN_RUTA solo puede seguir a PENDIENTE");
        }
        if (current == ShipmentStatus.ENTREGADO
                && event.previousStatus() != ShipmentStatus.EN_RUTA) {
            throw new PermanentShipmentEventException(
                    "ENTREGADO solo puede seguir a EN_RUTA");
        }
        if (current == ShipmentStatus.CANCELADO
                && event.previousStatus() != ShipmentStatus.PENDIENTE) {
            throw new PermanentShipmentEventException(
                    "CANCELADO solo puede seguir a PENDIENTE");
        }
    }

    private void validateContract(ShipmentStatusChangedEvent event) {
        if (event == null || event.eventId() == null || event.previousStatus() == null
                || event.currentStatus() == null || event.occurredAt() == null) {
            throw new PermanentShipmentEventException("Evento de envío incompleto");
        }
        parseCanonicalUuid("shipmentId", event.shipmentId());
        if (event.courierId() != null) {
            parseCanonicalUuid("courierId", event.courierId());
        }
        if (event.trackingCode() == null || event.trackingCode().isBlank()
                || event.trackingCode().length() > TRACKING_CODE_MAX_LENGTH) {
            throw new PermanentShipmentEventException(
                    "trackingCode debe contener entre 1 y " + TRACKING_CODE_MAX_LENGTH
                            + " caracteres");
        }
        if (event.occurredAt().isBefore(MYSQL_DATETIME_MIN)
                || event.occurredAt().isAfter(MYSQL_DATETIME_MAX)) {
            throw new PermanentShipmentEventException(
                    "occurredAt está fuera del rango DATETIME(6) persistible");
        }
    }

    private UUID parseCanonicalUuid(String field, String value) {
        if (value == null) {
            throw new PermanentShipmentEventException(field + " no puede ser nulo");
        }
        try {
            UUID parsed = UUID.fromString(value);
            if (!parsed.toString().equals(value)) {
                throw new IllegalArgumentException("formato no canónico");
            }
            return parsed;
        } catch (IllegalArgumentException exception) {
            throw new PermanentShipmentEventException(
                    field + " debe ser un UUID canónico: " + value, exception);
        }
    }

    private boolean isTerminal(ShipmentStatus status) {
        return status == ShipmentStatus.ENTREGADO || status == ShipmentStatus.CANCELADO;
    }
}
