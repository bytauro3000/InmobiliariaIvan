package com.Inmobiliaria.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reporte de pagos ya efectuados a vendedores (adelantos y cuotas mensuales).
 * Filtros: vendedor (por nombre) y tipo de pago (ADELANTO / MENSUAL / TODOS).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportePagosVendedorDTO {

    // ── Filtros aplicados ────────────────────────────────────────────
    private Integer idVendedor;             // null = todos los vendedores
    private boolean todosVendedores;
    private String filtroTipo;              // ADELANTO | MENSUAL | TODOS

    // ── Datos del vendedor (si se filtró por uno) ────────────────────
    private String nombreVendedor;
    private String apellidosVendedor;
    private String dniVendedor;

    // ── Resumen ──────────────────────────────────────────────────────
    private int totalPagos;
    private BigDecimal totalAdelantos;
    private BigDecimal totalMensuales;
    private BigDecimal totalGeneral;
    private String moneda;

    // ── Fecha de emisión ─────────────────────────────────────────────
    private LocalDateTime fechaEmision;

    // ── Detalle ──────────────────────────────────────────────────────
    private List<FilaPago> pagos;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilaPago {
        private int numero;
        private Integer idVendedor;
        private String nombreVendedor;
        private String tipo;                // ADELANTO | MENSUAL
        private LocalDateTime fechaPago;
        private String numeroLetra;         // null para el adelanto
        private Integer idContrato;
        private String manzana;
        private String numeroLote;
        private String programa;
        private BigDecimal monto;
        private String medioPago;
        private String numeroOperacion;
        private String numeroEgreso;
        private String moneda;
    }
}
