package com.Inmobiliaria.demo.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Config pública del PORTAL DEL CLIENTE (ruta /api/public/**: sin login).
 * El frontend necesita el Google Client ID para inicializar Google Identity
 * Services; así la única fuente de verdad sigue siendo la variable de entorno
 * del backend (GOOGLE_CLIENT_ID) y el mismo build sirve a todas las marcas.
 */
@RestController
@RequestMapping("/api/public/portal")
public class PublicPortalConfigController {

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    @GetMapping("/config")
    public ResponseEntity<PortalConfigDTO> config() {
        PortalConfigDTO dto = new PortalConfigDTO(
                (googleClientId == null || googleClientId.isBlank()) ? null : googleClientId.trim());
        return ResponseEntity.ok(dto);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PortalConfigDTO {
        private String googleClientId;
    }
}
