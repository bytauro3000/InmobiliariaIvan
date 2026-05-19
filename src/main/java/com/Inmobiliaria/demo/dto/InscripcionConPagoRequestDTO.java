package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InscripcionConPagoRequestDTO {

    // Datos de la inscripción
    private Integer     idContrato;
    private String      tipoServicio;       // "LUZ" o "AGUA"

    // Datos del pago / comprobante
    private BigDecimal  montoPagado;
    private LocalDate   fechaPago;
    private MedioPago   medioPago;
    private String      numeroOperacion;
    private String      observaciones;
    private TipoComprobante tipoComprobante;
    private String      numeroComprobantePersonalizado;
}