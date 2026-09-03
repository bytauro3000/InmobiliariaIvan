package com.Inmobiliaria.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Recibo de egresos (salida de dinero, ej. pago de comisión a vendedor).
 * Se numera con serie propia "EG01" de 1 en 1, reutilizando el mecanismo de
 * {@code serie_comprobante} (contador con lock pesimista).
 */
@Entity
@Table(name = "recibo_egreso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReciboEgreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recibo_egreso")
    private Long idReciboEgreso;

    @Column(name = "serie", nullable = false, length = 10)
    private String serie;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Column(name = "numero_completo", nullable = false, length = 30)
    private String numeroCompleto;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    /** Concepto del egreso (ej: "Comisión vendedor - Adelanto", "Comisión - letra N"). */
    @Column(name = "concepto", nullable = false, length = 500)
    private String concepto;

    /** Beneficiario (nombre del vendedor). */
    @Column(name = "beneficiario", nullable = false, length = 255)
    private String beneficiario;

    /** DNI del beneficiario (vendedor) para el recibo. */
    @Column(name = "dni_beneficiario", length = 20)
    private String dniBeneficiario;

    /** Nombre del usuario que registró el pago (quien realizó el egreso). */
    @Column(name = "usuario_registro", length = 255)
    private String usuarioRegistro;

    /** Contrato asociado (puede ser null en egresos genéricos). */
    @Column(name = "id_contrato")
    private Integer idContrato;

    @Column(name = "monto", precision = 12, scale = 2, nullable = false)
    private BigDecimal monto;

    @Column(name = "moneda", length = 10, nullable = false)
    private String moneda;

    /** Medio de pago (EFECTIVO, DEPOSITO, TRANSFERENCIA, YAPE, PLIN...). */
    @Column(name = "medio_pago", length = 20)
    private String medioPago;

    /** Número de operación bancaria (si aplica). */
    @Column(name = "numero_operacion", length = 100)
    private String numeroOperacion;

    /** Fecha de la operación (del voucher). */
    @Column(name = "fecha_operacion")
    private LocalDate fechaOperacion;
}