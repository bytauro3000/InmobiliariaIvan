-- ============================================================
-- Vouchers: fecha de operación para mora, inscripción y pago inicial
-- Consistente con pago_letra (columna fecha_operacion datetime).
--
-- IMPORTANTE: ejecutar en AMBAS BDs de producción (IVAN y MERRUIC)
-- ANTES de desplegar el backend que agrega los campos a las entidades.
-- ============================================================

ALTER TABLE pago_mora                    ADD COLUMN fecha_operacion datetime NULL;
ALTER TABLE pago_inicial                 ADD COLUMN fecha_operacion datetime NULL;
ALTER TABLE pago_inscripcion_comprobante ADD COLUMN fecha_operacion datetime NULL;

-- Verificación (debe devolver 3 filas con datetime):
SELECT table_name, column_name, data_type
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND column_name = 'fecha_operacion'
  AND table_name IN ('pago_mora', 'pago_inicial', 'pago_inscripcion_comprobante')
ORDER BY table_name;
