package com.Inmobiliaria.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PagosMultiplesRequestDTO {

    private List<PagoLetraRequestDTO> pagos;
    private BigDecimal descuentoNegociado;
    private String motivoDescuento;
    private Integer idLetraGratis;
    private String motivoLetraGratis;
}