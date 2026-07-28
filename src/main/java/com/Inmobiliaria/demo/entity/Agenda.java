package com.Inmobiliaria.demo.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import com.Inmobiliaria.demo.enums.EstadoAgenda;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agenda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Agenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_agenda")
    private Integer idAgenda;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora")
    private LocalTime hora;

    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "nombre_cliente", length = 150)
    private String nombreCliente;

    @Column(name = "telefono_cliente", length = 20)
    private String telefonoCliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20)
    private EstadoAgenda estado = EstadoAgenda.PENDIENTE;

    @Column(name = "recordatorio_enviado", nullable = false)
    private Boolean recordatorioEnviado = false;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private java.time.LocalDateTime fechaCreacion;

    @Column(name = "usuario_creacion", length = 100)
    private String usuarioCreacion;

    @PrePersist
    public void prePersist() {
        this.fechaCreacion = java.time.LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoAgenda.PENDIENTE;
        }
        if (this.recordatorioEnviado == null) {
            this.recordatorioEnviado = false;
        }
    }
}