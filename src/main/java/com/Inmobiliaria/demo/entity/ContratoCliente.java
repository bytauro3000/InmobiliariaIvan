package com.Inmobiliaria.demo.entity;
import com.Inmobiliaria.demo.enums.TipoPropietario;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contrato_cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContratoCliente {

    @EmbeddedId
    private ContratoClienteId id;

    @ManyToOne
    @MapsId("idContrato")
    @JoinColumn(name = "id_contrato")
    private Contrato contrato;

    @ManyToOne
    @MapsId("idCliente")
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_propietario", nullable = false)
    private TipoPropietario tipoPropietario;

    @Column(name = "orden", nullable = false)
    private Integer orden = 0;
}