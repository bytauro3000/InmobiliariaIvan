package com.Inmobiliaria.demo.dto.apisperu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApisperuInvoiceRequest {
    private String ublVersion;
    private String tipoOperacion;
    private String tipoDoc;
    private String serie;
    private String correlativo;
    private String fechaEmision;
    private ApisperuFormaPago formaPago;
    private String tipoMoneda;
    private ApisperuClient client;
    private ApisperuCompany company;
    private BigDecimal mtoOperGravadas;
    private BigDecimal mtoOperInafectas;
    private BigDecimal mtoIGV;
    private BigDecimal valorVenta;
    private BigDecimal totalImpuestos;
    private BigDecimal subTotal;
    private BigDecimal mtoImpVenta;
    private List<ApisperuDetail> details;
    private List<ApisperuLegend> legends;
    private String montoLetras;
}