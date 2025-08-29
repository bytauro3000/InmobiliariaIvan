package com.Inmobiliaria.demo.entity;

import java.util.Date;

import com.Inmobiliaria.demo.enums.EstadoLote;
import com.Inmobiliaria.demo.enums.EstadoSeparacion;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Separacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Separacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_separacion")
    private Integer idSeparacion;

    @ManyToOne
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "id_vendedor")
    private Vendedor vendedor;

    @ManyToOne
    @JoinColumn(name = "id_lote")
    private Lote lote;

    @Column(name = "monto", nullable = false)
    private Double monto;

    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_separacion", nullable = false)
    private Date fechaSeparacion;

    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_limite", nullable = false)
    private Date fechaLimite;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20)
    private EstadoSeparacion estado = EstadoSeparacion.EN_PROCESO;

    @Column(name = "observaciones")
    private String observaciones;
    
    //CONSTRUTOR PARA MOSTRAR LOS DATOS DEL CLIENTE Y LOTE EN LA BUSQUEDA DE SEPARACON CONTRATO
    public Separacion(Integer idSeparacion, Cliente cliente, Lote lote) {
        this.idSeparacion = idSeparacion;
        this.cliente = cliente;
        this.lote = lote;
    }
}
