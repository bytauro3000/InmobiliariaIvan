-- MIGRACIÓN: Convertir fechas de pago DATE → DATETIME (con hora)
--
-- EJECUTAR EN LAS 2 BASES DE DATOS (producción), ANTES de desplegar el backend:
--   1) Base de IVAN    (ver nombre exacto en DB_URL de la instancia Render de IVAN)
--   2) Base de MERRUIC (ver nombre exacto en DB_URL de la instancia Render de MERRUIC)
--
-- Selecciona la base en tu cliente MySQL (phpMyAdmin / Workbench / consola):
--     USE nombre_de_la_base;
-- y ejecuta este script completo. Repetir para la segunda base.
--
-- Notas:
--   * spring.jpa.hibernate.ddl-auto=update NO convierte DATE → DATETIME en MySQL.
--   * MySQL convierte los valores existentes a 00:00:00 automáticamente
--     (NO se necesita ningún UPDATE de datos).

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

-- ── VERIFICACIÓN (debe devolver 9 columnas con DATA_TYPE = datetime) ──
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND ((TABLE_NAME = 'pago_letra'   AND COLUMN_NAME IN ('fecha_pago', 'fecha_operacion'))
    OR (TABLE_NAME = 'pago_mora'    AND COLUMN_NAME = 'fecha_pago')
    OR (TABLE_NAME = 'pago_inicial' AND COLUMN_NAME = 'fecha_pago')
    OR (TABLE_NAME = 'pago_inscripcion_comprobante' AND COLUMN_NAME = 'fecha_pago')
    OR (TABLE_NAME = 'pago_comision_vendedor' AND COLUMN_NAME IN ('fecha_pago', 'fecha_operacion'))
    OR (TABLE_NAME = 'recibo_egreso' AND COLUMN_NAME = 'fecha_operacion'))
ORDER BY TABLE_NAME, COLUMN_NAME;
