package com.Inmobiliaria.demo.entity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.Inmobiliaria.demo.enums.TipoContrato;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Contrato")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contrato")
    private Integer idContrato;

    @ManyToOne
    @JoinColumn(name = "id_separacion", nullable = true)
    private Separacion separacion; // Puede ser null si es venta directa

    @ManyToOne
    @JoinColumn(name = "id_vendedor", nullable = true)
    private Vendedor vendedor; // 🔹 NUEVO: Vendedor que hizo el contrato

    @ManyToOne
    @JoinColumn(name = "idUsuario", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contrato", nullable = false)
    private TipoContrato tipoContrato; // 🔹 NUEVO: CONTADO o FINANCIADO

    @Temporal(TemporalType.DATE)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "fecha_contrato", nullable = false)
    private Date fechaContrato;

    @Column(name = "monto_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal montoTotal;

    @Column(name = "inicial", precision = 12, scale = 2)
    private BigDecimal inicial;

    @Column(name = "saldo", precision = 12, scale = 2)
    private BigDecimal saldo;

    @Column(name = "cantidad_letras")
    private Integer cantidadLetras;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    // Relaciones adicionales

    @OneToMany(mappedBy = "contrato")
    private List<LetraCambio> letrasCambio;

    @OneToMany(mappedBy = "contrato")
    private List<ContratoCliente> clientes;

    @OneToMany(mappedBy = "contrato")
    private List<ContratoLote> lotes;
}
