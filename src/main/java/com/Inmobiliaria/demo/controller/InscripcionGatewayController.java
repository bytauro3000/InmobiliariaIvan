package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.client.InscripcionClient;
import com.Inmobiliaria.demo.dto.InscripcionServicioDTO;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gateway/inscripciones")
public class InscripcionGatewayController {

    private final InscripcionClient inscripcionClient;

    public InscripcionGatewayController(InscripcionClient inscripcionClient) {
        this.inscripcionClient = inscripcionClient;
    }

    @PostMapping("/registrar")
    @PreAuthorize("hasAuthority('ROLE_SECRETARIA')")
    public ResponseEntity<?> registrarInscripcion(@RequestBody InscripcionServicioDTO dto) {
        try {
            InscripcionServicioDTO resultado = inscripcionClient.crearInscripcion(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (FeignException e) {
            // El microservicio devolvió un error controlado
            return ResponseEntity.status(e.status())
                    .body(e.contentUTF8());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error crítico de comunicación con Servicios Básicos.");
        }
    }

    @GetMapping("/contratos-activos")
    @PreAuthorize("hasAuthority('ROLE_SECRETARIA')")
    public ResponseEntity<List<Integer>> obtenerContratosPorServicio(@RequestParam String tipo) {
        try {
            return ResponseEntity.ok(inscripcionClient.obtenerContratosPorServicio(tipo));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}