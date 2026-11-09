package com.logitrack.courierservice.messaging;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "logitrack.messaging.shipment-status.enabled",
        havingValue = "true", matchIfMissing = true)
public class ShipmentStatusEventListener {

    private final ShipmentStatusEventProcessor processor;
    private final Validator validator;

    @RabbitListener(queues = RabbitMQConfiguration.SHIPMENT_STATUS_QUEUE)
    public void consume(ShipmentStatusChangedEvent event) {
        Set<ConstraintViolation<ShipmentStatusChangedEvent>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            throw new PermanentShipmentEventException("Evento de envío inválido: " + violations);
        }
        processor.validate(event);
        if (event.courierId() == null) {
            log.info("Evento {} del envío {} no tiene courierId; se confirma sin efectos",
                    event.eventId(), event.shipmentId());
            return;
        }
        processor.process(event);
    }
}
