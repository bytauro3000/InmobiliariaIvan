package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.PortalContratoDTO;
import com.Inmobiliaria.demo.dto.PortalDatosDTO;
import com.Inmobiliaria.demo.dto.PortalResumenDTO;

import java.util.List;

/**
 * Datos del PORTAL DEL CLIENTE (solo lectura).
 * La identidad SIEMPRE sale del token JWT (email que inyecta el gateway);
 * el cliente nunca envía ids por parámetro.
 */
public interface PortalClienteService {

    /** Datos personales enmascarados del cliente autenticado. */
    PortalDatosDTO obtenerMisDatos();

    /** Resumen global + deudas por contrato (sin letra a letra). */
    PortalResumenDTO obtenerResumen();

    /** Contratos del cliente con su letra a letra (estado efectivo + mora en vivo). */
    List<PortalContratoDTO> obtenerLetras();
}
