package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Resumen del portal del cliente: agregado global + desglose por contrato.
 * Los totales globales se separan por moneda (un cliente podría tener
 * contratos en USD y PEN; sumarlos sería incorrecto).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortalResumenDTO {

    private String nombreCliente;

    private List<PortalContratoDTO> contratos = new ArrayList<>();

    // Totales globales por moneda
    private BigDecimal deudaTotalUsd = BigDecimal.ZERO;
    private BigDecimal deudaTotalPen = BigDecimal.ZERO;
    private BigDecimal deudaInmediataUsd = BigDecimal.ZERO;
    private BigDecimal deudaInmediataPen = BigDecimal.ZERO;
    private BigDecimal moraInmediataUsd = BigDecimal.ZERO;
    private BigDecimal moraInmediataPen = BigDecimal.ZERO;

    private Integer letrasPagadas = 0;
    private Integer letrasVencidas = 0;
    private Integer letrasProximas = 0;
}
