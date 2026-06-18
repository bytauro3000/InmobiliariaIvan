package com.Inmobiliaria.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class LoteRequestDTO {
    @NotBlank
    private String manzana;

    @NotBlank
    private String numeroLote;

    @NotNull
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

    @NotNull
    private Integer idPrograma;
}
