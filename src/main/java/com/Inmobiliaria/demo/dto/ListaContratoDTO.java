package com.Inmobiliaria.demo.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListaContratoDTO {

    private Integer idContrato;
    private String nombreCliente1;
    private String nombreCliente2;
    private String manzana;          // MZ del lote 1 (o "A" si hay 2 en distintas MZ)
    private String manzana2;         // MZ del lote 2 si aplica (null si 1 lote o misma MZ)
    private String numeroLote;       // LT del lote 1 (o "05" si hay 2 lotes)
    private String numeroLote2;      // LT del lote 2 si aplica (null si 1 lote)
    private BigDecimal area;         // Area del lote 1
    private BigDecimal area2;        // Area del lote 2 si aplica (null si 1 lote)
    private BigDecimal areaTotal;    // Suma de areas
    private String celular1;         // Celular del primer cliente
    private String celular2;         // Celular del segundo cliente (null si 1 cliente)
    private String nombrePrograma;   // Nombre del programa
    private Integer idPrograma;      // ID del programa para agrupar
    private String estadoContrato;   // Estado del contrato
}
