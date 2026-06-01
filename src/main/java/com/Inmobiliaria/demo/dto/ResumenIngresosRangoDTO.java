package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenIngresosRangoDTO {

    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private BigDecimal totalLetras;
    private long       cantidadLetras;
    private BigDecimal totalMoras;
    private long       cantidadMoras;
    private BigDecimal totalIniciales;
    private long       cantidadIniciales;
    private BigDecimal totalInscripcionesServicios;
    private long       cantidadInscripcionesServicios;
    private BigDecimal totalGeneral;
    private long       cantidadTotal;
    private List<ResumenIngresoItemDTO> detalle;
}