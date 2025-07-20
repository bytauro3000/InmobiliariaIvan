package com.Inmobiliaria.demo.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration // Indica que esta clase proporciona configuración de Spring
@EnableWebSecurity // Habilita la configuración de seguridad web
public class SecurityConfig {

    // Manejador personalizado para redireccionar al usuario según su rol al iniciar sesión
    @Autowired
    private AuthenticationSuccessHandler successHandler;

    // Define la cadena de filtros de seguridad
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Desactiva la protección CSRF (útil en desarrollo o con clientes externos como Postman)
            .csrf(csrf -> csrf.disable())

            // Autorización de rutas según rol o acceso público
            .authorizeHttpRequests(auth -> auth
                // Estas rutas son públicas (no requieren autenticación)
                .requestMatchers("/login", "/css/**", "/js/**", "/img/**").permitAll()
                .requestMatchers("/contrato/buscar-separaciones").permitAll() 

                // Las siguientes rutas requieren que el usuario tenga un rol específico
                .requestMatchers("/secretaria/**").hasRole("SECRETARIA")
                .requestMatchers("/soporte/**").hasRole("SOPORTE")
                .requestMatchers("/administrador/**").hasRole("ADMINISTRADOR")
               

                // Cualquier otra ruta debe estar autenticada
                .anyRequest().authenticated()
            )

            // Configuración del login
            .formLogin(form -> form
                // Página personalizada de login
                .loginPage("/login")

                // Ruta que procesa el POST del formulario de login
                .loginProcessingUrl("/procesar-login")

                // Nombre del parámetro que se usará como "username" en el login
                .usernameParameter("correo")

                // Nombre del parámetro para la contraseña
                .passwordParameter("password")

                // Handler que define a dónde redirigir según el rol al iniciar sesión
                .successHandler(successHandler)

                // Permitir acceso público al login
                .permitAll()
            )

            // Configuración del logout
            .logout(logout -> logout
                // Ruta que ejecuta el cierre de sesión
                .logoutUrl("/logout")

                // Redirección al cerrar sesión correctamente
                .logoutSuccessUrl("/login?logout")

                // Permitir acceso público al logout
                .permitAll()
            );

        // Retorna el objeto configurado
        return http.build();
    }

    // Configuración del codificador de contraseñas
    @Bean
    public PasswordEncoder passwordEncoder() {
        // ⚠️ No se encripta la contraseña (solo para pruebas)
        return NoOpPasswordEncoder.getInstance();
    }

    // Bean para el AuthenticationManager (permite autenticación de usuarios)
    @Bean
    public AuthenticationManager authManager(
            org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration authConfig
    ) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}