package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleLetraVencidaDTO {

    private String           numeroLetra;
    private BigDecimal       importe;
    private LocalDate        fechaVencimiento;
    private int              diasMora;
    private BigDecimal       montoMora;
    private boolean          venceHoy;
    private String           estado;
    private String           nombreEmpresa;
}
