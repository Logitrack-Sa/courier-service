-- V2__add_dni_to_couriers.sql
-- Agrega columna dni única a la tabla couriers

ALTER TABLE couriers
    ADD COLUMN dni VARCHAR(20) NOT NULL,
    ADD CONSTRAINT uk_couriers_dni UNIQUE (dni);
