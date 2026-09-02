package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Comisión de vendedor por contrato (vista de secretaría).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComisionVendedorDTO {

    private Integer idComision;
    private Integer idContrato;
    private String nombreVendedor;
    private String nombreCliente;
    private String programa;
    private String manzanas;
    private String numeroLotes;
    private BigDecimal porcentajeComision;
    private BigDecimal montoTotalContrato;
    private BigDecimal montoComisionTotal;
    private String moneda;
    private BigDecimal montoAdelanto;
    private BigDecimal saldoPendiente;
    private String estado;
    /** true: la primera letra ya fue pagada y el adelanto aún no se registra. */
    private boolean adelantoHabilitado;
    /** Monto sugerido para el adelanto (30% de la inicial, o adelanto del programa). */
    private BigDecimal montoAdelantoSugerido;
    /** Cantidad de letras pagadas del contrato. */
    private long cantidadLetrasPagadas;
    private LocalDate fechaCreacion;
    /** Fecha del contrato (para ordenar la lista). */
    private LocalDate fechaContrato;
    /** Pagos mensuales de comisión acumulados pendientes (letras pagadas tras la 8ª sin pagar comisión). */
    private long pagosMensualesPendientes;
    /** Nivel de color: VERDE (0) / NARANJA (1-2) / ROJO (3+). */
    private String nivelColor;
    /** Cantidad de pagos mensuales de comisión ya registrados. */
    private long pagosMensualesRegistrados;
}