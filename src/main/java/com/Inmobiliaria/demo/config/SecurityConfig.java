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

                    // ==== 1️⃣ Recursos públicos (Angular, login, letras) ====
                    .requestMatchers(
                            "/", "/index.html", "/favicon.ico",
                            "/static/**", "/img/**", "/media/**",
                            "/**/*.js", "/**/*.css",
                            "/**/*.woff2", "/**/*.woff",
                            "/**/*.ttf", "/**/*.svg",
                            "/**/*.png", "/**/*.jpg", "/**/*.jpeg",
                            "/**/*.map"
                    ).permitAll()

                    .requestMatchers("/api/auth/login").permitAll()
                    .requestMatchers("/api/letras/**").permitAll()

                    // ==== 2️⃣ SOPORTE ====
                    .requestMatchers("/api/dashboard/**").hasRole("SOPORTE")

                    // ==== 3️⃣ SOPORTE o SECRETARIA ====
                    .requestMatchers("/api/lotes/**").hasAnyRole("SOPORTE", "SECRETARIA")
                    .requestMatchers("/api/programas/**").hasAnyRole("SOPORTE", "SECRETARIA")
                    .requestMatchers("/api/programas/reporte-excel").hasAnyRole("SOPORTE", "SECRETARIA")
                    .requestMatchers("/api/distritos/**").hasAnyRole("SOPORTE", "SECRETARIA")
                    .requestMatchers("/api/separaciones/**").hasAnyRole("SOPORTE", "SECRETARIA")

                    // ==== 4️⃣ SOLO SECRETARIA ====
                    .requestMatchers("/api/clientes/**").hasRole("SECRETARIA")
                    .requestMatchers("/api/contratos/**").hasRole("SECRETARIA")
                    .requestMatchers("/api/vendedores/**").hasRole("SECRETARIA")

                    // ==== 5️⃣ Todas las demás rutas requieren autenticación ====
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