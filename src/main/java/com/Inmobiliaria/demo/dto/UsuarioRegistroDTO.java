package com.Inmobiliaria.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioRegistroDTO {
    private String nombres;
    private String apellidos;
    private String correo;
    private String contrasena;
    private String telefono;
    private String direccion;
    private String dni;
    private Integer idRol; // 1 (Secretaria), 2 (Soporte), 3 (Administrador)
    private String estado; // "activo" o "inactivo"
}