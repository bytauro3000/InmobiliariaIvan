package com.Inmobiliaria.demo.config;

import com.Inmobiliaria.demo.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
            .cors(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
            	.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                // 1. RUTAS PÚBLICAS: Libre acceso (Login y endpoints marcados como public)
                .requestMatchers("/api/auth/**", "/api/public/**","/error").permitAll()

                // 2. RUTAS DE SECRETARIA: Solo usuarios con ROLE_SECRETARIA pueden acceder
                // Listamos todas las rutas que mencionaste
                .requestMatchers(
                    "/api/distritos/**", 
                    "/api/separaciones/**", 
                    "/api/clientes/**", 
                    "/api/contratos/**", 
                    "/api/vendedores/**", 
                    "/api/lotes/**", 
                    "/api/programas/**",
                    "/api/parceleros/**", 
                    "/api/letras/**", 
                    "/api/dashboard/**", 
                    "/api/gateway/inscripciones/**",
                    "/api/gateway/recibos/**",
                    "/api/mensajes/**",
                    "/chat/**",
                    "/api/archivos/**", 
                    "/ws/**",
                    "/api/pagos/**"
                   
                ).hasAuthority("ROLE_SECRETARIA")
                
             // 3. RUTAS DE ADMINISTRADOR: Solo gestión de usuarios
                .requestMatchers("/api/usuarios/**")
                .hasAnyAuthority("ROLE_SECRETARIA", "ROLE_ADMINISTRADOR")

                // 4. CUALQUIER OTRA PETICIÓN: Debe estar al menos autenticada
                .anyRequest().authenticated()

            )
            // Agregamos el filtro que lee las cabeceras X-Auth-User y X-Auth-Roles del Gateway
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}