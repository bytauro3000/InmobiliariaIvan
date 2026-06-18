package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.enums.EstadoCivil;
import com.Inmobiliaria.demo.enums.Genero;
import com.Inmobiliaria.demo.enums.TipoCliente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClienteRequestDTO {
    @NotBlank
    private String nombre;

    private String apellidos;

    @NotNull
    private EstadoCivil estadoCivil;

    @NotNull
    private TipoCliente tipoCliente;

    private String numDoc;

    @NotBlank
    private String celular;

    private String telefono;

    @NotBlank
    private String direccion;

    private String email;

    @NotNull
    private Genero genero;

    private Integer idDistrito;

    private String nacionalidad;
}
