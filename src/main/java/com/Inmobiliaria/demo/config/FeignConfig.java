package com.Inmobiliaria.demo.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    // El mismo secreto que usa el Gateway — el microservicio lo valida para
    // aceptar llamadas internas del monolito igual que acepta las del Gateway
    @Value("${gateway.secret-key}")
    private String gatewaySecretKey;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // Siempre inyectamos el secreto en llamadas Feign hacia el microservicio
                template.header("X-Gateway-Secret", gatewaySecretKey);

                // Propagamos el JWT del usuario si existe en el contexto actual
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    String authorizationHeader = request.getHeader("Authorization");
                    if (authorizationHeader != null) {
                        template.header("Authorization", authorizationHeader);
                    }
                }
            }
        };
    }
}