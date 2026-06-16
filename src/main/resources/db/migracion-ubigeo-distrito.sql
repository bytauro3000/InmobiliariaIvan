-- ==========================================================
-- MIGRACIÓN: Agregar ubigeo, provincia y departamento a distrito
-- ==========================================================
-- Ejecutar en la base de datos db_inmobiliaria
-- No altera IDs existentes

ALTER TABLE distrito
  ADD COLUMN codigo_ubigeo VARCHAR(6) DEFAULT NULL AFTER nombre,
  ADD COLUMN provincia VARCHAR(100) DEFAULT NULL AFTER codigo_ubigeo,
  ADD COLUMN departamento VARCHAR(100) DEFAULT NULL AFTER provincia;

-- Actualizar distritos existentes sin cambiar sus IDs
UPDATE distrito SET codigo_ubigeo = '150132', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 1;
UPDATE distrito SET codigo_ubigeo = '150103', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 2;
UPDATE distrito SET codigo_ubigeo = '150110', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 3;
UPDATE distrito SET codigo_ubigeo = '150135', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 4;
UPDATE distrito SET codigo_ubigeo = '150142', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 5;
UPDATE distrito SET codigo_ubigeo = '150143', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 6;
UPDATE distrito SET codigo_ubigeo = '150125', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 7;
UPDATE distrito SET codigo_ubigeo = '150117', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 8;
UPDATE distrito SET codigo_ubigeo = '150133', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 9;
UPDATE distrito SET codigo_ubigeo = '150108', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 10;
UPDATE distrito SET codigo_ubigeo = '150114', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 11;
UPDATE distrito SET codigo_ubigeo = '150122', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 12;
UPDATE distrito SET codigo_ubigeo = '150104', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 13;
UPDATE distrito SET codigo_ubigeo = '150105', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 14;
UPDATE distrito SET codigo_ubigeo = '150113', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 15;
UPDATE distrito SET codigo_ubigeo = '150136', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 16;
UPDATE distrito SET codigo_ubigeo = '150121', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 17;
UPDATE distrito SET codigo_ubigeo = '150120', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 18;
UPDATE distrito SET codigo_ubigeo = '150140', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 19;
UPDATE distrito SET codigo_ubigeo = '150141', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 20;
UPDATE distrito SET codigo_ubigeo = '150116', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 21;
UPDATE distrito SET codigo_ubigeo = '150130', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 22;
UPDATE distrito SET codigo_ubigeo = '150134', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 23;
UPDATE distrito SET codigo_ubigeo = '150137', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 24;
UPDATE distrito SET codigo_ubigeo = '150111', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 25;
UPDATE distrito SET codigo_ubigeo = '150128', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 26;
UPDATE distrito SET codigo_ubigeo = '150112', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 27;
UPDATE distrito SET codigo_ubigeo = '150106', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 28;
UPDATE distrito SET codigo_ubigeo = '150118', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 29;
UPDATE distrito SET codigo_ubigeo = '150139', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 30;
UPDATE distrito SET codigo_ubigeo = '150102', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 31;
UPDATE distrito SET codigo_ubigeo = '150115', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 32;
UPDATE distrito SET codigo_ubigeo = '150131', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 33;
UPDATE distrito SET codigo_ubigeo = '150119', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 41;
UPDATE distrito SET codigo_ubigeo = '150123', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 42;
UPDATE distrito SET codigo_ubigeo = '150124', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 43;
UPDATE distrito SET codigo_ubigeo = '150101', provincia = 'LIMA', departamento = 'LIMA' WHERE id_distrito = 30044;
-- Callao
UPDATE distrito SET codigo_ubigeo = '070101', provincia = 'CALLAO', departamento = 'CALLAO' WHERE id_distrito = 34;
UPDATE distrito SET codigo_ubigeo = '070102', provincia = 'CALLAO', departamento = 'CALLAO' WHERE id_distrito = 35;
UPDATE distrito SET codigo_ubigeo = '070103', provincia = 'CALLAO', departamento = 'CALLAO' WHERE id_distrito = 38;
UPDATE distrito SET codigo_ubigeo = '070104', provincia = 'CALLAO', departamento = 'CALLAO' WHERE id_distrito = 36;
UPDATE distrito SET codigo_ubigeo = '070105', provincia = 'CALLAO', departamento = 'CALLAO' WHERE id_distrito = 37;
UPDATE distrito SET codigo_ubigeo = '070106', provincia = 'CALLAO', departamento = 'CALLAO' WHERE id_distrito = 40;
UPDATE distrito SET codigo_ubigeo = '070107', provincia = 'CALLAO', departamento = 'CALLAO' WHERE id_distrito = 39;
