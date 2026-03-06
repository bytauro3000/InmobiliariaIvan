package com.Inmobiliaria.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    @Column(name = "url_voucher", length = 500)  // URL de Cloudinary
    private String urlVoucher;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante")
    private TipoComprobante tipoComprobante;

    @Column(name = "numero_comprobante", length = 50)
    private String numeroComprobante;

    @Column(name = "observaciones", length = 255)
    private String observaciones;
}