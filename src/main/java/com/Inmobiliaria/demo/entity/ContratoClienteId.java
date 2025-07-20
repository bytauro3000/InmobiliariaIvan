package com.Inmobiliaria.demo.entity;

import java.io.Serializable;
import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContratoClienteId implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private Integer idContrato;
    private Integer idCliente;
}
