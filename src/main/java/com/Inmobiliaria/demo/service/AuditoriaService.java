package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.enums.AccionAuditoria;

/**
 * Fachada de auditoría. Implementaciones NUNCA deben lanzar excepción:
 * un fallo de auditoría no puede tumbar un pago.
 */
public interface AuditoriaService {

    void registrar(AccionAuditoria accion, String entidad, Integer idEntidad, String descripcion);

    /** Atajo cuando no hay id de entidad (ej: LOGIN/LOGOUT). */
    default void registrar(AccionAuditoria accion, String entidad, String descripcion) {
        registrar(accion, entidad, null, descripcion);
    }
}
