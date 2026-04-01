package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de solo lectura que devuelve el cálculo de mora ANTES de registrar el pago.
 * El frontend lo usa para mostrar al operador cuánto debe pagar el cliente
 * cuando se detecta que la letra está vencida.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalculoMoraDTO {

    private Integer idLetra;
    private String  numeroLetra;
    private BigDecimal importeLetra;

    private LocalDate fechaVencimiento;
    private LocalDate fechaCalculo;      // Fecha en que se hace el cálculo (hoy)

    private Integer    diasMora;
    private BigDecimal montoPorcentaje;  // importe * 5%
    private BigDecimal montoDiario;      // dias * $1
    private BigDecimal montoMoraTotal;   // total mora

    // true si ya existe una mora registrada para esta letra (no se genera otra)
    private boolean tieneMoreaPrevia;
    private Integer idMoraPrevia;        // si ya existe, se devuelve su ID para pagarla
}