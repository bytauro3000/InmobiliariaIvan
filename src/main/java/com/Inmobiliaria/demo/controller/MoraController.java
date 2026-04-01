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

import java.util.List;

@RestController
@RequestMapping("/api/moras")
@RequiredArgsConstructor
public class MoraController {

    private final MoraService moraService;
    private final PagoMoraRepository pagoMoraRepository;
    private final PagoLetraService pagoLetraService;

    // ── CONSULTAS ─────────────────────────────────────────────────────────────

    /**
     * Calcula la mora para una letra vencida SIN guardarla.
     * GET /api/moras/calcular/{idLetra}
     */
    @GetMapping("/calcular/{idLetra}")
    public ResponseEntity<CalculoMoraDTO> calcularMora(@PathVariable Integer idLetra) {
        return ResponseEntity.ok(moraService.calcularMora(idLetra));
    }

    /**
     * Lista todas las moras (cualquier estado) de un contrato.
     * GET /api/moras/contrato/{idContrato}
     */
    @GetMapping("/contrato/{idContrato}")
    public ResponseEntity<List<MoraResponseDTO>> listarPorContrato(
            @PathVariable Integer idContrato) {
        return ResponseEntity.ok(moraService.listarPorContrato(idContrato));
    }

    /**
     * Lista solo las moras PENDIENTES de un contrato.
     * GET /api/moras/contrato/{idContrato}/pendientes
     */
    @GetMapping("/contrato/{idContrato}/pendientes")
    public ResponseEntity<List<MoraResponseDTO>> listarPendientesPorContrato(
            @PathVariable Integer idContrato) {
        return ResponseEntity.ok(moraService.listarPendientesPorContrato(idContrato));
    }

    /**
     * Lista todas las moras de una letra específica.
     * GET /api/moras/letra/{idLetra}
     */
    @GetMapping("/letra/{idLetra}")
    public ResponseEntity<List<MoraResponseDTO>> listarPorLetra(
            @PathVariable Integer idLetra) {
        return ResponseEntity.ok(moraService.listarPorLetra(idLetra));
    }

    /**
     * Obtiene una mora por su ID.
     * GET /api/moras/{idMora}
     */
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

    /**
     * Resumen de mora pendiente de un contrato (cantidad + total acumulado).
     * GET /api/moras/contrato/{idContrato}/resumen
     */
    @GetMapping("/contrato/{idContrato}/resumen")
    public ResponseEntity<MoraResumenContratoDTO> obtenerResumenPorContrato(
            @PathVariable Integer idContrato) {
        return ResponseEntity.ok(moraService.obtenerResumenPorContrato(idContrato));
    }

    // ── ACCIONES ──────────────────────────────────────────────────────────────

    /**
     * Registra el pago de una mora.
     * POST /api/moras/pagar
     */
    @PostMapping("/pagar")
    public ResponseEntity<PagoMoraResponseDTO> pagarMora(
            @RequestBody PagoMoraRequestDTO request) {
        return new ResponseEntity<>(moraService.pagarMora(request), HttpStatus.CREATED);
    }

    /**
     * Anula una mora (solo para correcciones administrativas).
     * PATCH /api/moras/{idMora}/anular
     */
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
            Authentication authentication) {   // Spring inyecta esto automáticamente
        try {
            PagoMora pagoMora = pagoMoraRepository.findById(idPagoMora)
                    .orElseThrow(() -> new NegocioException(
                            "Pago de mora no encontrado: " + idPagoMora));

          
            String rolUsuario = "SECRETARIA";
            if (authentication != null && authentication.getAuthorities() != null) {
                rolUsuario = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)   // "ROLE_SECRETARIA"
                        .map(r -> r.replace("ROLE_", ""))      // "SECRETARIA"
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