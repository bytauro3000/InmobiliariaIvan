package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resultado del registro de pago de comisión: recibo(s) de egresos generados,
 * estado de la(s) comisión(es) afectada(s) y los vouchers subidos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoComisionResponseDTO {

    private List<String> numerosEgreso;
    private List<Integer> idsComision;
    private BigDecimal saldoPendiente;
    private String estado;
    private LocalDate fechaPago;
    private List<String> urlsVoucher;
    /** Detalle por lote del egreso (concepto multi-línea). */
    private String conceptoDetalle;
}