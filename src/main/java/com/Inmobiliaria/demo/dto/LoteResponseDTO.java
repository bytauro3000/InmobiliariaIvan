package com.Inmobiliaria.demo.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteResponseDTO {
    private Integer idLote;
    private String manzana;
    private String numeroLote;
    private BigDecimal area;
    private BigDecimal largo1;
    private BigDecimal largo2;
    private BigDecimal ancho1;
    private BigDecimal ancho2;
    private BigDecimal precioM2;
    private String colindanteNorte;
    private String colindanteSur;
    private String colindanteEste;
    private String colindanteOeste;
    private String nombrePrograma;
    private Integer idPrograma;
    private String estado;
}