package com.Inmobiliaria.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "voucher")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idVoucher;

    @Column(name = "tipo_origen", nullable = false, length = 20)
    private String tipoOrigen; // "PAGO_LETRA" | "PAGO_INICIAL" | "PAGO_MORA"

    @Column(name = "referencia_id", nullable = false)
    private Integer referenciaId;

    @Column(name = "url", length = 500, nullable = false)
    private String url;
}