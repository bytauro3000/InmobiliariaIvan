package com.Inmobiliaria.demo.entity;

import jakarta.persistence.*;
import lombok.*;

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
}