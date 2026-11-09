package com.logitrack.courierservice.messaging;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;

/** Error de contrato o negocio que debe rechazarse hacia la DLQ. */
public class PermanentShipmentEventException extends AmqpRejectAndDontRequeueException {
    public PermanentShipmentEventException(String message) {
        super(message);
    }

    public PermanentShipmentEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
