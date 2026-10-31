package com.logitrack.courierservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "couriers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Courier extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String dni;

    @Column(name = "vehicle_type", nullable = false, length = 50)
    private String vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourierStatus status = CourierStatus.ACTIVO;

    public Courier(String name, String dni, String vehicleType) {
        this.name = name;
        this.dni = dni;
        this.vehicleType = vehicleType;
    }

    public void changeStatus(CourierStatus newStatus) {
        this.status = Objects.requireNonNull(newStatus, "El estado no puede ser nulo");
    }

    public void updateInfo(String name, String dni, String vehicleType) {
        this.name = Objects.requireNonNull(name, "El nombre no puede ser nulo");
        this.dni = Objects.requireNonNull(dni, "El DNI no puede ser nulo");
        this.vehicleType = Objects.requireNonNull(vehicleType, "El tipo de vehículo no puede ser nulo");
    }

    public boolean isAvailable() {
        return status == CourierStatus.ACTIVO;
    }
}
