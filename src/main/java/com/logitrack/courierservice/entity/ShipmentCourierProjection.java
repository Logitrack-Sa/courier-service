package com.logitrack.courierservice.entity;

import com.logitrack.courierservice.messaging.ShipmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "shipment_courier_projection")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShipmentCourierProjection {
    @Id
    @Column(name = "shipment_id", length = 36, updatable = false)
    private String shipmentId;

    @Column(name = "courier_id", nullable = false, length = 36, updatable = false)
    private String courierId;

    @Column(name = "tracking_code", nullable = false, length = 13, updatable = false)
    private String trackingCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShipmentStatus status;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Version
    private Long version;

    public void transitionTo(ShipmentStatus newStatus, Instant newOccurredAt) {
        status = newStatus;
        occurredAt = newOccurredAt;
    }
}
