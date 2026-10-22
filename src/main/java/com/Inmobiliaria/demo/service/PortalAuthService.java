package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.LoginResponse;

/**
 * Autenticación del PORTAL DEL CLIENTE con Google.
 * El frontend envía el ID token de Google (Identity Services) y el backend
 * lo verifica contra Google antes de emitir el JWT propio del portal.
 */
public interface PortalAuthService {

    /**
     * Verifica el credential de Google, busca al cliente por email y emite
     * el JWT del portal (ROLE_CLIENTE, 12 h).
     *
     * @throws com.Inmobiliaria.demo.exception.NegocioException con mensaje
     *         seguro para el usuario ("Correo no registrado", etc.).
     */
    LoginResponse autenticarConGoogle(String credential);
}
