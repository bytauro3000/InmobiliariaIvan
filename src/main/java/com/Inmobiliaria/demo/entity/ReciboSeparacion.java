package com.Inmobiliaria.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "recibo_separacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReciboSeparacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recibo")
    private Integer idRecibo;

    @ManyToOne
    @JoinColumn(name = "id_separacion", nullable = false)
    private Separacion separacion;

    @Column(name = "numero_recibo", length = 20, unique = true)
    private String numeroRecibo;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_emision", nullable = false)
    private Date fechaEmision;
}