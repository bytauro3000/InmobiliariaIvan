package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
//DTO para la respuesta del login
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
	//Contenedor para enviar el token JWT al cliente
    private String token;
}