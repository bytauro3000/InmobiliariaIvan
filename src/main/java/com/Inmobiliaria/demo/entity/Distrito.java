package com.Inmobiliaria.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Distrito")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Distrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_distrito")
    private Integer idDistrito;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;
}
