package com.Inmobiliaria.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pago_letra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagoLetras extends PagoBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Integer idPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_letra", nullable = false)
    private LetraCambio letra;

 
    @Column(name = "es_pago_acuenta", nullable = false)
    private Boolean esPagoAcuenta = false;

   
    @Column(name = "descuento_aplicado", precision = 12, scale = 2)
    private BigDecimal descuentoAplicado = BigDecimal.ZERO;

    @Column(name = "es_letra_gratis", nullable = false)
    private Boolean esLetraGratis = false;

    @Column(name = "fecha_operacion")
    private LocalDate fechaOperacion;
}