package com.logitrack.courierservice.entity;

/** Disponibilidad de un repartidor para recibir asignaciones. */
public enum CourierStatus {
    AVAILABLE,
    BUSY,
    INACTIVE,
    ACTIVO,
    INACTIVO;

    public boolean isAvailable() {
        return this == AVAILABLE || this == ACTIVO;
    }
}
