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
public class SeparacionClienteId implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer idSeparacion;
    private Integer idCliente;
}