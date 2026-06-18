package com.Inmobiliaria.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioRegistroDTO {
    @NotBlank
    private String nombres;

    @NotBlank
    private String apellidos;

    @NotBlank
    @Email
    private String correo;

    @NotBlank
    private String contrasena;

    private String telefono;
    private String direccion;
    private String dni;

    @NotNull
    private Integer idRol;

    private String estado;
}