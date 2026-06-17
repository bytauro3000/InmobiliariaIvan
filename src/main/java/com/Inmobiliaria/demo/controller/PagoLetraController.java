package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.AnulacionRequestDTO;
import com.Inmobiliaria.demo.dto.PagoLetraRequestDTO;
import com.Inmobiliaria.demo.dto.PagoLetraResponseDTO;
import com.Inmobiliaria.demo.dto.PagosMultiplesRequestDTO;
import com.Inmobiliaria.demo.dto.SugerenciaNumeroComprobanteDTO;
import com.Inmobiliaria.demo.entity.PagoLetras;
import com.Inmobiliaria.demo.entity.Voucher;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.PagoLetraRepository;
import com.Inmobiliaria.demo.repository.UsuarioRepository;
import com.Inmobiliaria.demo.repository.VoucherRepository;
import com.Inmobiliaria.demo.service.PagoLetraService;
import com.Inmobiliaria.demo.util.ComprobantePagoLetraPdf;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoLetraController {

    private final PagoLetraService    pagoLetraService;
    private final PagoLetraRepository pagoLetraRepository;
    private final UsuarioRepository   usuarioRepository;
    private final VoucherRepository   voucherRepository;

    // ── Lectura básica ────────────────────────────────────────────────────────

    @GetMapping("/contrato/{idContrato}")
    public ResponseEntity<List<PagoLetraResponseDTO>> listarPorContrato(
            @PathVariable Integer idContrato) {
        return ResponseEntity.ok(pagoLetraService.listarPorContrato(idContrato));
    }

    @GetMapping("/letra/{idLetra}")
    public ResponseEntity<List<PagoLetraResponseDTO>> listarPorLetra(
            @PathVariable Integer idLetra) {
        return ResponseEntity.ok(pagoLetraService.listarPorLetra(idLetra));
    }

    @GetMapping("/{idPago}")
    public ResponseEntity<PagoLetraResponseDTO> obtenerPorId(
            @PathVariable Integer idPago) {
        return ResponseEntity.ok(pagoLetraService.obtenerPorId(idPago));
    }

    // ── Registro ──────────────────────────────────────────────────────────────

    @PostMapping(value = "/registrar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PagoLetraResponseDTO> registrarPago(
            @RequestPart("pago") PagoLetraRequestDTO request,
            @RequestPart(value = "vouchers", required = false) List<MultipartFile> vouchers)
            throws IOException {
        PagoLetraResponseDTO response = pagoLetraService.registrarPago(request, vouchers);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping(value = "/registrar-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> registrarPagosMultiples(
            @RequestPart("pagos") PagosMultiplesRequestDTO request,
            @RequestPart(value = "vouchers", required = false) List<MultipartFile> vouchers)
            throws IOException {
        Map<String, Object> resultado = pagoLetraService.registrarPagosMultiples(request, vouchers);
        return new ResponseEntity<>(resultado, HttpStatus.CREATED);
    }

    @PutMapping(value = "/actualizar/{idPago}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PagoLetraResponseDTO> actualizarPago(
            @PathVariable Integer idPago,
            @RequestPart("pago") PagoLetraRequestDTO request,
            @RequestPart(value = "vouchers", required = false) List<MultipartFile> vouchers)
            throws IOException {
        PagoLetraResponseDTO response = pagoLetraService.actualizarPago(idPago, request, vouchers);
        return ResponseEntity.ok(response);
    }

    // ── Consultas de apoyo ────────────────────────────────────────────────────

    @GetMapping("/letra/{idLetra}/saldo")
    public ResponseEntity<Map<String, Object>> consultarSaldoPendiente(
            @PathVariable Integer idLetra) {
        java.math.BigDecimal saldo = pagoLetraService.consultarSaldoPendiente(idLetra);
        return ResponseEntity.ok(Map.of(
            "idLetra",        idLetra,
            "saldoPendiente", saldo
        ));
    }

    @GetMapping("/sugerir-numero")
    public ResponseEntity<SugerenciaNumeroComprobanteDTO> sugerirNumeroComprobante(
            @RequestParam TipoComprobante tipoComprobante) {
        return ResponseEntity.ok(pagoLetraService.sugerirNumeroComprobante(tipoComprobante));
    }

    // ── Comprobantes PDF ──────────────────────────────────────────────────────

    @GetMapping("/{idPago}/comprobante-pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> descargarComprobante(@PathVariable Integer idPago) {
        PagoLetras pago = pagoLetraRepository.findById(idPago)
                .orElseThrow(() -> new NegocioException("Pago no encontrado con ID: " + idPago));

        String rolUsuario = "SECRETARIA";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            var usuarioOpt = usuarioRepository.findByCorreo(auth.getName());
            if (usuarioOpt.isPresent()) {
                rolUsuario = usuarioOpt.get().getRol().getRolUsuario();
            }
        }

        // Obtener vouchers adjuntos al pago
        List<Voucher> vouchers = voucherRepository
                .findByTipoOrigenAndReferenciaId("PAGO_LETRA", idPago);

        byte[] pdf = ComprobantePagoLetraPdf.generar(pago, rolUsuario, vouchers);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=comprobante-pago-" + idPago + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/comprobante-multiple/{numeroComprobante}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> descargarComprobanteMultiple(
            @PathVariable String numeroComprobante) {

        List<PagoLetras> pagos =
                pagoLetraRepository.findByComprobanteNumeroCompleto(numeroComprobante);

        if (pagos == null || pagos.isEmpty()) {
            throw new NegocioException(
                    "No se encontraron pagos con el comprobante: " + numeroComprobante);
        }

        String rolUsuarioMultiple = "SECRETARIA";
        Authentication authMultiple = SecurityContextHolder.getContext().getAuthentication();
        if (authMultiple != null && authMultiple.getName() != null) {
            var usuarioOpt = usuarioRepository.findByCorreo(authMultiple.getName());
            if (usuarioOpt.isPresent()) {
                rolUsuarioMultiple = usuarioOpt.get().getRol().getRolUsuario();
            }
        }

        // Obtener todos los vouchers de todos los pagos del comprobante.
        // OJO: al registrar un pago múltiple, el mismo voucher se guarda una vez
        // por cada letra del lote (mismo url, distinto referenciaId) para que cada
        // pago individual pueda mostrarlo. Si no deduplicamos aquí por url, el
        // comprobante combinado terminaría mostrando la misma imagen N veces
        // (una por cada letra pagada).
        List<Voucher> vouchers = new ArrayList<>();
        java.util.Set<String> urlsVistas = new java.util.HashSet<>();
        for (PagoLetras p : pagos) {
            for (Voucher v : voucherRepository
                    .findByTipoOrigenAndReferenciaId("PAGO_LETRA", p.getIdPago())) {
                if (urlsVistas.add(v.getUrl())) {
                    vouchers.add(v);
                }
            }
        }

        byte[] pdf = ComprobantePagoLetraPdf.generarMultiple(pagos, rolUsuarioMultiple, vouchers);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=comprobante-multiple-" + numeroComprobante + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ── ADMIN: Anular ─────────────────────────────────────────────────────────

    @PatchMapping("/{idPago}/anular")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR')")
    public ResponseEntity<PagoLetraResponseDTO> anularPago(
            @PathVariable Integer idPago,
            @Valid @RequestBody AnulacionRequestDTO request,
            Authentication authentication) {
        String anuladoPor = authentication.getName();
        return ResponseEntity.ok(pagoLetraService.anularPago(idPago, request.getMotivo(), anuladoPor));
    }

    // ── ADMIN: Eliminar físicamente ───────────────────────────────────────────

    @DeleteMapping("/{idPago}")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR')")
    public ResponseEntity<Void> eliminarPago(
            @PathVariable Integer idPago) throws IOException {
        pagoLetraService.eliminarPago(idPago);
        return ResponseEntity.noContent().build();
    }

    // ── ADMIN: Listado general con filtros ────────────────────────────────────

    @GetMapping("/todos")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR')")
    public ResponseEntity<List<PagoLetraResponseDTO>> listarTodos(
            @RequestParam(required = false) String numeroComprobante,
            @RequestParam(required = false) String manzana,
            @RequestParam(required = false) String numeroLote,
            @RequestParam(required = false) Integer idPrograma,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(
            pagoLetraService.listarTodos(numeroComprobante, manzana, numeroLote, idPrograma, desde, hasta));
    }
}