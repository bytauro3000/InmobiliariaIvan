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
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /** Headers de identidad que SOLO el gateway puede inyectar. Cualquier otro origen = sospechoso. */
    private static final Set<String> GATEWAY_ONLY_HEADERS = Set.of(
            "x-auth-user",
            "x-auth-roles",
            "x-auth-secret"
    );

    @Value("${gateway.secret-key}")
    private String gatewaySecretKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // ── 1. Rutas públicas (pasan sin validar secreto del gateway) ──────────
        if (esRutaPublica(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── 2. Validar X-Gateway-Secret ─────────────────────────────────────────
        String gatewayHeader = request.getHeader("X-Gateway-Secret");
        boolean secretoValido = gatewayHeader != null && gatewayHeader.equals(gatewaySecretKey);

        if (!secretoValido) {
            // Detectar intento de inyección de identidad sin pasar por el gateway
            List<String> headersSospechosos = detectarHeadersSospechosos(request);
            if (!headersSospechosos.isEmpty()) {
                log.warn("INTENTO DE INYECCIÓN bloqueado | IP: {} | Path: {} | Headers sospechosos: {}",
                        obtenerIpCliente(request), path, headersSospechosos);
            } else {
                log.warn("Acceso directo bloqueado al monolito | IP: {} | Path: {} | Secreto: {}",
                        obtenerIpCliente(request), path,
                        gatewayHeader != null ? "[PRESENTE PERO INCORRECTO]" : "[AUSENTE]");
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso denegado: origen no autorizado");
            return;
        }

        // ── 3. Petición legítima del gateway → leer identidad ───────────────────
        String userEmail = request.getHeader("X-Auth-User");
        String userRole  = request.getHeader("X-Auth-Roles");

        log.debug("Ruta desde Gateway: {} | Usuario: {} | Rol: {}", path, userEmail, userRole);

        if (userEmail != null && !userEmail.isEmpty()) {
            String rolLimpio = (userRole != null) ? userRole.trim() : "ROLE_USER";
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userEmail, null, List.of(new SimpleGrantedAuthority(rolLimpio))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        } else {
            log.debug("Header X-Auth-User ausente en ruta protegida: {}", path);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Determina si la ruta es pública y no requiere validar el secreto del gateway.
     * Coincide con la whitelist del SecurityConfig.
     */
    private boolean esRutaPublica(String path) {
        return path.contains("/api/auth/login")
                || path.contains("/api/auth/refresh")
                || path.contains("/api/auth/logout")
                || path.contains("/api/public/ping")
                || path.equals("/favicon.ico")
                || path.equals("/favicon.png")
                || path.matches(".*/api/pagos/\\d+/comprobante-pdf")
                || path.matches(".*/api/pagos/comprobante-multiple/[^/]+")
                || path.matches(".*/api/gateway/inscripciones/pago/\\d+/comprobante-pdf")
                || path.matches(".*/api/moras/pago/\\d+/comprobante-pdf")
                || path.matches(".*/api/contratos/\\d+/pago-inicial/comprobante-pdf")
                || path.equals("/");
    }

    /**
     * Devuelve la lista de headers X-Auth-* presentes en el request,
     * para registrar intentos de inyección de identidad.
     */
    private List<String> detectarHeadersSospechosos(HttpServletRequest request) {
        return Collections.list(request.getHeaderNames()).stream()
                .filter(GATEWAY_ONLY_HEADERS::contains)
                .toList();
    }

    /**
     * Devuelve la IP del cliente, considerando proxies (X-Forwarded-For).
     */
    private String obtenerIpCliente(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
