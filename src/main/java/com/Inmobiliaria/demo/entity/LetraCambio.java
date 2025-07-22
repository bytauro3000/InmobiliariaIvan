package com.Inmobiliaria.demo.entity;

import java.util.Date;

import com.Inmobiliaria.demo.enums.EstadoLetra;
import com.Inmobiliaria.demo.enums.TipoComprobante;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "LetraCambio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LetraCambio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_letra")
    private Integer idLetra;

    @ManyToOne
    @JoinColumn(name = "id_contrato", nullable = false)
    private Contrato contrato;

    @ManyToOne
    @JoinColumn(name = "id_distrito", nullable = false)
    private Distrito distrito;

    @Column(name = "numero_letra", nullable = false)
    private String numeroLetra;

    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_giro", nullable = false)
    private Date fechaGiro;

    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_vencimiento", nullable = false)
    private Date fechaVencimiento;

    @Column(name = "importe", nullable = false)
    private Double importe;

    @Column(name = "importe_letras")
    private String importeLetras;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoLetra estado;

    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_pago")
    private Date fechaPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante")
    private TipoComprobante tipoComprobante;

    @Column(name = "numero_comprobante")
    private String numeroComprobante;

    @Column(name = "observaciones")
    private String observaciones;
}
