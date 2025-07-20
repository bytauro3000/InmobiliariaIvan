package com.Inmobiliaria.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "Programa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Programa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_programa")
    private Integer idPrograma;

    @Column(name = "nombre_programa", nullable = false, length = 100)
    private String nombrePrograma;

    @Column(name = "ubicacion", length = 150)
    private String ubicacion;

    @Column(name = "area_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal areaTotal;

    @Column(name = "precio_m2", precision = 10, scale = 2)
    private BigDecimal precioM2;

    @Column(name = "costo_total", precision = 12, scale = 2)
    private BigDecimal costoTotal;

    @ManyToOne
    @JoinColumn(name = "id_parcelero")
    private Parcelero parcelero;

    @ManyToOne
    @JoinColumn(name = "id_distrito")
    private Distrito distrito;
}
