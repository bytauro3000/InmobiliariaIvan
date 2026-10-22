package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Letra vista por el cliente en el portal (solo lectura).
 * "estado" es el estado EFECTIVO calculado con la fecha de hoy
 * (PENDIENTE/PARCIAL/PAGADA/VENCIDA/ANULADO); la mora se calcula en vivo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortalLetraDTO {

    private Integer idLetra;
    private Integer idContrato;
    private String  numeroLetra;
    private LocalDate fechaVencimiento;
    private BigDecimal importe;
    private BigDecimal saldoPendiente;
    private String  estado;          // PENDIENTE | PARCIAL | PAGADA | VENCIDA | ANULADO
    private Integer diasVencida;     // 0 si no está vencida
    private BigDecimal moraAcumulada;// 0 si no está vencida (o si no aplica)
}
