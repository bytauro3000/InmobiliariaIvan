package com.Inmobiliaria.demo.controller;

import java.util.List;
import java.util.Map;

import com.Inmobiliaria.demo.dto.SesionResumenDTO;
import com.Inmobiliaria.demo.entity.Cliente;
import com.Inmobiliaria.demo.enums.AccionAuditoria;
import com.Inmobiliaria.demo.repository.ClienteRepository;
import com.Inmobiliaria.demo.security.crypto.AesGcmConverter;
import com.Inmobiliaria.demo.service.AuditoriaService;
import com.Inmobiliaria.demo.service.SesionActivaService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SesionActivaService sesionActivaService;
    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoriaService;

    @PersistenceContext
    private EntityManager em;

    @GetMapping("/sesiones")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<SesionResumenDTO> obtenerSesiones() {
        return ResponseEntity.ok(sesionActivaService.obtenerResumen());
    }

    /**
     * FASE 2 – Migración one-off: cifra numDocumento/celular/telefono de todos
     * los clientes existentes con AES-256-GCM (IV determinístico, idempotente).
     * Ejecutar UNA vez tras desplegar, con ventana de mantenimiento.
     */
    @PostMapping("/migrar-cifrado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public ResponseEntity<Map<String, Object>> migrarCifrado() {
        List<Cliente> clientes = clienteRepository.findAll();
        int migrados = 0;
        for (Cliente c : clientes) {
            clienteRepository.actualizarCamposCifrados(
                    c.getIdCliente(),
                    AesGcmConverter.cifrar(c.getNumDoc()),
                    AesGcmConverter.cifrar(c.getCelular()),
                    AesGcmConverter.cifrar(c.getTelefono()));
            migrados++;
        }
        em.clear();

        auditoriaService.registrar(AccionAuditoria.MIGRAR, "Cliente", null,
                "Migración a cifrado AES-256-GCM: " + migrados + " clientes procesados");

        return ResponseEntity.ok(Map.of(
                "migrados", migrados,
                "mensaje", "Campos sensibles de cliente cifrados correctamente."));
    }
}
