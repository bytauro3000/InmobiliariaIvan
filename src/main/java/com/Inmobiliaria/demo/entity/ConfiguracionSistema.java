package com.Inmobiliaria.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "configuracion_sistema")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ConfiguracionSistema {

    @Id
    @Column(name = "clave", length = 50)
    private String clave;

    @Column(name = "valor", nullable = false, length = 100)
    private String valor;

    @Column(name = "descripcion", length = 255)
    private String descripcion;
}