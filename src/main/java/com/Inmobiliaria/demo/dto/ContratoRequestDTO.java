package com.Inmobiliaria.demo.dto;

import java.util.List;
import com.Inmobiliaria.demo.enums.Moneda;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContratoRequestDTO {
    private String fechaContrato;
    private String tipoContrato;
    private Double montoTotal;
    private Double inicial;
    private Double saldo;
    private Integer cantidadLetras;
    private String observaciones;
    private Integer idVendedor;
    private Integer idUsuario;
    private Integer idSeparacion;
    private List<Integer> idClientes;
    private List<Integer> idLotes;
    private Moneda moneda;
}