package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.*;
import com.Inmobiliaria.demo.entity.PagoMora;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.PagoMoraRepository;
import com.Inmobiliaria.demo.service.MoraService;
import com.Inmobiliaria.demo.service.PagoLetraService;
import com.Inmobiliaria.demo.util.ComprobanteMoraPdf;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/moras")
@RequiredArgsConstructor
public class MoraController {

    private final MoraService          moraService;
    private final PagoMoraRepository   pagoMoraRepository;
    private final PagoLetraService     pagoLetraService;

    // ── CONSULTAS ─────────────────────────────────────────────────────────────

    @GetMapping("/calcular/{idLetra}")
    public ResponseEntity<CalculoMoraDTO> calcularMora(@PathVariable Integer idLetra) {
        return ResponseEntity.ok(moraService.calcularMora(idLetra));
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

    /**
     * Sugiere el siguiente número de comprobante disponible.
     * CORRECCIÓN: ahora llama a sugerirNumeroComprobante() que existe en la interfaz
     * y devuelve directamente SugerenciaNumeroComprobanteDTO.
     */
    @GetMapping("/sugerir-numero")
    public ResponseEntity<SugerenciaNumeroComprobanteDTO> sugerirNumeroComprobante(
            @RequestParam TipoComprobante tipoComprobante) {
        return ResponseEntity.ok(pagoLetraService.sugerirNumeroComprobante(tipoComprobante)); // ← CORREGIDO
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

    // ── ACCIONES ──────────────────────────────────────────────────────────────

    @PostMapping(value = "/pagar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PagoMoraResponseDTO> pagarMora(
            @RequestPart("pago") PagoMoraRequestDTO request,
            @RequestPart(value = "vouchers", required = false) List<MultipartFile> vouchers)
            throws java.io.IOException {
        return new ResponseEntity<>(moraService.pagarMora(request, vouchers), HttpStatus.CREATED);
    }

    @PatchMapping("/{idMora}/anular")
    public ResponseEntity<MoraResponseDTO> anularMora(
            @PathVariable Integer idMora,
            @RequestParam(required = false, defaultValue = "") String motivo) {
        return ResponseEntity.ok(moraService.anularMora(idMora, motivo));
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

            String nombreArchivo = "comprobante-mora-" + idPagoMora + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + nombreArchivo + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (NegocioException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}