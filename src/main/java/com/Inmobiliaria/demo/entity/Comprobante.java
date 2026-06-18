package com.Inmobiliaria.demo.entity;

import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.enums.TipoOrigenComprobante;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "comprobante")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Comprobante {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comprobante")
    private Long idComprobante;


    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", nullable = false, length = 15)
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

    @Column(name = "hash_cdr", length = 64)
    private String hashCdr;

    @Lob
    @Column(name = "cdr_base64", columnDefinition = "MEDIUMTEXT")
    private String cdrBase64;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comprobante_referencia_id")
    private Comprobante comprobanteReferencia;

    @Column(name = "motivo_nota_credito", length = 500)
    private String motivoNotaCredito;

    @Column(name = "codigo_motivo", length = 2)
    private String codigoMotivo;

    @Column(name = "estado_sunat", length = 15)
    private String estadoSunat;

    @Column(name = "id_nota_credito_anulacion")
    private Long idNotaCreditoAnulacion;

    @Column(name = "fecha_anulacion_sunat")
    private LocalDateTime fechaAnulacionSunat;
}