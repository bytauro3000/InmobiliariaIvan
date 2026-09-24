package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbonoInscripcionRequestDTO {

    private Integer        idInscripcion;
    private Integer        idContrato;
    private String         tipoServicio;
    private BigDecimal     montoPagado;
    private LocalDate      fechaPago;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate      fechaOperacion;

    /** Hora exacta del voucher (ISO: HH:mm o HH:mm:ss); si llega, se respeta aunque el día sea otro. */
    private LocalTime      horaOperacion;

    private MedioPago      medioPago;
    private String         numeroOperacion;
    private String         observaciones;
    private TipoComprobante tipoComprobante;
    private String         numeroComprobantePersonalizado;
}