package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.*;
import com.Inmobiliaria.demo.entity.PagoMora;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.PagoMoraRepository;
import com.Inmobiliaria.demo.service.MoraService;
import com.Inmobiliaria.demo.service.PagoLetraService;
import com.Inmobiliaria.demo.util.ComprobanteMoraPdf;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/moras")
@RequiredArgsConstructor
public class MoraController {

    private final MoraService          moraService;
    private final PagoMoraRepository   pagoMoraRepository;
    private final PagoLetraService     pagoLetraService;

    // ── Consultas ─────────────────────────────────────────────────────────────

    @GetMapping("/calcular/{idLetra}")
    public ResponseEntity<CalculoMoraDTO> calcularMora(
            @PathVariable Integer idLetra,
            @RequestParam(required = false) LocalDate fecha) {
        CalculoMoraDTO resultado = (fecha != null)
            ? moraService.calcularMora(idLetra, fecha)
            : moraService.calcularMora(idLetra);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/contrato/{idContrato}")
    public ResponseEntity<List<MoraResponseDTO>> listarPorContrato(
            @PathVariable Integer idContrato) {
        return ResponseEntity.ok(moraService.listarPorContrato(idContrato));
    }

    @GetMapping("/contrato/{idContrato}/pendientes")
    public ResponseEntity<List<MoraResponseDTO>> listarPendientesPorContrato(
            @PathVariable Integer idContrato) {
        return ResponseEntity.ok(moraService.listarPendientesPorContrato(idContrato));
    }

    @GetMapping("/letra/{idLetra}")
    public ResponseEntity<List<MoraResponseDTO>> listarPorLetra(
            @PathVariable Integer idLetra) {
        return ResponseEntity.ok(moraService.listarPorLetra(idLetra));
    }

    @GetMapping("/{idMora}")
    public ResponseEntity<MoraResponseDTO> obtenerPorId(@PathVariable Integer idMora) {
        return ResponseEntity.ok(moraService.obtenerPorId(idMora));
    }

    @GetMapping("/sugerir-numero")
    public ResponseEntity<SugerenciaNumeroComprobanteDTO> sugerirNumeroComprobante(
            @RequestParam TipoComprobante tipoComprobante) {
        return ResponseEntity.ok(pagoLetraService.sugerirNumeroComprobante(tipoComprobante));
    }

    @PostMapping("/crear-pendiente/{idLetra}")
    public ResponseEntity<MoraResponseDTO> crearMoraPendiente(
            @PathVariable Integer idLetra) {
        return new ResponseEntity<>(moraService.crearMoraPendiente(idLetra), HttpStatus.CREATED);
    }

    @GetMapping("/contrato/{idContrato}/resumen")
    public ResponseEntity<MoraResumenContratoDTO> obtenerResumenPorContrato(
            @PathVariable Integer idContrato) {
        return ResponseEntity.ok(moraService.obtenerResumenPorContrato(idContrato));
    }

    // ── Acciones ──────────────────────────────────────────────────────────────

    @PostMapping(value = "/pagar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PagoMoraResponseDTO> pagarMora(
            @RequestPart("pago") PagoMoraRequestDTO request,
            @RequestPart(value = "vouchers", required = false) List<MultipartFile> vouchers)
            throws java.io.IOException {
        return new ResponseEntity<>(moraService.pagarMora(request, vouchers), HttpStatus.CREATED);
    }

    @GetMapping("/pago/{idPagoMora}/comprobante-pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> descargarComprobanteMora(
            @PathVariable Integer idPagoMora,
            Authentication authentication) {
        try {
            PagoMora pagoMora = pagoMoraRepository.findById(idPagoMora)
                    .orElseThrow(() -> new NegocioException(
                            "Pago de mora no encontrado: " + idPagoMora));

            String rolUsuario = "SECRETARIA";
            if (authentication != null && authentication.getAuthorities() != null) {
                rolUsuario = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(r -> r.replace("ROLE_", ""))
                        .findFirst()
                        .orElse("SECRETARIA");
            }

            byte[] pdf = ComprobanteMoraPdf.generar(pagoMora, rolUsuario);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"comprobante-mora-" + idPagoMora + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (NegocioException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── ADMIN: Anular mora ────────────────────────────────────────────────────

    @PatchMapping("/{idMora}/anular")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR', 'ROLE_SOPORTE')")
    public ResponseEntity<MoraResponseDTO> anularMora(
            @PathVariable Integer idMora,
            @Valid @RequestBody AnulacionRequestDTO request,
            Authentication authentication) {
        return ResponseEntity.ok(
            moraService.anularMora(idMora, request.getMotivo(), authentication.getName()));
    }

    // ── ADMIN: Anular pago de mora ────────────────────────────────────────────

    @PatchMapping("/pago/{idPagoMora}/anular")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR', 'ROLE_SOPORTE')")
    public ResponseEntity<PagoMoraResponseDTO> anularPagoMora(
            @PathVariable Integer idPagoMora,
            @Valid @RequestBody AnulacionRequestDTO request,
            Authentication authentication) {
        return ResponseEntity.ok(
            moraService.anularPagoMora(idPagoMora, request.getMotivo(), authentication.getName()));
    }

    // ── ADMIN: Eliminar pago de mora físicamente ──────────────────────────────

    @DeleteMapping("/pago/{idPagoMora}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMINISTRADOR', 'ROLE_SOPORTE')")
    public ResponseEntity<Void> eliminarPagoMora(@PathVariable Integer idPagoMora) {
        moraService.eliminarPagoMora(idPagoMora);
        return ResponseEntity.noContent().build();
    }

    // ── ADMIN: Listado general de pagos de mora con filtros ───────────────────

    @GetMapping("/pagos/todos")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR')")
    public ResponseEntity<List<PagoMoraResponseDTO>> listarPagosTodos(
            @RequestParam(required = false) String numeroComprobante,
            @RequestParam(required = false) String manzana,
            @RequestParam(required = false) String numeroLote,
            @RequestParam(required = false) Integer idPrograma,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(
            moraService.listarPagosTodos(numeroComprobante, manzana, numeroLote, idPrograma, desde, hasta));
    }
}