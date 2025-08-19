package com.Inmobiliaria.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private AuthenticationSuccessHandler successHandler;

    // Configuración de la cadena de seguridad
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Configuración de CORS (Cross-Origin Resource Sharing)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Deshabilitar CSRF para APIs RESTful (común en servicios REST)
            .csrf(csrf -> csrf.disable())

            // Configuración de rutas y autorización
            .authorizeHttpRequests(auth -> auth
                // Rutas públicas (login y recursos estáticos)
                .requestMatchers("/login", "/css/**", "/js/**", "/img/**").permitAll()
                .requestMatchers("/contrato/buscar-separaciones").permitAll()

                // Rutas de la API accesibles sin autenticación
                .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/**").permitAll()
                .requestMatchers(HttpMethod.PUT, "/api/**").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/**").permitAll()

                // Rutas protegidas por rol
                .requestMatchers("/secretaria/**").hasRole("SECRETARIA")
                .requestMatchers("/soporte/**").hasRole("SOPORTE")
                .requestMatchers("/administrador/**").hasRole("ADMINISTRADOR")

                // Cualquier otra ruta requiere autenticación
                .anyRequest().authenticated()
            )

            // Configuración de inicio de sesión con formulario
            .formLogin(form -> form
                .loginPage("/login")  // Página personalizada de login
                .loginProcessingUrl("/procesar-login")  // URL para el procesamiento del formulario de login
                .usernameParameter("correo")  // Parámetro del formulario para el nombre de usuario
                .passwordParameter("password")  // Parámetro del formulario para la contraseña
                .successHandler(successHandler)  // Redirección según el rol del usuario
                .permitAll()
            )

            // Configuración de cierre de sesión
            .logout(logout -> logout
                .logoutUrl("/logout")  // URL de logout
                .logoutSuccessUrl("/login?logout")  // Redirección al logout
                .permitAll()
            )

            // Manejo de excepciones para no redirigir al login en las APIs
            .exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest")  // Si la solicitud es AJAX, devuelve 401
                )
            );

        return http.build();
    }

    // Configuración CORS detallada
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));  // Permitir todos los orígenes (puedes especificar tus orígenes en producción)
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));  // Métodos HTTP permitidos
        configuration.setAllowedHeaders(List.of("*"));  // Permitir todos los encabezados
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);  // Aplicar a todas las rutas
        return source;
    }

    // Bean para el codificador de contraseñas
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();  // No se usa cifrado (solo para desarrollo)
    }

    // Bean para el AuthenticationManager (permite la autenticación de usuarios)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
