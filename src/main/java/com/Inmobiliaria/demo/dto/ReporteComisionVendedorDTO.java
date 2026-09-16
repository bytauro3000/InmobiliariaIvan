package com.Inmobiliaria.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteComisionVendedorDTO {

    // ── Datos del vendedor ──────────────────────────────────────────
    private Integer idVendedor;
    private String nombreVendedor;
    private String apellidosVendedor;
    private String dniVendedor;
    private String celularVendedor;
    private String direccionVendedor;
    private String distritoVendedor;
    private BigDecimal porcentajeComision;

    // ── Resumen de comisiones ───────────────────────────────────────
    private BigDecimal totalComision;      // montoComisionTotal de todas las comisiones
    private BigDecimal aporteComision;     // suma de pagos realizados (adelanto + mensuales)
    private BigDecimal saldoPendiente;     // total - aporte
    private String moneda;                 // moneda de las comisiones

    // ── Fecha de emisión ────────────────────────────────────────────
    private LocalDateTime fechaEmision;

    // ── Detalle por programa ────────────────────────────────────────
    private List<ProgramaComision> programas;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgramaComision {
        private String nombrePrograma;
        private List<FilaComision> filas;
        private BigDecimal totalPrograma;
        private int totalLotes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilaComision {
        private int numero;
        private String manzana;
        private String numeroLote;
        private LocalDate fechaContrato;
        private BigDecimal montoComision;
        private BigDecimal pagosRealizados;  // adelanto + mensuales de esta comision
        private BigDecimal saldoComision;
        private String moneda;
    }
}
