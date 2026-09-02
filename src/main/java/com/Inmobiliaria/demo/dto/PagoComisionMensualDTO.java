package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pago mensual de comisión habilitado: 10% del importe de una letra ya pagada
 * por el cliente (después de 8 letras pagadas). Solo se habilita si el cliente
 * está al día y aún no se registró el pago de esa letra.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoComisionMensualDTO {

    private Integer idLetra;
    private String numeroLetra;
    private LocalDate fechaVencimiento;
    private BigDecimal importeLetra;
    /** 10% del importe de la letra, redondeado a entero (22.50 → 23). */
    private BigDecimal montoComision;
    /** true si el saldo pendiente es menor al 10% (sería el último pago). */
    private boolean ultimoPago;
    private boolean seleccionado;
}