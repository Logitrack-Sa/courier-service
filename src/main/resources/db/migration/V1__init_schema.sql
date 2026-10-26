-- V1__init_schema.sql
-- Esquema inicial exclusivo para courier-service en MySQL

CREATE TABLE couriers (
                          id BINARY(16) NOT NULL,
                          name VARCHAR(120) NOT NULL,
                          vehicle_type VARCHAR(50) NOT NULL,
                          status VARCHAR(20) NOT NULL,

                          created_at DATETIME(6) NOT NULL,
                          updated_at DATETIME(6) NOT NULL,
                          created_by VARCHAR(255) NOT NULL,
                          last_modified_by VARCHAR(255) NOT NULL,
                          version BIGINT NOT NULL,

                          CONSTRAINT pk_couriers PRIMARY KEY (id)
);