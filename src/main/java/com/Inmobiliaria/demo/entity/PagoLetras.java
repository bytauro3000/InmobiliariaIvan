package com.Inmobiliaria.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pago_letra")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PagoLetras {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Integer idPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_letra", nullable = false)
    private LetraCambio letra;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @Column(name = "importe_pagado", nullable = false, precision = 12, scale = 2)
    private BigDecimal importePagado;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", length = 20)
    private MedioPago medioPago;

    @Column(name = "numero_operacion", length = 50)
    private String numeroOperacion;

    @Column(name = "fecha_operacion")
    private LocalDate fechaOperacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante")
    private TipoComprobante tipoComprobante;

    @Column(name = "numero_comprobante", length = 50)
    private String numeroComprobante;

    @Column(name = "observaciones", length = 255)
    private String observaciones;

    // Marca si el comprobante ya fue enviado por email — evita reenvíos
    @Column(name = "email_enviado", nullable = false)
    private boolean emailEnviado = false;
    
    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Voucher> vouchers = new ArrayList<>();
}