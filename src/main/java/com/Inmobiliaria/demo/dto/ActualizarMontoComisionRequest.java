package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Actualización del monto total de comisión acordado (negociado por el gerente).
 * Solo aplica mientras no haya pagos registrados. No puede superar el 3% del
 * monto total del contrato.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarMontoComisionRequest {
    private Integer idComision;
    private BigDecimal monto;
}