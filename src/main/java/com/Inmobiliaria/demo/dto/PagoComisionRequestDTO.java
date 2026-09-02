package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.enums.MedioPago;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Registro de pago de comisión a un vendedor (adelanto o pago mensual).
 * Para pagos mensuales multi-lote, {@code idLetras} puede cruzar varios contratos:
 * se genera UN solo recibo de egresos EG01 con el detalle por lote y el total.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoComisionRequestDTO {

    /** ADELANTO o MENSUAL */
    private String tipo;

    /** Obligatorio para ADELANTO. */
    private Integer idComision;

    /** Obligatorio para MENSUAL: ids de las letras pagadas cuya comisión se paga. */
    private List<Integer> idLetras;

    /** Monto del adelanto (para MENSUAL se calcula solo: 10% de cada letra). */
    private BigDecimal monto;

    private MedioPago medioPago;

    private String numeroOperacion;

    private LocalDate fechaOperacion;

    /** Por defecto se usa la fecha actual. */
    private LocalDate fechaPago;

    private String observacion;
}