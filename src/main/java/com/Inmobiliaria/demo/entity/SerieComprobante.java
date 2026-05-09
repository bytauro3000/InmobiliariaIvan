package com.Inmobiliaria.demo.entity;

import com.Inmobiliaria.demo.enums.TipoComprobante;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "serie_comprobante",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_serie_comprobante_tipo_serie",
        columnNames = {"tipo_comprobante", "serie"}
    )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SerieComprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Columna en BD: "tipo_comprobante"
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", nullable = false, length = 10)
    private TipoComprobante tipoComprobante;

    @Column(name = "serie", nullable = false, length = 10)
    private String serie;

    @Column(name = "ultimo_numero", nullable = false)
    private Integer ultimoNumero = 0;

   
    @Version
    @Column(name = "version")
    private Long version;
}