package com.Inmobiliaria.demo.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.Inmobiliaria.demo.enums.TipoContrato;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LetraCambioDTO {

    private Integer idLetra;
    private Date fechaGiro;
    private Date fechaVencimiento;
    private BigDecimal importe;
    private String importeLetras;
    private String estadoLetra;
    private String numeroLetra;
    
    
    private Integer idContrato;
    private String nombreCliente;

}