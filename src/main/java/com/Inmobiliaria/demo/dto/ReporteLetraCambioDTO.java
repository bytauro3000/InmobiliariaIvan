package com.Inmobiliaria.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteLetraCambioDTO {

    private String numeroLetra;
    private LocalDate fechaGiro;
    private LocalDate fechaVencimiento;
    private BigDecimal importe;
    private String importeLetras;
    
    private String distritoNombre; 

    private String cliente1Nombre;
    private String cliente1Apellidos;
    private String cliente1NumDocumento;
    private String cliente2Nombre;
    private String cliente2Apellidos;
    private String cliente2NumDocumento;

    private String cliente1Direccion;
    
    private String cliente1Distrito;
}
