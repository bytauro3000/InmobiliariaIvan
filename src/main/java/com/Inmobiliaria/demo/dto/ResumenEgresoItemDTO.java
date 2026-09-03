package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fila del reporte de egresos (recibo de egreso EG01).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenEgresoItemDTO {

    private String numeroEgreso;
    private String serie;
    private Integer numero;
    private LocalDate fechaEmision;
    private String concepto;
    private String beneficiario;
    private Integer idContrato;
    private BigDecimal monto;
    private String moneda;
    private String medioPago;
    private String numeroOperacion;
    private LocalDate fechaOperacion;
    private String usuarioRegistro;
}