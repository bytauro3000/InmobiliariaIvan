package com.Inmobiliaria.demo.entity;

import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.enums.TipoOrigenComprobante;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Table(name = "comprobante")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Comprobante {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comprobante")
    private Long idComprobante;


    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", nullable = false, length = 10)
    private TipoComprobante tipoComprobante;

    @Column(name = "serie", nullable = false, length = 10)
    private String serie;


    @Column(name = "numero", nullable = false)
    private Integer numero;


    @Column(name = "numero_completo", nullable = false, length = 20)
    private String numeroCompleto;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;


    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

 
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_origen", nullable = false, length = 25)
    private TipoOrigenComprobante tipoOrigen;


    @Column(name = "referencia_id")
    private Integer referenciaId;


    @Column(name = "email_enviado", nullable = false)
    private boolean emailEnviado = false;
}