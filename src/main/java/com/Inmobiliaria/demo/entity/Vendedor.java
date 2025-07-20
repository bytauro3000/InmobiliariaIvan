package com.Inmobiliaria.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Vendedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vendedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vendedor")
    private Integer idVendedor;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "dni", nullable = false, unique = true, length = 20)
    private String dni;

    @Column(name = "celular", length = 20)
    private String celular;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "direccion", length = 150)
    private String direccion;

    @Column(name = "codigo", unique = true, length = 20)
    private String codigo;

    @Column(name = "zona", length = 50)
    private String zona;

    @ManyToOne
    @JoinColumn(name = "id_distrito")
    private Distrito distrito;
}
