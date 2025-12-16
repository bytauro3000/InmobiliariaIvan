package com.Inmobiliaria.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "separacion_lote")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeparacionLote {

    @EmbeddedId
    private SeparacionLoteId id;

    @ManyToOne
    @MapsId("idSeparacion")
    @JoinColumn(name = "id_separacion")
    @JsonIgnore
    private Separacion separacion;

    @ManyToOne
    @MapsId("idLote")
    @JoinColumn(name = "id_lote")
    private Lote lote;
}