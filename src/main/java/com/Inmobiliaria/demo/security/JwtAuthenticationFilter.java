package com.Inmobiliaria.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    // ✅ Se inyecta desde variable de entorno GATEWAY_SECRET — nunca hardcodeado
    @Value("${gateway.secret-key}")
    private String gatewaySecretKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Rutas públicas: pasan directo sin validar nada
        if (path.contains("/api/auth/login") || path.contains("/api/public/ping") || path.equals("/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ VALIDACIÓN: Solo el Gateway puede hablar con el monolito
        // Si la petición no trae el header secreto correcto → 403 inmediato
        String gatewayHeader = request.getHeader("X-Gateway-Secret");
        if (gatewayHeader == null || !gatewayHeader.equals(gatewaySecretKey)) {
            log.warn("Acceso directo bloqueado al monolito en ruta: {} | Header recibido: {}",
                    path, gatewayHeader != null ? "[PRESENTE PERO INCORRECTO]" : "[AUSENTE]");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso denegado: origen no autorizado");
            return;
        }

        // ✅ Si llegó aquí, la petición viene del Gateway → procesar normalmente
        String userEmail = request.getHeader("X-Auth-User");
        String userRole  = request.getHeader("X-Auth-Roles");

        log.debug("Ruta solicitada desde Gateway: {}", path);

        if (userEmail != null && !userEmail.isEmpty()) {
            String rolLimpio = (userRole != null) ? userRole.trim() : "ROLE_USER";

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userEmail, null, List.of(new SimpleGrantedAuthority(rolLimpio))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            log.debug("Autenticacion exitosa - Principal: {}, Rol: [{}]", userEmail, rolLimpio);
        } else {
            log.debug("Header X-Auth-User ausente en ruta protegida: {}", path);
        }

        filterChain.doFilter(request, response);
    }
}