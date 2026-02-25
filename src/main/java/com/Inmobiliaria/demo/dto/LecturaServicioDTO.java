package com.Inmobiliaria.demo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class LecturaServicioDTO {
    private Integer idContrato;
    private String clienteNombre;   // Nombre + Apellidos
    private String nombrePrograma;  // Para saber a qué asociación pertenece
    private String manzana;         // Mz
    private String lote;            // Lt (numero_lote)
    private Double lecturaAnterior;
    private Double lecturaActual;   // Lo llenará el personal de campo
    private BigDecimal consumo;     // Lectura Actual - Lectura Anterior
    private BigDecimal importe;     // Consumo * Precio Unitario
}