package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.SesionResumenDTO;
import com.Inmobiliaria.demo.service.SesionActivaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SesionActivaService sesionActivaService;

    @GetMapping("/sesiones")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SesionResumenDTO> obtenerSesiones() {
        return ResponseEntity.ok(sesionActivaService.obtenerResumen());
    }
}
