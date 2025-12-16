package com.Inmobiliaria.demo.entity;

import com.Inmobiliaria.demo.enums.TipoPropietario;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "separacion_cliente")
public class SeparacionCliente {

    @EmbeddedId
    private SeparacionClienteId id;

    @ManyToOne
    @MapsId("idSeparacion")
    @JoinColumn(name = "id_separacion")
    @JsonIgnore
    private Separacion separacion;

    @ManyToOne
    @MapsId("idCliente")
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_propietario", nullable = false)
    private TipoPropietario tipoPropietario;
}