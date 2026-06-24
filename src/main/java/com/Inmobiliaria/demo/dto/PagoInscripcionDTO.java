package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para pagos de inscripción de servicios básicos.
 *
 * Campos base     → usados por secretaria y admin (/pagos)
 * Campos anulación→ seteados siempre; el frontend decide si mostrarlos
 * Campos admin    → nombreCliente; solo se popula en /pagos/todos (admin)
 */
@Data
@NoArgsConstructor
public class PagoInscripcionDTO {

    // ── Identificación ────────────────────────────────────────────────────────
    private Integer         idPagoInscripcionComprobante;
    private Integer         idContrato;

    // ── Datos del pago ────────────────────────────────────────────────────────
    private BigDecimal      importePagado;
    private LocalDate       fechaPago;
    private MedioPago       medioPago;
    private String          numeroOperacion;
    private String          observaciones;
    private String          tipoServicio;   // "LUZ" | "AGUA"

    // ── Comprobante ───────────────────────────────────────────────────────────
    private TipoComprobante tipoComprobante;
    private String          numeroComprobante;
    private LocalDate       fechaEmision;
    private Long            idComprobante;

    // ── Ubicación del lote ────────────────────────────────────────────────────
    private Integer         idPrograma;
    private String          nombrePrograma;
    private String          manzana;
    private String          numeroLote;

    // ── Anulación ─────────────────────────────────────────────────────────────
    private Boolean         anulado;
    private String          motivoAnulacion;
    private LocalDateTime   fechaAnulacion;
    private String          anuladoPor;

    // ── Solo admin (/pagos/todos) ─────────────────────────────────────────────
    private String          nombreCliente;
    private String          moneda;   // "USD" o "PEN"
}