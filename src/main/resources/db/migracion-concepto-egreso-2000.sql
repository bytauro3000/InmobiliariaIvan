-- Amplía concepto del recibo de egreso para pagos multi-lote de comisiones.
-- El concepto del EG01 agrega una línea por letra seleccionada y superaba 500 chars.
-- Ejecutar UNA vez en la base de producción (MySQL).
-- Nota: spring.jpa.hibernate.ddl-auto=update NO amplía columnas VARCHAR existentes en MySQL.

ALTER TABLE recibo_egreso MODIFY COLUMN concepto VARCHAR(2000) NOT NULL;
