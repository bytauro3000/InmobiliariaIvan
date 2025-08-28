package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

//lo utlizare para cargar el contenedor de mi Contrato
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteProgramaResponseDTO {
    private Integer idLote;
    private String manzana;
    private String numeroLote;
    private BigDecimal area;

    private Integer idPrograma;
    private String nombrePrograma;
    private BigDecimal precioM2;
}
