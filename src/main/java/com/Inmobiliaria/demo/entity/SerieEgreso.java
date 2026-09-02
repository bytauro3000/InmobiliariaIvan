package com.Inmobiliaria.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "serie_egreso",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_serie_egreso_serie",
        columnNames = {"serie"}
    )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SerieEgreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "serie", nullable = false, length = 10)
    private String serie;

    @Column(name = "ultimo_numero", nullable = false)
    private Integer ultimoNumero = 0;

    @Version
    @Column(name = "version")
    private Long version;
}