package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Resumen de mora pendiente para un contrato.
 * Se usa en la vista de detalle del contrato y en la pantalla de pago de letras.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoraResumenContratoDTO {

    private Integer idContrato;
    private long    cantidadMorasPendientes;
    private BigDecimal totalMoraPendiente;   // suma de todos los montoMoraTotal PENDIENTES
}