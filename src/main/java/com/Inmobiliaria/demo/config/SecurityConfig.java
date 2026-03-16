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
                .requestMatchers(
                    "/api/auth/**",
                    "/api/public/**",
                    "/api/pagos/*/comprobante-pdf",
                    "/error"
                ).permitAll()
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
                    "/ws/**",
                    "/api/archivos/**",
                    "/api/pagos/**"
                ).hasAuthority("ROLE_SECRETARIA")
                .requestMatchers("/api/usuarios/**")
                .hasAnyAuthority("ROLE_SECRETARIA", "ROLE_ADMINISTRADOR")
                .anyRequest().authenticated()
            )
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