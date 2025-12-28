package com.Inmobiliaria.demo.entity;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false) 
public class ContratoClienteId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id_contrato")
    private Integer idContrato;

    @Column(name = "id_cliente")
    private Integer idCliente;
}