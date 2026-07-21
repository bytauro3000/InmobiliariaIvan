package com.Inmobiliaria.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioListadoDTO {
    private Integer id;
    private String nombres;
    private String apellidos;
    private String correo;
    private String telefono;
    private String dni;
    private String direccion;
    private String rol;
    private String estado;
    private Integer idDistrito;
    private String distritoNombre;
    private String provincia;
    private String departamento;
}