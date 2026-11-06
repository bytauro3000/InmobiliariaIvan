package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngresoDetalladoItemDTO {

    private Long idComprobante;
    private String numeroCompleto;
    private String tipoComprobante;      // BOLETA | RECIBO
    private LocalDate fechaEmision;
    private BigDecimal monto;
    private String moneda;               // USD | PEN | N/D
    private String dni;
    private String receptor;             // "12345678 - Apellidos, Nombres"
    private String estado;               // VIGENTE | ANULADO_NC | ANULADO_PAGO
    private String ncNumeroCompleto;     // nro. de la nota de crédito que lo anuló (null si está vigente)
    private LocalDate ncFechaEmision;
}
