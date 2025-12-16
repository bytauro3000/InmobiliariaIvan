package com.Inmobiliaria.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.Inmobiliaria.demo.enums.EstadoLetra;
import com.Inmobiliaria.demo.enums.TipoComprobante;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "letra_cambio")
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
    @JoinColumn(name = "id_contrato")
    private Contrato contrato;

    @ManyToOne
    @JoinColumn(name = "id_distrito", nullable = false)
    private Distrito distrito;

    @Column(name = "numero_letra", nullable = false, length = 50)
    private String numeroLetra;

    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_giro", nullable = false)
    private LocalDate fechaGiro;

    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate  fechaVencimiento;

    @Column(name = "importe", nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;

    @Column(name = "importe_letras", length = 255)
    private String importeLetras;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_letra", nullable = false)
    private EstadoLetra estadoLetra = EstadoLetra.PENDIENTE;

    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_pago", nullable = true)
    private LocalDate fechaPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", nullable = true)
    private TipoComprobante tipoComprobante;

    @Column(name = "numero_comprobante", length = 50)
    private String numeroComprobante;

    @Column(name = "observaciones", nullable = true)
    private String observaciones;
}
