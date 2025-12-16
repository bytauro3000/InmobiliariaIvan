package com.Inmobiliaria.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import com.Inmobiliaria.demo.enums.EstadoUsuario;
import java.util.Date;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idUsuario")
    private Integer id;

    @Column(length = 80, nullable = false)
    private String nombres;

    @Column(length = 100, nullable = false)
    private String apellidos;

    @Column(length = 100, nullable = false, unique = true)
    private String correo;

    @Column(length = 100, nullable = false)
    private String contrasena;

    @Column(length = 20)
    private String telefono;

    @Column(length = 150)
    private String direccion;

    @Column(length = 8)
    private String dni; 

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoUsuario estado = EstadoUsuario.activo;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fechaRegistro", nullable = false, updatable = false)
    private Date fechaRegistro;

    @ManyToOne
    @JoinColumn(name = "idRolUsuario", nullable = false)
    private RolUsuario rol;
}
