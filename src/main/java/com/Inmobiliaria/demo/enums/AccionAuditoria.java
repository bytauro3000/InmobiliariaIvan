package com.Inmobiliaria.demo.enums;

/**
 * Acciones registradas en el módulo de auditoría.
 * Se persisten como String (EnumType.STRING) para que futuras altas de
 * enum no rompan filas históricas.
 */
public enum AccionAuditoria {
    LOGIN,
    LOGOUT,
    CREAR,
    ACTUALIZAR,
    ELIMINAR,
    ANULAR,
    PAGAR,
    ENVIAR_SUNAT,
    MIGRAR
}
