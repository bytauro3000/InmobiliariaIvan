package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.AnulacionRequestDTO;
import com.Inmobiliaria.demo.dto.PagoInicialResponseDTO;
import com.Inmobiliaria.demo.service.PagoInicialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/contratos")
@RequiredArgsConstructor
public class PagoInicialController {

    private final PagoInicialService pagoInicialService;

    // ── Lectura por contrato ──────────────────────────────────────────────────

    @GetMapping("/{idContrato}/pago-inicial")
    public ResponseEntity<PagoInicialResponseDTO> obtenerPorContrato(
            @PathVariable Integer idContrato) {
        return ResponseEntity.ok(pagoInicialService.obtenerPorContrato(idContrato));
    }

    // ── ADMIN: Listado general con filtros ────────────────────────────────────

    @GetMapping("/pagos-iniciales/todos")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR')")
    public ResponseEntity<List<PagoInicialResponseDTO>> listarTodos(
            @RequestParam(required = false) String numeroComprobante,
            @RequestParam(required = false) String manzana,
            @RequestParam(required = false) String numeroLote,
            @RequestParam(required = false) Integer idPrograma,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(
            pagoInicialService.listarTodos(numeroComprobante, manzana, numeroLote, idPrograma, desde, hasta));
    }

    // ── ADMIN: Anular pago inicial ────────────────────────────────────────────

    @PatchMapping("/{idContrato}/pago-inicial/anular")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR')")
    public ResponseEntity<PagoInicialResponseDTO> anularPagoInicial(
            @PathVariable Integer idContrato,
            @Valid @RequestBody AnulacionRequestDTO request,
            Authentication authentication) {
        return ResponseEntity.ok(
            pagoInicialService.anularPagoInicial(
                idContrato, request.getMotivo(), authentication.getName()));
    }

    // ── ADMIN: Eliminar físicamente ───────────────────────────────────────────

    @DeleteMapping("/{idContrato}/pago-inicial")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR')")
    public ResponseEntity<Void> eliminarPagoInicial(
            @PathVariable Integer idContrato) {
        pagoInicialService.eliminarPagoInicial(idContrato);
        return ResponseEntity.noContent().build();
    }
}