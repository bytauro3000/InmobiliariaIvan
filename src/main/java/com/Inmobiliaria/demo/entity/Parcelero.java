package com.Inmobiliaria.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Parcelero")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Parcelero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parcelero")
    private Integer idParcelero;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "dni", nullable = false, unique = true, length = 15)
    private String dni;

    @Column(name = "celular", length = 20)
    private String celular;

    @Column(name = "direccion", length = 150)
    private String direccion;

    @Column(name = "email", length = 100)
    private String email;

    @ManyToOne
    @JoinColumn(name = "id_distrito")
    private Distrito distrito;
}
