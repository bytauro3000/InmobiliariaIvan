package com.Inmobiliaria.demo.entity;

import java.util.Date;
import java.util.List;
import com.Inmobiliaria.demo.enums.EstadoSeparacion;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "separacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Separacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_separacion")
    private Integer idSeparacion;

    @ManyToOne
    @JoinColumn(name = "id_vendedor")
    private Vendedor vendedor;

    @Column(name = "monto", nullable = false)
    private Double monto;

    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_separacion", nullable = false)
    private Date fechaSeparacion;

    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_limite", nullable = false)
    private Date fechaLimite;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20)
    private EstadoSeparacion estado = EstadoSeparacion.EN_PROCESO;

    @Column(name = "observaciones")
    private String observaciones;
    
    // Estas son las relaciones que ahora mandan
    @OneToMany(mappedBy = "separacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SeparacionCliente> clientes;

    @OneToMany(mappedBy = "separacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SeparacionLote> lotes;
}