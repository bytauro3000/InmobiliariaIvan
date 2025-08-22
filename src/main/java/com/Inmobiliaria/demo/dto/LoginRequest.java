package com.Inmobiliaria.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    @Email
    private String correo;
    
    @NotBlank
    private String contrasena;
}