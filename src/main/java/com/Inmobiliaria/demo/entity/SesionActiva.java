package com.Inmobiliaria.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sesion_activa")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class SesionActiva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime ultimoRefresh;

    @Column(length = 50)
    private String ip;

    @Column(length = 500, name = "user_agent")
    private String userAgent;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(nullable = false)
    private LocalDateTime fechaLogueo;
}
