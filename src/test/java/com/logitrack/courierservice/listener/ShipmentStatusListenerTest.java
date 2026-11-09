package com.logitrack.courierservice.listener;

import com.logitrack.courierservice.dto.ShipmentStatusChangedEvent;
import com.logitrack.courierservice.entity.CourierStatus;
import com.logitrack.courierservice.exception.ResourceNotFoundException;
import com.logitrack.courierservice.service.CourierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentStatusListenerTest {

    @Mock
    private CourierService courierService;

    private ShipmentStatusListener listener;

    private final UUID courierId = UUID.randomUUID();
    private final UUID shipmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listener = new ShipmentStatusListener(courierService);
    }

    private ShipmentStatusChangedEvent createEvent(String currentStatus, String courierIdStr) {
        return new ShipmentStatusChangedEvent(
                UUID.randomUUID(),
                shipmentId.toString(),
                "LT-TEST12345",
                courierIdStr,
                "PENDIENTE",
                currentStatus,
                Instant.now()
        );
    }

    @Test
    @DisplayName("Cuando currentStatus es EN_RUTA, actualiza el repartidor a estado BUSY")
    void whenCurrentStatusIsEnRuta_updatesCourierToBusy() {
        var event = createEvent("EN_RUTA", courierId.toString());

        listener.handleShipmentStatusChanged(event);

        verify(courierService, times(1)).changeStatus(courierId, CourierStatus.BUSY);
    }

    @Test
    @DisplayName("Cuando currentStatus es ENTREGADO, libera al repartidor a estado AVAILABLE")
    void whenCurrentStatusIsEntregado_updatesCourierToAvailable() {
        var event = createEvent("ENTREGADO", courierId.toString());

        listener.handleShipmentStatusChanged(event);

        verify(courierService, times(1)).changeStatus(courierId, CourierStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Cuando currentStatus es CANCELADO, libera al repartidor a estado AVAILABLE")
    void whenCurrentStatusIsCancelado_updatesCourierToAvailable() {
        var event = createEvent("CANCELADO", courierId.toString());

        listener.handleShipmentStatusChanged(event);

        verify(courierService, times(1)).changeStatus(courierId, CourierStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Cuando courierId es null, descarta el evento de forma segura sin interacciones")
    void whenCourierIdIsNull_safelyIgnored() {
        var event = createEvent("CANCELADO", null);

        assertThatCode(() -> listener.handleShipmentStatusChanged(event)).doesNotThrowAnyException();

        verifyNoInteractions(courierService);
    }

    @Test
    @DisplayName("Cuando courierId es cadena vacía o espacios, se descarta de forma segura")
    void whenCourierIdIsBlank_safelyIgnored() {
        var event = createEvent("EN_RUTA", "   ");

        assertThatCode(() -> listener.handleShipmentStatusChanged(event)).doesNotThrowAnyException();

        verifyNoInteractions(courierService);
    }

    @Test
    @DisplayName("Cuando courierId no tiene formato UUID válido, se maneja de forma segura")
    void whenCourierIdIsInvalidUuid_safelyIgnored() {
        var event = createEvent("EN_RUTA", "invalido-no-uuid");

        assertThatCode(() -> listener.handleShipmentStatusChanged(event)).doesNotThrowAnyException();

        verifyNoInteractions(courierService);
    }

    @Test
    @DisplayName("Cuando el repartidor no existe en BD, captura ResourceNotFoundException sin propagar fallo")
    void whenCourierNotFound_safelyHandled() {
        var event = createEvent("EN_RUTA", courierId.toString());
        doThrow(new ResourceNotFoundException("Repartidor", courierId))
                .when(courierService).changeStatus(courierId, CourierStatus.BUSY);

        assertThatCode(() -> listener.handleShipmentStatusChanged(event)).doesNotThrowAnyException();

        verify(courierService).changeStatus(courierId, CourierStatus.BUSY);
    }

    @Test
    @DisplayName("Cuando currentStatus no requiere mutación (ej. PENDIENTE), no altera al repartidor")
    void whenCurrentStatusIsPendiente_noStatusChange() {
        var event = createEvent("PENDIENTE", courierId.toString());

        listener.handleShipmentStatusChanged(event);

        verifyNoInteractions(courierService);
    }
}
