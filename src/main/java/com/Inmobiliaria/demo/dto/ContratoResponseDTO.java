package com.Inmobiliaria.demo.dto;

import java.math.BigDecimal;
import java.util.Date;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.Inmobiliaria.demo.enums.TipoContrato;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContratoResponseDTO {
    private Integer idContrato;
    private Date fechaContrato;
    private TipoContrato tipoContrato;
    private BigDecimal montoTotal;
    private BigDecimal inicial;
    private BigDecimal saldo;
    private Integer cantidadLetras;
    private String observaciones;
    private List<ClienteResponseDTO> clientes; // <-- Lista de DTOs de clientes
}