package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.enums.EstadoLetra;
import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PagoLetraResponseDTO {

    private Integer         idPago;
    private Integer         idLetra;
    private String          numeroLetra;
    private LocalDate       fechaPago;
    private LocalDate       fechaOperacion;
    private BigDecimal      importePagado;
    private MedioPago       medioPago;
    private String          numeroOperacion;
    private TipoComprobante tipoComprobante;
    private String          numeroComprobante;
    private Long            idComprobante;
    private String          observaciones;
    private List<String>    urlsVoucher;

    private BigDecimal      importeLetra;
    private BigDecimal      saldoPendiente;
    private EstadoLetra     estadoLetra;
    private Boolean         esPagoAcuenta;

    // ── SUNAT ─────────────────────────────────────────────────────────────────
    private Boolean         sunatAceptado;
    private String          sunatMensaje;
    private String          sunatHash;

    // ── Anulación ─────────────────────────────────────────────────────────────
    private Boolean         anulado;
    private String          motivoAnulacion;
    private LocalDateTime   fechaAnulacion;
    private String          anuladoPor;

    // ── Contexto admin (solo se popula en /letras/todos) ──────────────────────
    private Integer         idContrato;
    private String          manzana;
    private String          numeroLote;
    private Integer         idPrograma;
    private String          nombrePrograma;
    private String          nombreCliente;
    private String          moneda;   // "USD" o "PEN" según el contrato
}