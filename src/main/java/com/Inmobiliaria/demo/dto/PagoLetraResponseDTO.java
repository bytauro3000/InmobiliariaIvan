package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.enums.EstadoLetra;
import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@Data
public class PagoLetraResponseDTO {

    private Integer         idPago;
    private Integer         idLetra;
    private String          numeroLetra;
    private LocalDate       fechaPago;
    private BigDecimal      importePagado;
    private MedioPago       medioPago;
    private String          numeroOperacion;
    private TipoComprobante tipoComprobante;
    private String          numeroComprobante;
    private Long            idComprobante;
    private String          observaciones;
    private List<String>    urlsVoucher;

    // ── Campos de pago parcial ─────────────────────────────────────────────────
    /** Importe original de la letra (sin descuentos). */
    private BigDecimal      importeLetra;
    /** Saldo que queda por pagar después de este pago. 0 = letra completada. */
    private BigDecimal      saldoPendiente;
    /** Estado actual de la letra tras este pago. */
    private EstadoLetra     estadoLetra;
    /** true si este pago fue registrado como pago a cuenta (parcial). */
    private Boolean         esPagoAcuenta;
}