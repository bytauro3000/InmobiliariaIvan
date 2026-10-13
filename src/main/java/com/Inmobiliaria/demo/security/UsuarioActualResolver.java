package com.Inmobiliaria.demo.security;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Punto ÚNICO de resolución del usuario actual.
 *
 * Evita la ambigüedad que existe hoy en el código (Principal, header
 * X-Auth-User o SecurityContextHolder según el controller): el
 * JwtAuthenticationFilter ya puebla el SecurityContext con el email que
 * envía el gateway, por lo que SecurityContextHolder es la fuente canónica.
 *
 * Nunca lanza excepción: si no hay contexto (job/scheduler), devuelve
 * valores de sistema.
 */
@Component
public class UsuarioActualResolver {

    private static final String ANONIMO = "sistema";

    /** Email del usuario autenticado o "sistema" fuera de contexto HTTP. */
    public String email() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null
                || auth.getName().isBlank()) {
            return ANONIMO;
        }
        return auth.getName();
    }

    /** Primera autoridad del usuario (ej: ROLE_SECRETARIA) o null. */
    public String rol() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null || auth.getAuthorities().isEmpty()) {
            return null;
        }
        return auth.getAuthorities().iterator().next().getAuthority();
    }

    /** IP real del cliente (detrás del gateway se entrega en X-Forwarded-For). */
    public String ip() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
