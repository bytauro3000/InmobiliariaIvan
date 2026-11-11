package com.Inmobiliaria.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pago_mora")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagoMora extends PagoBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago_mora")
    private Integer idPagoMora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mora", nullable = false)
    private MoraLetra mora;

    @Column(name = "fecha_operacion")
    private LocalDateTime fechaOperacion;

    /** Descuento negociado proporcional aplicado a este pago (pago múltiple de mora). */
    @Column(name = "descuento_aplicado", precision = 12, scale = 2)
    private BigDecimal descuentoAplicado;
}