package com.Inmobiliaria.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pago_inscripcion_comprobante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagoInscripcionComprobante extends PagoBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago_inscripcion_comprobante")
    private Integer idPagoInscripcionComprobante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contrato", nullable = false)
    private Contrato contrato;

    @Column(name = "id_inscripcion_servicio", nullable = false)
    private Integer idInscripcionServicio;

    @Column(name = "tipo_servicio", length = 10, nullable = false)
    private String tipoServicio;

    // Id del abono en el microservicio de servicios básicos (pago_inscripcion.idPagoInscripcion).
    // Se guarda al registrar el abono para poder anularlo ahí cuando se emite una NC.
    @Column(name = "id_pago_inscripcion_ms")
    private Long idPagoInscripcionMs;
}