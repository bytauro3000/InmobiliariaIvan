-- Pagos de comisión multi-lote con muchos contratos (ej: 31 contratos / 68 letras):
--   1) observacion lista todos los contratos y superaba 255 chars → Data truncation (HTTP 500).
--   2) concepto del EG01 (tabla por letra) superaba 2000 chars y se truncaba → recibo a medias.
-- Ejecutar UNA vez en cada base de producción (Hetzner y Render) antes del deploy.
-- Nota: spring.jpa.hibernate.ddl-auto=update NO amplía columnas VARCHAR existentes en MySQL.

ALTER TABLE pago_comision_vendedor MODIFY COLUMN observacion VARCHAR(2000);
ALTER TABLE recibo_egreso MODIFY COLUMN concepto VARCHAR(8000) NOT NULL;
