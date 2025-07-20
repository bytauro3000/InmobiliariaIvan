package com.Inmobiliaria.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ContratoLote")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContratoLote {

    @EmbeddedId
    private ContratoLoteId id;

    @ManyToOne
    @MapsId("idContrato")
    @JoinColumn(name = "id_contrato")
    private Contrato contrato;

    @ManyToOne
    @MapsId("idLote")
    @JoinColumn(name = "id_lote")
    private Lote lote;
}
