package com.Inmobiliaria.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.Inmobiliaria.demo.enums.EstadoLote;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "lote")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lote")
    private Integer idLote;

    @Column(name = "manzana", nullable = false, length = 10)
    private String manzana;

    @Column(name = "numero_lote", nullable = false, length = 10)
    private String numeroLote;

    @Column(name = "area", nullable = false, precision = 10, scale = 2)
    private BigDecimal area;

    @Column(name = "largo1", precision = 5, scale = 2)
    private BigDecimal largo1;

    @Column(name = "largo2", precision = 5, scale = 2)
    private BigDecimal largo2;

    @Column(name = "ancho1", precision = 5, scale = 2)
    private BigDecimal ancho1;

    @Column(name = "ancho2", precision = 5, scale = 2)
    private BigDecimal ancho2;

    @Column(name = "precio_m2", precision = 10, scale = 2)
    private BigDecimal precioM2;

    @Column(name = "colindante_norte", length = 100)
    private String colindanteNorte;

    @Column(name = "colindante_sur", length = 100)
    private String colindanteSur;

    @Column(name = "colindante_este", length = 100)
    private String colindanteEste;

    @Column(name = "colindante_oeste", length = 100)
    private String colindanteOeste;
    
 // 1. MÉTODO PARA AUTOCALCULAR EL ÁREA (Llamado antes de persistir o actualizar)
    @PrePersist
    @PreUpdate
    public void ajustarDecimales() {
        if (this.area != null) {
            this.area = this.area.setScale(2, RoundingMode.HALF_UP);
        }
    }

    // 2. MÉTODO PARA EL PRECIO TOTAL REDONDEADO A ENTERO
    @Transient 
    public BigDecimal getPrecioListaTotal() {
        if (this.area != null && this.precioM2 != null) {
            // Usa el área digitada (ej. la del plano) para el precio
            BigDecimal total = this.area.multiply(this.precioM2);
            return total.setScale(0, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
    
    
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20)
    private EstadoLote estado = EstadoLote.Disponible;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_programa")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Programa programa;
}
