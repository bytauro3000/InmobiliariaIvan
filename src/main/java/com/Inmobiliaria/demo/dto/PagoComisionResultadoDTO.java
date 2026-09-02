package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resultado del registro de pagos de comisión: recibo(s) de egresos generados
 * y el estado actualizado de la comisión.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoComisionResultadoDTO {
    private List<String> numerosEgreso;
    private Integer idComision;
    private BigDecimal saldoPendiente;
    private String estado;
    private LocalDate fechaPago;
}