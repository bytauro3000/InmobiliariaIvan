package com.Inmobiliaria.demo.entity;

import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pago_mora")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagoMora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago_mora")
    private Integer idPagoMora;

    // Mora que se está pagando
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mora", nullable = false)
    private MoraLetra mora;

    @Column(name = "monto_pagado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoPagado;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", length = 20)
    private MedioPago medioPago;

    @Column(name = "numero_operacion", length = 50)
    private String numeroOperacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", length = 20)
    private TipoComprobante tipoComprobante;

    @Column(name = "numero_comprobante", length = 50)
    private String numeroComprobante;

    @Column(name = "observaciones", length = 255)
    private String observaciones;
}