package com.Inmobiliaria.demo.entity;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    
    @OneToMany(mappedBy = "separacion", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<SeparacionCliente> clientes = new LinkedHashSet<>();

    @OneToMany(mappedBy = "separacion", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<SeparacionLote> lotes = new LinkedHashSet<>();
}