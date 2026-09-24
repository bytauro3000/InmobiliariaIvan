package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;


@Data
public class PagoMoraRequestDTO {

    private Integer         idMora;
    private BigDecimal      montoPagado;
    private LocalDate       fechaPago;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate       fechaOperacion;

    /** Hora exacta del voucher (ISO: HH:mm o HH:mm:ss); si llega, se respeta aunque el día sea otro. */
    private LocalTime       horaOperacion;

    private MedioPago       medioPago;
    private String          numeroOperacion;
    private TipoComprobante tipoComprobante;
    private String          numeroComprobantePersonalizado;
    private String          observaciones;
}