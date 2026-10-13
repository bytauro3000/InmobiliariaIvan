package com.Inmobiliaria.demo.controller;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Inmobiliaria.demo.dto.AuditoriaResponseDTO;
import com.Inmobiliaria.demo.entity.Auditoria;
import com.Inmobiliaria.demo.repository.AuditoriaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Consulta de la bitácora de auditoría. Exclusivo del ADMINISTRADOR.
 */
@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AuditoriaController {

    private final AuditoriaRepository auditoriaRepository;

    @GetMapping
    public ResponseEntity<Page<AuditoriaResponseDTO>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) String usuario,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200));

        Specification<Auditoria> spec = (root, query, cb) -> cb.conjunction();
        if (desde != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fechaHora"), desde));
        }
        if (hasta != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("fechaHora"), hasta));
        }
        if (usuario != null && !usuario.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("usuarioEmail")), "%" + usuario.toLowerCase().trim() + "%"));
        }

        Page<Auditoria> pagina = auditoriaRepository.findAll(spec, pageable);
        return ResponseEntity.ok(pagina.map(AuditoriaResponseDTO::desde));
    }
}
