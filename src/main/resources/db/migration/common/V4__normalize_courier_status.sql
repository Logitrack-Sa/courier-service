UPDATE couriers SET availability = 'BUSY' WHERE status = 'BUSY';
UPDATE couriers SET status = 'ACTIVO' WHERE status IN ('AVAILABLE', 'BUSY');
UPDATE couriers SET status = 'INACTIVO' WHERE status = 'INACTIVE';
