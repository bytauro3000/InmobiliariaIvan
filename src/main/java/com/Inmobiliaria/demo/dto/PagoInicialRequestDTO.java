package com.Inmobiliaria.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoInicialRequestDTO {

    private BigDecimal importePagado;
    private LocalDate  fechaPago;
    private MedioPago  medioPago;
    private String     numeroOperacion;
    private String     observaciones;
    private TipoComprobante tipoComprobante;
    private String     numeroComprobantePersonalizado;
}