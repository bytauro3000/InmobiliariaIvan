package com.Inmobiliaria.demo.dto;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor; 
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContratoRequestDTO {
    private String fechaContrato;               // formato 'yyyy-MM-dd'
    private String tipoContrato;                 // 'CONTADO' o 'FINANCIADO'
    private Double montoTotal;
    private Double inicial;
    private Double saldo;
    private Integer cantidadLetras;
    private String observaciones;
    private Integer idVendedor;
    private Integer idUsuario;                   // O se obtiene del Principal en controller
    private Integer idSeparacion;
    private List<Integer> idClientes;
    private List<Integer> idLotes;
}
