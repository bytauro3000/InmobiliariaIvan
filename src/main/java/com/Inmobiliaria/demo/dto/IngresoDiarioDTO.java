package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngresoDiarioDTO {

    /** Suma de todos los importe_pagado en pago_letra con fecha_pago = hoy */
    private BigDecimal totalPagoLetras;
    /** Suma de todos los importe_pagado en pago_mora con fecha_pago = hoy */
    private BigDecimal totalPagoMoras;
    /** Suma de todos los importe_pagado en pago_inicial con fecha_pago = hoy */
    private BigDecimal totalPagoIniciales;
    /**
     * Suma de monto_pagado de inscripciones_servicios (microservicio)
     * cuya fecha_inscripcion = hoy.
     */
    private BigDecimal totalInscripcionesServicios;

    /**
     * Gran total = pagoLetras + pagoMoras + pagoIniciales + inscripcionesServicios.
     * Se calcula en el service para evitar lógica en el frontend.
     */
    private BigDecimal totalGeneral;

    /** Cantidad de pagos de letras registrados hoy */
    private long cantidadPagoLetras;

    /** Cantidad de pagos de moras registrados hoy */
    private long cantidadPagoMoras;

    /** Cantidad de pagos de iniciales registrados hoy */
    private long cantidadPagoIniciales;

    /** Cantidad de inscripciones de servicios básicos registradas hoy */
    private long cantidadInscripcionesServicios;
}