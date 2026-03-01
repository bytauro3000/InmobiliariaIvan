package com.Inmobiliaria.demo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ReciboConClienteDTO {
    private Long idRecibo;
    private Integer idContrato;
    private String tipoServicio;
    private Double lecturaAnterior;
    private Double lecturaActual;
    private BigDecimal consumoMes;
    private BigDecimal importeTotal;
    private LocalDate fechaGiro;
    private LocalDate fechaVencimiento;
    private String estado;
    private LocalDate fechaLectura;
    private String nombreCliente;
    private String manzana;
    private String lote;
    private String nombrePrograma; 
}