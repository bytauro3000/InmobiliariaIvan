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
	    
	    System.out.println("DEBUG GATEWAY -> Ruta: " + path);
	    System.out.println("DEBUG GATEWAY -> Usuario: " + userEmail);
	    System.out.println("DEBUG GATEWAY -> Rol: " + userRole);

	    if (userEmail != null) {
	        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
	            userEmail, null, List.of(new SimpleGrantedAuthority(userRole != null ? userRole : "ROLE_USER"))
	        );
	        SecurityContextHolder.getContext().setAuthentication(auth);
	    }
	    
	    filterChain.doFilter(request, response);
	}
}