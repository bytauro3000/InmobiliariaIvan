package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PagoLetraResponseDTO {
    private Integer idPago;
    private Integer idLetra;
    private String numeroLetra;
    private LocalDate fechaPago;
    private BigDecimal importePagado;
    private MedioPago medioPago;
    private String numeroOperacion;
    private LocalDate fechaOperacion;
    private String urlVoucher;
    private TipoComprobante tipoComprobante;
    private String numeroComprobante;
    private String observaciones;
}