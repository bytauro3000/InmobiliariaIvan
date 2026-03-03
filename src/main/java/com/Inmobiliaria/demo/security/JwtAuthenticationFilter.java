package com.Inmobiliaria.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	@Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        //EXCEPCIONES: Rutas que NO necesitan validar headers del Gateway
        //Si es login o el ping público, pasamos directamente al siguiente filtro (al Controller)
        if (path.contains("/api/auth/login") || path.contains("/api/public/ping") || path.equals("/")) {
            filterChain.doFilter(request, response);
            return; // Salimos del método para no ejecutar los logs de abajo
        }

        // 2. 🟢 LOGS DE DEPURACIÓN PARA RUTAS PROTEGIDAS
        String userEmail = request.getHeader("X-Auth-User");
        String userRole = request.getHeader("X-Auth-Roles");
        
        System.out.println("\n====== INICIO DEBUG MONOLITO ======");
        System.out.println("1. Ruta solicitada: " + path);
        System.out.println("2. X-Auth-User recibido: " + userEmail);
        System.out.println("3. X-Auth-Roles recibido: " + userRole);
        
        if (userEmail != null && !userEmail.isEmpty()) {
            String rolLimpio = (userRole != null) ? userRole.trim() : "ROLE_USER";
            
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userEmail, null, List.of(new SimpleGrantedAuthority(rolLimpio))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            System.out.println("4. ✅ Autenticación exitosa en Spring Security.");
            System.out.println("   -> Principal: " + userEmail);
            System.out.println("   -> Autoridad asignada: [" + rolLimpio + "]");
        } else {
            // Este log ahora solo saldrá si intentas entrar a una ruta protegida (ej: /api/clientes) sin estar logueado
            System.out.println("4. ❌ ACCESO DENEGADO: Falta header X-Auth-User en ruta protegida.");
        }
        System.out.println("===================================\n");
        
        filterChain.doFilter(request, response);
    }
}