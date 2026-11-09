package com.logitrack.courierservice.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.logitrack.courierservice.entity.Courier;
import com.logitrack.courierservice.entity.CourierAvailability;
import com.logitrack.courierservice.entity.CourierStatus;
import com.logitrack.courierservice.entity.ShipmentCourierProjection;
import com.logitrack.courierservice.mapper.CourierMapper;
import com.logitrack.courierservice.repository.CourierRepository;
import com.logitrack.courierservice.repository.ProcessedShipmentEventRepository;
import com.logitrack.courierservice.repository.ShipmentCourierProjectionRepository;
import com.logitrack.courierservice.service.CourierStatusQueryService;
import com.logitrack.courierservice.service.implementation.CourierServiceImp;
import jakarta.validation.Validation;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.dao.TransientDataAccessResourceException;

class ShipmentStatusConsumerUnitTest {
    private static final UUID EVENT_ID = UUID.fromString("b7d2f9b8-18e3-4c92-b4ea-12051db52971");
    private static final UUID COURIER_ID = UUID.fromString("7f9a1c2e-3b4d-4e5f-8a9b-0c1d2e3f4a5b");
    private static final String SHIPMENT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final Instant OCCURRED_AT = Instant.parse("2026-11-05T22:00:00Z");

    private ProcessedShipmentEventRepository processed;
    private ShipmentCourierProjectionRepository projections;
    private CourierRepository couriers;
    private ShipmentStatusEventProcessor processor;
    private Courier courier;

    @BeforeEach
    void setUp() {
        processed = mock(ProcessedShipmentEventRepository.class);
        projections = mock(ShipmentCourierProjectionRepository.class);
        couriers = mock(CourierRepository.class);
        processor = new ShipmentStatusEventProcessor(processed, projections, couriers);
        courier = new Courier("Ana", "38456789", "MOTO");
    }

    @Test
    void decodesRealProducerJsonWithoutTypeHeaderIncludingUuidInstantAndNullCourier() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        String json = """
                {"eventId":"b7d2f9b8-18e3-4c92-b4ea-12051db52971",
                 "shipmentId":"550e8400-e29b-41d4-a716-446655440000",
                 "trackingCode":"LT-A1B2C3D4E5","courierId":null,
                 "previousStatus":"PENDIENTE","currentStatus":"CANCELADO",
                 "occurredAt":"2026-11-05T22:00:00Z"}
                """;
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setInferredArgumentType(ShipmentStatusChangedEvent.class);
        Message message = new Message(json.getBytes(StandardCharsets.UTF_8), properties);

        Object decoded = converter.fromMessage(message);

        assertThat(decoded).isEqualTo(new ShipmentStatusChangedEvent(EVENT_ID, SHIPMENT_ID,
                "LT-A1B2C3D4E5", null, ShipmentStatus.PENDIENTE,
                ShipmentStatus.CANCELADO, OCCURRED_AT));
    }

    @Test
    void nullCourierAcknowledgesWithoutRepositoryAccess() {
        ShipmentStatusEventListener listener = new ShipmentStatusEventListener(processor,
                Validation.buildDefaultValidatorFactory().getValidator());

        listener.consume(event(null, ShipmentStatus.PENDIENTE, ShipmentStatus.CANCELADO));

        verify(processed, never()).claim(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(couriers, never()).findByIdForUpdate(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void nullCourierStillRequiresAValidEventContract() {
        ShipmentStatusEventListener listener = new ShipmentStatusEventListener(processor,
                Validation.buildDefaultValidatorFactory().getValidator());
        ShipmentStatusChangedEvent invalid = customEvent("1-1-1-1-1", null,
                "LT-A1B2C3D4E5", OCCURRED_AT);

        assertThatThrownBy(() -> listener.consume(invalid))
                .isInstanceOf(PermanentShipmentEventException.class);

        verify(processed, never()).claim(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void enRutaMarksCourierBusy() {
        arrangeNew(ShipmentStatus.EN_RUTA);

        processor.process(event(COURIER_ID.toString(), ShipmentStatus.PENDIENTE,
                ShipmentStatus.EN_RUTA));

        assertThat(courier.getAvailability()).isEqualTo(CourierAvailability.BUSY);
    }

    @Test
    void terminalMakesCourierAvailableWhenNoOtherActiveShipment() {
        courier.changeAvailability(CourierAvailability.BUSY);
        ShipmentCourierProjection projection = arrangeExisting(ShipmentStatus.EN_RUTA);
        when(projections.countByCourierIdAndStatusAndShipmentIdNot(COURIER_ID.toString(),
                ShipmentStatus.EN_RUTA, SHIPMENT_ID)).thenReturn(0L);

        processor.process(event(COURIER_ID.toString(), ShipmentStatus.EN_RUTA,
                ShipmentStatus.ENTREGADO));

        verify(projection).transitionTo(ShipmentStatus.ENTREGADO, OCCURRED_AT);
        assertThat(courier.getAvailability()).isEqualTo(CourierAvailability.AVAILABLE);
    }

    @Test
    void cancelledShipmentMakesCourierAvailableWhenNoOtherActiveShipment() {
        courier.changeAvailability(CourierAvailability.BUSY);
        arrangeNew(ShipmentStatus.CANCELADO);
        when(projections.countByCourierIdAndStatusAndShipmentIdNot(COURIER_ID.toString(),
                ShipmentStatus.EN_RUTA, SHIPMENT_ID)).thenReturn(0L);

        processor.process(event(COURIER_ID.toString(), ShipmentStatus.PENDIENTE,
                ShipmentStatus.CANCELADO));

        assertThat(courier.getAvailability()).isEqualTo(CourierAvailability.AVAILABLE);
    }

    @Test
    void endingOneOfTwoShipmentsKeepsCourierBusy() {
        courier.changeAvailability(CourierAvailability.BUSY);
        arrangeNew(ShipmentStatus.CANCELADO);
        when(projections.countByCourierIdAndStatusAndShipmentIdNot(COURIER_ID.toString(),
                ShipmentStatus.EN_RUTA, SHIPMENT_ID)).thenReturn(1L);

        processor.process(event(COURIER_ID.toString(), ShipmentStatus.PENDIENTE,
                ShipmentStatus.CANCELADO));

        assertThat(courier.getAvailability()).isEqualTo(CourierAvailability.BUSY);
    }

    @Test
    void duplicateHasNoSecondEffect() {
        when(processed.claim(EVENT_ID.toString(), OCCURRED_AT)).thenReturn(0);

        processor.process(event(COURIER_ID.toString(), ShipmentStatus.PENDIENTE,
                ShipmentStatus.EN_RUTA));

        verify(couriers, never()).findByIdForUpdate(COURIER_ID);
        verify(projections, never()).initialize(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void lateEnRutaDoesNotReopenTerminalShipment() {
        arrangeExisting(ShipmentStatus.ENTREGADO);

        processor.process(event(COURIER_ID.toString(), ShipmentStatus.PENDIENTE,
                ShipmentStatus.EN_RUTA));

        assertThat(courier.getAvailability()).isEqualTo(CourierAvailability.AVAILABLE);
    }

    @Test
    void administrativeInactiveIsNeverReactivatedByAvailabilityEvent() {
        courier.changeStatus(CourierStatus.INACTIVO);
        arrangeNew(ShipmentStatus.EN_RUTA);

        processor.process(event(COURIER_ID.toString(), ShipmentStatus.PENDIENTE,
                ShipmentStatus.EN_RUTA));

        assertThat(courier.getStatus()).isEqualTo(CourierStatus.INACTIVO);
        assertThat(courier.isAvailable()).isFalse();
    }

    @Test
    void missingCourierIsPermanentFailure() {
        when(processed.claim(EVENT_ID.toString(), OCCURRED_AT)).thenReturn(1);
        when(couriers.findByIdForUpdate(COURIER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processor.process(event(COURIER_ID.toString(),
                ShipmentStatus.PENDIENTE, ShipmentStatus.EN_RUTA)))
                .isInstanceOf(PermanentShipmentEventException.class);
    }

    @Test
    void unknownEnumFailsConversionUsingListenerInference() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(mapper);
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setInferredArgumentType(ShipmentStatusChangedEvent.class);
        Message invalid = new Message("{\"currentStatus\":\"UNKNOWN\"}"
                .getBytes(StandardCharsets.UTF_8), properties);
        assertThatThrownBy(() -> converter.fromMessage(invalid))
                .isInstanceOf(MessageConversionException.class);
    }

    @Test
    void transientPersistenceFailurePropagates() {
        when(processed.claim(EVENT_ID.toString(), OCCURRED_AT))
                .thenThrow(new TransientDataAccessResourceException("db unavailable"));
        assertThatThrownBy(() -> processor.process(event(COURIER_ID.toString(),
                ShipmentStatus.PENDIENTE, ShipmentStatus.EN_RUTA)))
                .isInstanceOf(TransientDataAccessResourceException.class);
    }

    @Test
    void availabilityServiceProjectsBusyAndAvailableForFeignAndKeepsInternalStatus() {
        when(couriers.findById(COURIER_ID)).thenReturn(Optional.of(courier));
        CourierServiceImp service = new CourierServiceImp(couriers, new CourierMapper());

        courier.changeAvailability(CourierAvailability.BUSY);
        var busy = service.getAvailability(COURIER_ID);
        assertThat(busy.status()).isEqualTo(CourierStatus.INACTIVO);
        assertThat(busy.available()).isFalse();

        courier.changeAvailability(CourierAvailability.AVAILABLE);
        var available = service.getAvailability(COURIER_ID);
        assertThat(available.status()).isEqualTo(CourierStatus.ACTIVO);
        assertThat(available.available()).isTrue();

        var internal = new CourierStatusQueryService(couriers).findStatus(COURIER_ID);
        assertThat(internal.status()).isEqualTo(CourierStatus.ACTIVO);
        assertThat(courier.getStatus()).isEqualTo(CourierStatus.ACTIVO);
    }

    @Test
    void enRutaToCancelledIsRejectedBeforeAnyWrite() {
        ShipmentStatusChangedEvent invalid = event(COURIER_ID.toString(),
                ShipmentStatus.EN_RUTA, ShipmentStatus.CANCELADO);

        assertThatThrownBy(() -> processor.process(invalid))
                .isInstanceOf(PermanentShipmentEventException.class);

        verify(processed, never()).claim(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(couriers, never()).findByIdForUpdate(org.mockito.ArgumentMatchers.any());
        verify(projections, never()).initialize(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void malformedIdentifiersLengthsAndTimestampAreRejectedBeforeSql() {
        ShipmentStatusChangedEvent[] invalidEvents = {
                customEvent("1-1-1-1-1", COURIER_ID.toString(), "LT-A1", OCCURRED_AT),
                customEvent(SHIPMENT_ID, "1-1-1-1-1", "LT-A1", OCCURRED_AT),
                customEvent(SHIPMENT_ID, COURIER_ID.toString(), " ", OCCURRED_AT),
                customEvent(SHIPMENT_ID, COURIER_ID.toString(), "X".repeat(14), OCCURRED_AT),
                customEvent(SHIPMENT_ID, COURIER_ID.toString(), "LT-A1",
                        Instant.parse("0999-12-31T23:59:59Z")),
                customEvent(SHIPMENT_ID, COURIER_ID.toString(), "LT-A1",
                        Instant.parse("+10000-01-01T00:00:00Z"))
        };

        for (ShipmentStatusChangedEvent invalid : invalidEvents) {
            assertThatThrownBy(() -> processor.process(invalid))
                    .isInstanceOf(PermanentShipmentEventException.class);
        }

        verify(processed, never()).claim(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(couriers, never()).findByIdForUpdate(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void contradictoryIdentityIsPermanentAndDoesNotMutateAvailability() {
        ShipmentCourierProjection projection = arrangeExisting(ShipmentStatus.EN_RUTA);
        when(projection.getCourierId()).thenReturn(UUID.randomUUID().toString());

        assertThatThrownBy(() -> processor.process(event(COURIER_ID.toString(),
                ShipmentStatus.EN_RUTA, ShipmentStatus.ENTREGADO)))
                .isInstanceOf(PermanentShipmentEventException.class);

        assertThat(courier.getAvailability()).isEqualTo(CourierAvailability.AVAILABLE);
    }

    @Test
    void incompatibleTerminalIsPermanentAndDoesNotMutateAvailability() {
        arrangeExisting(ShipmentStatus.CANCELADO);

        assertThatThrownBy(() -> processor.process(event(COURIER_ID.toString(),
                ShipmentStatus.EN_RUTA, ShipmentStatus.ENTREGADO)))
                .isInstanceOf(PermanentShipmentEventException.class);

        assertThat(courier.getAvailability()).isEqualTo(CourierAvailability.AVAILABLE);
    }

    @Test
    void declaresExactDurableTopologyAndDeadLetterArguments() {
        RabbitMQConfiguration config = new RabbitMQConfiguration();
        var exchange = config.shipmentTopicExchange();
        var dlx = config.shipmentStatusDeadLetterExchange();
        var queue = config.shipmentStatusQueue();
        var dlq = config.shipmentStatusDeadLetterQueue();
        Binding binding = config.shipmentStatusBinding(queue, exchange);
        Binding dlqBinding = config.shipmentStatusDeadLetterBinding(dlq, dlx);

        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.isExclusive()).isFalse();
        assertThat(queue.isAutoDelete()).isFalse();
        assertThat(binding.getRoutingKey()).isEqualTo("shipment.status.changed");
        assertThat(queue.getArguments()).containsEntry("x-dead-letter-exchange",
                "courier.shipment-status.dlx").containsEntry("x-dead-letter-routing-key",
                "courier.shipment-status.failed");
        assertThat(dlq.isDurable()).isTrue();
        assertThat(dlqBinding.getRoutingKey()).isEqualTo("courier.shipment-status.failed");
    }

    private void arrangeNew(ShipmentStatus state) {
        when(processed.claim(EVENT_ID.toString(), OCCURRED_AT)).thenReturn(1);
        when(couriers.findByIdForUpdate(COURIER_ID)).thenReturn(Optional.of(courier));
        when(projections.initialize(SHIPMENT_ID, COURIER_ID.toString(), "LT-A1B2C3D4E5",
                state.name(), OCCURRED_AT)).thenReturn(1);
        ShipmentCourierProjection projection = projection(state);
        when(projections.findByShipmentIdForUpdate(SHIPMENT_ID))
                .thenReturn(Optional.of(projection));
    }

    private ShipmentCourierProjection arrangeExisting(ShipmentStatus state) {
        when(processed.claim(EVENT_ID.toString(), OCCURRED_AT)).thenReturn(1);
        when(couriers.findByIdForUpdate(COURIER_ID)).thenReturn(Optional.of(courier));
        when(projections.initialize(org.mockito.ArgumentMatchers.eq(SHIPMENT_ID),
                org.mockito.ArgumentMatchers.eq(COURIER_ID.toString()),
                org.mockito.ArgumentMatchers.eq("LT-A1B2C3D4E5"),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(OCCURRED_AT)))
                .thenReturn(0);
        ShipmentCourierProjection projection = projection(state);
        when(projections.findByShipmentIdForUpdate(SHIPMENT_ID))
                .thenReturn(Optional.of(projection));
        return projection;
    }

    private ShipmentCourierProjection projection(ShipmentStatus state) {
        ShipmentCourierProjection projection = mock(ShipmentCourierProjection.class);
        when(projection.getCourierId()).thenReturn(COURIER_ID.toString());
        when(projection.getTrackingCode()).thenReturn("LT-A1B2C3D4E5");
        when(projection.getStatus()).thenReturn(state);
        return projection;
    }

    private ShipmentStatusChangedEvent event(String courierId, ShipmentStatus previous,
            ShipmentStatus current) {
        return new ShipmentStatusChangedEvent(EVENT_ID, SHIPMENT_ID, "LT-A1B2C3D4E5",
                courierId, previous, current, OCCURRED_AT);
    }

    private ShipmentStatusChangedEvent customEvent(String shipmentId, String courierId,
            String trackingCode, Instant occurredAt) {
        return new ShipmentStatusChangedEvent(EVENT_ID, shipmentId, trackingCode, courierId,
                ShipmentStatus.PENDIENTE, ShipmentStatus.EN_RUTA, occurredAt);
    }
}
