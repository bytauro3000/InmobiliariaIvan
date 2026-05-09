package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;


@Data
public class PagoMoraRequestDTO {

    private Integer         idMora;
    private BigDecimal      montoPagado;
    private LocalDate       fechaPago;
    private MedioPago       medioPago;
    private String          numeroOperacion;
    private TipoComprobante tipoComprobante;
    private String          numeroComprobantePersonalizado;
    private String          observaciones;
}