package com.Inmobiliaria.demo.entity;

import java.io.Serializable;
import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ContratoLoteId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id_contrato")
    private Integer idContrato;

    @Column(name = "id_lote")
    private Integer idLote;
}
