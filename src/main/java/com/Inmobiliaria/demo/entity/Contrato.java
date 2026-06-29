package com.Inmobiliaria.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.BatchSize;
import com.Inmobiliaria.demo.enums.TipoContrato;
import com.Inmobiliaria.demo.enums.EstadoContrato;
import com.Inmobiliaria.demo.enums.Moneda;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contrato")
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
    private Separacion separacion;

    @ManyToOne
    @JoinColumn(name = "id_vendedor", nullable = true)
    private Vendedor vendedor;

    @ManyToOne
    @JoinColumn(name = "idUsuario", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contrato", nullable = false)
    private TipoContrato tipoContrato = TipoContrato.CONTADO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_contrato", nullable = false)
    private EstadoContrato estadoContrato = EstadoContrato.ACTIVO;

    @Column(name = "fecha_contrato", nullable = false)
    private LocalDate fechaContrato;

    @Column(name = "monto_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal montoTotal;

    @Column(name = "inicial", precision = 12, scale = 2, nullable = true)
    private BigDecimal inicial = BigDecimal.ZERO;

    @Column(name = "saldo", precision = 12, scale = 2, nullable = true)
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(name = "cantidad_letras", nullable = true)
    private Integer cantidadLetras = 0;

    @Column(name = "observaciones", columnDefinition = "TEXT", nullable = true)
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(name = "moneda", nullable = false, length = 3)
    private Moneda moneda = Moneda.USD;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_comprobante_inicial")
    private Comprobante comprobanteInicial;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pago_inicial")
    private PagoInicial pagoInicial;

    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<LetraCambio> letrasCambio;

    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private Set<ContratoCliente> clientes;

    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private Set<ContratoLote> lotes;
}