package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;


@Data
public class PagoLetraRequestDTO {

    private Integer         idLetra;
    private BigDecimal      importePagado;
    private MedioPago       medioPago;
    private String          numeroOperacion;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate       fechaPago;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate       fechaOperacion;
    private TipoComprobante tipoComprobante;
    private String          numeroComprobantePersonalizado;
    private String          observaciones;
    private Boolean esPagoAcuenta = false;
    private String          seriePersonalizada;
}