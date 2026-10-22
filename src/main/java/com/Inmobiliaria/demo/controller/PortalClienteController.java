package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.PortalContratoDTO;
import com.Inmobiliaria.demo.dto.PortalDatosDTO;
import com.Inmobiliaria.demo.dto.PortalResumenDTO;
import com.Inmobiliaria.demo.service.PortalClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API del PORTAL DEL CLIENTE (solo lectura). Protegida con ROLE_CLIENTE en
 * SecurityConfig (/api/portal/**). La identidad se resuelve del token JWT
 * (email inyectado por el gateway); no acepta ids por parámetro.
 */
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalClienteController {

    private final PortalClienteService portalClienteService;

    @GetMapping("/mis-datos")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
    public ResponseEntity<PortalDatosDTO> misDatos() {
        return ResponseEntity.ok(portalClienteService.obtenerMisDatos());
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
    public ResponseEntity<PortalResumenDTO> resumen() {
        return ResponseEntity.ok(portalClienteService.obtenerResumen());
    }

    @GetMapping("/letras")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
    public ResponseEntity<List<PortalContratoDTO>> letras() {
        return ResponseEntity.ok(portalClienteService.obtenerLetras());
    }
}
