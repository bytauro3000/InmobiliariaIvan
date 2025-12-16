package com.Inmobiliaria.demo.entity;


import java.io.Serializable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rol_usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolUsuario implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idRolUsuario")
    private Integer idRolUsuario;

    @Column(name = "rolUsuario", nullable = false, length = 50)
    private String rolUsuario;
}
