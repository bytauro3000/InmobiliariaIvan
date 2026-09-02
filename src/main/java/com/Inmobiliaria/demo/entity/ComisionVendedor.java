package com.Inmobiliaria.demo.entity;

import com.Inmobiliaria.demo.enums.EstadoComision;
import com.Inmobiliaria.demo.enums.Moneda;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Comisión que gana un vendedor por un contrato (lote vendido).
 * El porcentaje se CONGELA al momento de crear el contrato (si el % del vendedor
 * cambia después, no afecta las comisiones ya creadas).
 */
@Entity
@Table(name = "comision_vendedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComisionVendedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comision")
    private Integer idComision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contrato", nullable = false)
    private Contrato contrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vendedor", nullable = false)
    private Vendedor vendedor;

    /** % de comisión congelado al crear el contrato (ej: 3.00 = 3%). */
    @Column(name = "porcentaje_comision", precision = 5, scale = 2, nullable = false)
    private BigDecimal porcentajeComision;

    @Column(name = "monto_total_contrato", precision = 12, scale = 2, nullable = false)
    private BigDecimal montoTotalContrato;

    /** Comisión total = % × montoTotal, redondeada SIEMPRE hacia abajo (floor). */
    @Column(name = "monto_comision_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal montoComisionTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "moneda", length = 10, nullable = false)
    private Moneda moneda;

    /** Adelanto ya pagado al vendedor (30% de la inicial, o adelanto del programa). */
    @Column(name = "monto_adelanto", precision = 12, scale = 2)
    private BigDecimal montoAdelanto;

    /** Monto que aún se debe pagar (comisión total − adelanto − pagos mensuales). */
    @Column(name = "saldo_pendiente", precision = 12, scale = 2, nullable = false)
    private BigDecimal saldoPendiente;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoComision estado = EstadoComision.PENDIENTE;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}