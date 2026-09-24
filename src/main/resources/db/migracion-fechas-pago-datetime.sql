-- MIGRACIÓN: Convertir fechas de pago DATE → DATETIME (con hora)
-- Ejecutar UNA vez en la base de producción ANTES de desplegar el backend.
-- Nota: spring.jpa.hibernate.ddl-auto=update NO convierte DATE → DATETIME en MySQL.
-- MySQL convierte automáticamente los valores existentes a 00:00:00 (sin necesidad de UPDATE).

USE db_inmobiliaria;

-- ── Pagos de letras ──
ALTER TABLE pago_letra MODIFY COLUMN fecha_pago DATETIME NOT NULL;
ALTER TABLE pago_letra MODIFY COLUMN fecha_operacion DATETIME NULL;

-- ── Pagos de moras ──
ALTER TABLE pago_mora MODIFY COLUMN fecha_pago DATETIME NOT NULL;

-- ── Pagos iniciales ──
ALTER TABLE pago_inicial MODIFY COLUMN fecha_pago DATETIME NOT NULL;

-- ── Abonos de inscripción (comprobante local en el monolito) ──
ALTER TABLE pago_inscripcion_comprobante MODIFY COLUMN fecha_pago DATETIME NOT NULL;

-- ── Pagos de comisiones a vendedores ──
ALTER TABLE pago_comision_vendedor MODIFY COLUMN fecha_pago DATETIME NOT NULL;
ALTER TABLE pago_comision_vendedor MODIFY COLUMN fecha_operacion DATETIME NULL;

-- ── Recibos de egreso (fecha del voucher bancario) ──
ALTER TABLE recibo_egreso MODIFY COLUMN fecha_operacion DATETIME NULL;
