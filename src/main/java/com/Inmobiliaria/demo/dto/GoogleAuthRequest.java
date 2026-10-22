package com.Inmobiliaria.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Petición de login del portal del cliente con Google.
 * "credential" es el ID token (JWT) que emite Google Identity Services
 * en el frontend y que el backend verifica contra Google (tokeninfo).
 */
@Data
public class GoogleAuthRequest {

    @NotBlank
    private String credential;
}
