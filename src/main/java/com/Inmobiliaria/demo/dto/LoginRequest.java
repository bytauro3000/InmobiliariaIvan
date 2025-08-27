package com.Inmobiliaria.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
// DTO para la petición de logout
@Data
public class LoginRequest {
	//Anotaciones para validar que el campo no esté vacío y sea un correo válido
    @NotBlank
    @Email
    private String correo;
    
    @NotBlank
    private String contrasena;
}