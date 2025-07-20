package com.Inmobiliaria.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "Lote")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lote")
    private Integer idLote;

    @Column(name = "manzana", nullable = false, length = 10)
    private String manzana;

    @Column(name = "numero_lote", nullable = false, length = 10)
    private String numeroLote;

    @Column(name = "area", nullable = false, precision = 10, scale = 2)
    private BigDecimal area;

    @Column(name = "largo1", precision = 5, scale = 2)
    private BigDecimal largo1;

    @Column(name = "largo2", precision = 5, scale = 2)
    private BigDecimal largo2;

    @Column(name = "ancho1", precision = 5, scale = 2)
    private BigDecimal ancho1;

    @Column(name = "ancho2", precision = 5, scale = 2)
    private BigDecimal ancho2;

    @Column(name = "precio_m2", precision = 10, scale = 2)
    private BigDecimal precioM2;

    @Column(name = "colindante_norte", length = 100)
    private String colindanteNorte;

    @Column(name = "colindante_sur", length = 100)
    private String colindanteSur;

    @Column(name = "colindante_este", length = 100)
    private String colindanteEste;

    @Column(name = "colindante_oeste", length = 100)
    private String colindanteOeste;

    @Column(name = "estado", length = 20)
    private String estado = "Disponible";

    @ManyToOne
    @JoinColumn(name = "id_programa")
    private Programa programa;
}
