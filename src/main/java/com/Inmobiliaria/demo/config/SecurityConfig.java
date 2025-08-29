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
        // Inyecta el filtro como un parámetro del método
        JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {
        http
            // Configura CORS para permitir peticiones desde el frontend de Angular
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Deshabilita la protección CSRF ya que se usa autenticación basada en tokens
            .csrf(AbstractHttpConfigurer::disable)
            // Configura la política de sesión sin estado (STATELESS)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
            		
            	// Permite acceso a la ruta de login sin autenticación
                .requestMatchers("/api/auth/login").permitAll()
             	// Permite que SECRETARIA acceda a todos los endpoints de clientes
                .requestMatchers("/api/clientes/**").hasRole("SECRETARIA")
             	// Permite que SECRETARIA acceda a todos los endpoints de distritos
        
                .requestMatchers("/api/distritos/**").hasAnyRole("SECRETARIA", "SOPORTE")

        
                .requestMatchers("/api/contratos/**").hasRole("SECRETARIA")
                
                .requestMatchers("/api/lotes/**").hasAnyRole("SOPORTE", "SECRETARIA")
                
                .requestMatchers("/api/separaciones/**").hasRole("SECRETARIA")
                
                // 🚨 Exportar Excel de Programas → SOPORTE y SECRETARIA
                .requestMatchers("/api/programas/reporte-excel").hasAnyRole("SOPORTE", "SECRETARIA")
                
                // Lote API: permite acceso solo al rol SOPORTE
                //.requestMatchers("/api/lotes/**").hasRole("SOPORTE")
             	// Programa API: permite acceso solo al rol SOPORTE

                .requestMatchers("/api/programas/**").permitAll()

                .requestMatchers("/api/programas/**").hasRole("SOPORTE")
                .requestMatchers("/api/distritos/**").hasRole("SOPORTE")

                
                // Cualquier otra petición debe estar autenticada
                .anyRequest().authenticated()
            )
            // Agrega el filtro que se inyectó en el método, antes del filtro de autenticación por defecto
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        
        return http.build();
    }
    
    // Bean para codificar las contraseñas con BCrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Bean para gestionar la autenticación de usuarios
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // Bean que configura las reglas de CORS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Permite las peticiones desde el origen de tu frontend de Angular
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        // Define los métodos HTTP permitidos
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Permite todos los encabezados
        configuration.setAllowedHeaders(List.of("*"));
        // Permite el envío de credenciales (cookies, encabezados de autenticación, etc.)
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplica la configuración a todas las rutas
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}