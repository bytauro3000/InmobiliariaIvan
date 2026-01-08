package com.Inmobiliaria.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LetraResponseDTO {
    private String numeroLetra;
    private LocalDate fechaVencimiento;
    private BigDecimal importe;
    private String importeLetras;
}