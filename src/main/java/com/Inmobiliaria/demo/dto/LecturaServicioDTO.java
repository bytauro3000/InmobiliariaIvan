package com.Inmobiliaria.demo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class LecturaServicioDTO {
    private Integer idContrato;
    private String clienteNombre; // Nombre + Apellidos
    private String manzana;        // Mz
    private String lote;           // Lt
    private Double lecturaAnterior;
    private Double lecturaActual;   // Lo llenará la secretaria
    private BigDecimal consumo;     
    private BigDecimal importe;     
}