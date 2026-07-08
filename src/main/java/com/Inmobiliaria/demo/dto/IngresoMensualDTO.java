package com.Inmobiliaria.demo.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngresoMensualDTO {
    private int mes;
    private int anio;
    private String etiqueta;
    private BigDecimal totalPagoLetras;
    private BigDecimal totalPagoMoras;
    private BigDecimal totalPagoIniciales;
    private BigDecimal totalInscripcionesServicios;
    private BigDecimal totalGeneral;

    private BigDecimal totalBoleta;
    private BigDecimal totalRecibo;
    private BigDecimal totalEfectivo;
    private BigDecimal totalBancario;
}
