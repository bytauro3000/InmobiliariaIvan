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
	    
	    // 🟢 LOGS DE DEPURACIÓN
	    String path = request.getRequestURI();
	    String userEmail = request.getHeader("X-Auth-User");
	    String userRole = request.getHeader("X-Auth-Roles");
	    
	 // Ocultamos la ruta "/" para no ensuciar la consola
        if (!path.equals("/")) {
            System.out.println("\n====== INICIO DEBUG MONOLITO ======");
            System.out.println("1. Ruta solicitada: " + path);
            System.out.println("2. X-Auth-User recibido: " + userEmail);
            System.out.println("3. X-Auth-Roles recibido: " + userRole);
            
            if (userEmail != null && !userEmail.isEmpty()) {
                // .trim() elimina cualquier espacio en blanco invisible que cause el 403
                String rolLimpio = (userRole != null) ? userRole.trim() : "ROLE_USER";
                
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userEmail, null, List.of(new SimpleGrantedAuthority(rolLimpio))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
                
                System.out.println("4. ✅ Autenticación exitosa en Spring Security.");
                System.out.println("   -> Principal: " + userEmail);
                System.out.println("   -> Autoridad asignada: [" + rolLimpio + "]");
            } else {
                System.out.println("4. ❌ NO se autenticó. El header X-Auth-User llegó nulo o vacío.");
            }
            System.out.println("===================================\n");
        }
        
        filterChain.doFilter(request, response);
    }
}