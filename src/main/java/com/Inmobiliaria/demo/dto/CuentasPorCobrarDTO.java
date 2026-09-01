package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Cuentas por cobrar: ingresos futuros esperados por las letras pendientes
 * de pago (PENDIENTE/PARCIAL/VENCIDO, no PAGADO ni ANULADO) de los contratos
 * financiados ACTIVO/MORA. Agrupado por programa, desglosado en USD y PEN.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CuentasPorCobrarDTO {

    private BigDecimal totalUsd;
    private BigDecimal totalPen;
    private List<GrupoPrograma> programas;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrupoPrograma {
        private String nombrePrograma;
        private BigDecimal totalUsd;
        private BigDecimal totalPen;
        private List<FilaCuenta> contratos;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilaCuenta {
        private Integer idContrato;
        private String nombreCliente;
        private String manzana;
        private String numeroLote;
        private String nombrePrograma;
        private String moneda;
        private int cantidadLetras;
        private BigDecimal montoPorCobrar;
        private BigDecimal montoPagado;
        private LocalDate proximaVencimiento;
    }
}