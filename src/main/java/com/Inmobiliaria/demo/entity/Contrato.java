package com.Inmobiliaria.demo.entity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import com.Inmobiliaria.demo.enums.TipoContrato;
import com.Inmobiliaria.demo.enums.EstadoContrato;
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

    @Temporal(TemporalType.DATE)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "fecha_contrato", nullable = false)
    private Date fechaContrato;

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
    
    

    // 🟢 CORRECCIÓN: Cascada para Letras de Cambio
    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LetraCambio> letrasCambio;

    // 🟢 CORRECCIÓN: Cascada para la tabla intermedia contrato_cliente
    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContratoCliente> clientes;

    // 🟢 CORRECCIÓN: Cascada para la tabla intermedia contrato_lote
    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContratoLote> lotes;
    
    
}