package com.Inmobiliaria.demo.entity;

import com.Inmobiliaria.demo.enums.MedioPago;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@MappedSuperclass
@Getter
@Setter
public abstract class PagoBase {

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @Column(name = "importe_pagado", nullable = false, precision = 12, scale = 2)
    private BigDecimal importePagado;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", length = 20)
    private MedioPago medioPago;

    @Column(name = "numero_operacion", length = 50)
    private String numeroOperacion;

    @Column(name = "observaciones", length = 255)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_comprobante")
    private Comprobante comprobante;
}