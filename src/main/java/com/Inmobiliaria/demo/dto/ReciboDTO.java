package com.Inmobiliaria.demo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ReciboDTO {
    private Long idRecibo;
    private Integer idContrato; 
    private String tipoServicio; // LUZ o AGUA
    private Double lecturaAnterior;
    private Double lecturaActual;
    private BigDecimal consumoMes;
    private BigDecimal importeTotal;
    private LocalDate fechaGiro;
    private LocalDate fechaVencimiento;
    private String estado;
    private LocalDate fechaLectura;
}