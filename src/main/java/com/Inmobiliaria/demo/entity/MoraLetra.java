package com.Inmobiliaria.demo.entity;

import com.Inmobiliaria.demo.enums.EstadoMora;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mora_letra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MoraLetra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mora")
    private Integer idMora;

    // Letra que originó la mora
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_letra", nullable = false)
    private LetraCambio letra;

    // Pago de letra con el que se registró esta mora (nullable).
    // Se asigna cuando el operador paga la letra vencida y genera la mora en el mismo acto.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pago_letra", nullable = true)
    private PagoLetras pagoLetra;

    // ── Detalle del cálculo ──────────────────────────────────────────────────

    // Días transcurridos entre la fecha de vencimiento y la fecha en que se registra el pago
    @Column(name = "dias_mora", nullable = false)
    private Integer diasMora;

    // Porcentaje aplicado — siempre 0.05 (5%), pero lo guardamos por si cambia en el futuro
    @Column(name = "porcentaje_aplicado", nullable = false, precision = 5, scale = 4)
    private BigDecimal porcentajeAplicado;

    // Resultado de: importe_letra * porcentaje (ej. 200 * 0.05 = 10)
    @Column(name = "monto_porcentaje", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoPorcentaje;

    // Resultado de: dias_mora * 1.00 (ej. 1 día = $1.00)
    @Column(name = "monto_diario", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoDiario;

    // Total: monto_porcentaje + monto_diario (ej. 10 + 1 = 11)
    @Column(name = "monto_mora_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoMoraTotal;

    // Fecha en que se calculó y registró esta mora
    @Column(name = "fecha_generacion", nullable = false)
    private LocalDate fechaGeneracion;

    // Fecha de vencimiento original de la letra (referencia histórica)
    @Column(name = "fecha_vencimiento_letra", nullable = false)
    private LocalDate fechaVencimientoLetra;

    // ── Estado ───────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_mora", nullable = false, length = 20)
    private EstadoMora estadoMora = EstadoMora.PENDIENTE;

    // ── Pagos de esta mora ────────────────────────────────────────────────────
    // Un cliente puede pagar la mora en cualquier momento (inmediatamente o después)

    @OneToMany(mappedBy = "mora", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PagoMora> pagos = new ArrayList<>();
}