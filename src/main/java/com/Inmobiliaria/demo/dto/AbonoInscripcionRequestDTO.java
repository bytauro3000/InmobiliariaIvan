package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbonoInscripcionRequestDTO {

    private Integer        idInscripcion;
    private Integer        idContrato;
    private String         tipoServicio;
    private BigDecimal     montoPagado;
    private LocalDate      fechaPago;
    private MedioPago      medioPago;
    private String         numeroOperacion;
    private String         observaciones;
    private TipoComprobante tipoComprobante;
    private String         numeroComprobantePersonalizado;
}