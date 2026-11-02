-- Equivalente de la migracion MySQL V2 para las pruebas aisladas con H2.
ALTER TABLE couriers ADD COLUMN dni VARCHAR(20) NOT NULL;
ALTER TABLE couriers ADD CONSTRAINT uk_couriers_dni UNIQUE (dni);
