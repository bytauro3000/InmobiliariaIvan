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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
            	// RUTAS PARA EL ACCEDSO A ANGULAR UNIFICADO
                .requestMatchers("/", "/index.html", "/favicon.ico", "/static/**", "/img/**", "/media/**", "/**/*.js", "/**/*.css", "/**/*.woff2", "/**/*.woff", "/**/*.ttf", "/**/*.svg", "/**/*.png", "/**/*.jpg", "/**/*.jpeg", "/**/*.map").permitAll()
            	// 1. Permite acceso a la ruta de login sin autenticación (la más específica)
                .requestMatchers("/api/auth/login").permitAll()
        
                // 2. Reglas para el rol SOPORTE
                .requestMatchers("/api/dashboard/**").hasRole("SOPORTE")
               
                // 3. Reglas para el rol SECRETARIA
                .requestMatchers("/api/clientes/**").hasRole("SECRETARIA")
                .requestMatchers("/api/contratos/**").hasRole("SECRETARIA")
                .requestMatchers("/api/vendedores/**").hasRole("SECRETARIA")
                .requestMatchers("/api/lotes/**").hasRole("SECRETARIA")
                .requestMatchers("/api/programas/**").hasAnyRole("SECRETARIA")
                .requestMatchers("/api/letras/**").hasAnyRole("SECRETARIA")
                
                
                
                // 4. Reglas compartidas entre SECRETARIA y SOPORTE
                .requestMatchers("/api/distritos/**").hasAnyRole("SECRETARIA", "SOPORTE")
                .requestMatchers("/api/programas/reporte-excel").hasAnyRole("SOPORTE", "SECRETARIA")
                .requestMatchers("/api/programas/**").permitAll()
                .requestMatchers("/api/programas/**").hasRole("SOPORTE")
                .requestMatchers("/api/distritos/**").hasRole("SOPORTE")
                .requestMatchers("/api/vendedores**").hasRole("SOPORTE")
                
                // Cualquier otra petición debe estar autenticada

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}