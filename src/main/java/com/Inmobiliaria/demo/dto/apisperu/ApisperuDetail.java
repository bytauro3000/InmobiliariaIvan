package com.Inmobiliaria.demo.dto.apisperu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApisperuDetail {
    private String unidad;
    private String descripcion;
    private BigDecimal cantidad;
    private BigDecimal mtoValorUnitario;
    private BigDecimal mtoValorVenta;
    private BigDecimal mtoBaseIgv;
    private BigDecimal porcentajeIgv;
    private BigDecimal igv;
    private String tipAfeIgv;
    private BigDecimal totalImpuestos;
    private BigDecimal mtoPrecioUnitario;
    private String codProducto;
}