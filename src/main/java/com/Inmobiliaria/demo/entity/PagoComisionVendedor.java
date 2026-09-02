package com.Inmobiliaria.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pago de comisión a un vendedor: adelanto (al firmar contrato) o cuota mensual
 * (10% del valor de la letra, habilitado después de que el cliente paga 8 letras).
 */
@Entity
@Table(name = "pago_comision_vendedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagoComisionVendedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago_comision")
    private Integer idPagoComision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_comision", nullable = false)
    private ComisionVendedor comision;

    /** Letra que originó la cuota mensual (null para el adelanto). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_letra")
    private LetraCambio letra;

    /** ADELANTO o MENSUAL */
    @Column(name = "tipo", length = 15, nullable = false)
    private String tipo;

    @Column(name = "monto", precision = 12, scale = 2, nullable = false)
    private BigDecimal monto;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    /** Recibo de egresos generado (serie EG01-número). */
    @Column(name = "numero_egreso", length = 30)
    private String numeroEgreso;

    @Column(name = "observacion", length = 255)
    private String observacion;
}