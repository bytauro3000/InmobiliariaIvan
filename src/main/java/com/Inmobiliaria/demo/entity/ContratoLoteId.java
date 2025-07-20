package com.Inmobiliaria.demo.entity;

import java.io.Serializable;
import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContratoLoteId implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private Integer idContrato;
    private Integer idLote;
}
