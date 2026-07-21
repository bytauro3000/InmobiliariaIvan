package com.Inmobiliaria.demo.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DetalleVentaDTO {
    private BigDecimal montoTotal;
    private Integer cantidadLetras;
    private List<String> clientes;
    private List<String> lotes;
}
