package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PagoMoraResponseDTO {

    private Integer         idPagoMora;
    private Integer         idMora;
    private BigDecimal      montoPagado;
    private LocalDate       fechaPago;
    private MedioPago       medioPago;
    private String          numeroOperacion;
    private TipoComprobante tipoComprobante;
    private String          numeroComprobante;
    private Long            idComprobante;
    private String          observaciones;
    private List<String>    urlsVoucher;

    // ── Anulación ─────────────────────────────────────────────────────────────
    private Boolean         anulado;
    private String          motivoAnulacion;
    private LocalDateTime   fechaAnulacion;
    private String          anuladoPor;

    // ── Contexto admin (solo se popula en /moras/pagos/todos) ─────────────────
    private Integer         idContrato;
    private String          manzana;
    private String          numeroLote;
    private Integer         idPrograma;
    private String          nombrePrograma;
    private String          nombreCliente;
    private String          moneda;   // "USD" o "PEN"
}