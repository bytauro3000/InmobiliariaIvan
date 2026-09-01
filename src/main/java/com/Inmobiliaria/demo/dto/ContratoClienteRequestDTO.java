package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.enums.TipoPropietario;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContratoClienteRequestDTO {
    @NotNull
    private Integer idCliente;

    @NotNull
    private TipoPropietario tipoPropietario;
}