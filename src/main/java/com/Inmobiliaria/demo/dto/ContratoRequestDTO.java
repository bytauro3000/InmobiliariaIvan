package com.Inmobiliaria.demo.dto;

import java.util.List;
import com.Inmobiliaria.demo.enums.Moneda;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContratoRequestDTO {
    @NotBlank
    private String fechaContrato;

    @NotBlank
    private String tipoContrato;

    @NotNull
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
    private PagoInicialRequestDTO pagoInicial;
}