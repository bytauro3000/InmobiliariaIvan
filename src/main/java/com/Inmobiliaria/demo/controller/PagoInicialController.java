package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.AnulacionRequestDTO;
import com.Inmobiliaria.demo.dto.PagoInicialResponseDTO;
import com.Inmobiliaria.demo.entity.PagoInicial;
import com.Inmobiliaria.demo.entity.Voucher;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.VoucherRepository;
import com.Inmobiliaria.demo.service.PagoInicialService;
import com.Inmobiliaria.demo.util.ComprobantePagoInicialPdf;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/contratos")
@RequiredArgsConstructor
public class PagoInicialController {

    private static final Logger log = LoggerFactory.getLogger(PagoInicialController.class);

    private final PagoInicialService pagoInicialService;
    private final VoucherRepository  voucherRepository;

    // ── Lectura por contrato ──────────────────────────────────────────────────

    @GetMapping("/{idContrato}/pago-inicial")
    public ResponseEntity<PagoInicialResponseDTO> obtenerPorContrato(
            @PathVariable Integer idContrato) {
        return ResponseEntity.ok(pagoInicialService.obtenerPorContrato(idContrato));
    }

    // ── Descargar PDF del comprobante de pago inicial ─────────────────────────

    @GetMapping("/{idContrato}/pago-inicial/comprobante-pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> descargarComprobantePagoInicial(
            @PathVariable Integer idContrato,
            Authentication authentication) {

        try {
            PagoInicial pago = pagoInicialService.obtenerEntidadPorContrato(idContrato);

            String rolUsuario = "SECRETARIA";
            if (authentication != null && authentication.getAuthorities() != null) {
                rolUsuario = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(r -> r.replace("ROLE_", ""))
                        .findFirst()
                        .orElse("SECRETARIA");
            }

            // Vouchers adjuntos (informativo interno, no van a SUNAT)
            List<Voucher> vouchers = voucherRepository
                    .findByTipoOrigenAndReferenciaId("PAGO_INICIAL", pago.getIdPagoInicial());

            byte[] pdf = ComprobantePagoInicialPdf.generar(pago, rolUsuario, vouchers);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"comprobante-inicial-" + idContrato + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (NegocioException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error generando comprobante PDF para contrato {}: ", idContrato, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error generando PDF: " + e.getClass().getSimpleName() + ": " + e.getMessage()).getBytes());
        }
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