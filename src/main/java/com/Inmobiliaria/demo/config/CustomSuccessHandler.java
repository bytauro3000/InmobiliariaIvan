package com.Inmobiliaria.demo.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component // ✅ Esto hace que Spring lo registre como un Bean
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        // Obtener el rol del usuario autenticado
        String rol = authentication.getAuthorities().iterator().next().getAuthority();

        // Redirigir según el rol
        switch (rol) {
            case "ROLE_SECRETARIA":
                response.sendRedirect("dashboard/secretaria");
                break;
            case "ROLE_SOPORTE":
                response.sendRedirect("dashboard/soporte");
                break;
            case "ROLE_ADMINISTRADOR":
                response.sendRedirect("dashboard/administrador");
                break;
            default:
                response.sendRedirect("/login?error");
                break;
        }
    }
}
