package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resumen de egresos (recibos EG01) por rango de fechas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenEgresosRangoDTO {

    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private BigDecimal totalUsd;
    private BigDecimal totalPen;
    private BigDecimal totalGeneral;
    private long cantidadTotal;
    private List<ResumenEgresoItemDTO> detalle;
}