package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoInicialResponseDTO {

    private Integer         idPagoInicial;
    private BigDecimal      importePagado;
    private LocalDate       fechaPago;
    private MedioPago       medioPago;
    private String          numeroOperacion;
    private String          observaciones;
    private List<String>    urlsVoucher;
    private Long            idComprobante;
    private TipoComprobante tipoComprobante;
    private String          numeroComprobante;
}