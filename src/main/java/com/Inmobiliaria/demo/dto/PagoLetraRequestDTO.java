package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PagoLetraRequestDTO {
    private Integer idLetra;
    private BigDecimal importePagado;
    private MedioPago medioPago;
    private String numeroOperacion;
    private LocalDate fechaOperacion;
    private TipoComprobante tipoComprobante;
    private String numeroComprobante;
    private String observaciones;
    // La imagen se maneja aparte como MultipartFile
}