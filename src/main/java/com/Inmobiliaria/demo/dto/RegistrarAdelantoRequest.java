package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Registro del adelanto de comisión al vendedor. El monto es editable por la
 * secretaría (valor sugerido: 30% de la inicial o adelanto del programa).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarAdelantoRequest {
    private Integer idComision;
    private BigDecimal monto;
    private String observacion;
}