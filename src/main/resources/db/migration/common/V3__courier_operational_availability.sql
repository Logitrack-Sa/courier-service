-- Separa habilitación administrativa (status) de disponibilidad operativa.
-- Los registros existentes comienzan AVAILABLE. Los envíos ya EN_RUTA deben reconciliarse
-- operativamente porque no existe garantía de replay automático de eventos históricos.
ALTER TABLE couriers
    ADD COLUMN availability VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE';

CREATE TABLE processed_shipment_events (
    event_id VARCHAR(36) NOT NULL,
    processed_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_processed_shipment_events PRIMARY KEY (event_id)
);

CREATE TABLE shipment_courier_projection (
    shipment_id VARCHAR(36) NOT NULL,
    courier_id VARCHAR(36) NOT NULL,
    tracking_code VARCHAR(13) NOT NULL,
    status VARCHAR(20) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT pk_shipment_courier_projection PRIMARY KEY (shipment_id)
);

CREATE INDEX idx_shipment_projection_courier_status
    ON shipment_courier_projection (courier_id, status);
