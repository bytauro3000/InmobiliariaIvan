package com.Inmobiliaria.demo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class LecturaUnificadaDTO {
    private Integer idContrato;
    private String clienteNombre;
    private String manzana;
    private String lote;
    // 💡 BLOQUE LUZ
    private boolean inscritoLuz;
    private Double lecturaAntLuz;
    private Double lecturaActLuz;
    private Double consumoLuz;
    private BigDecimal importeLuz;
    // 💧 BLOQUE AGUA
    private boolean inscritoAgua;
    private Double lecturaAntAgua;
    private Double lecturaActAgua;
    private Double consumoAgua;
    private BigDecimal importeAgua;
}