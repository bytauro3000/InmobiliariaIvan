package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.PagoLetraRequestDTO;
import com.Inmobiliaria.demo.dto.PagoLetraResponseDTO;
import com.Inmobiliaria.demo.dto.PagosMultiplesRequestDTO;
import com.Inmobiliaria.demo.dto.SugerenciaNumeroComprobanteDTO;
import com.Inmobiliaria.demo.entity.PagoLetras;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.PagoLetraRepository;
import com.Inmobiliaria.demo.repository.UsuarioRepository;
import com.Inmobiliaria.demo.service.PagoLetraService;
import com.Inmobiliaria.demo.util.ComprobantePagoLetraPdf;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
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

    @PostMapping(value = "/registrar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PagoLetraResponseDTO> registrarPago(
            @RequestPart("pago") PagoLetraRequestDTO request,
            @RequestPart(value = "vouchers", required = false) List<MultipartFile> vouchers)
            throws IOException {
        PagoLetraResponseDTO response = pagoLetraService.registrarPago(request, vouchers);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping(value = "/registrar-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<PagoLetraResponseDTO>> registrarPagosMultiples(
            @RequestPart("pagos") PagosMultiplesRequestDTO request,
            @RequestPart(value = "vouchers", required = false) List<MultipartFile> vouchers)
            throws IOException {
        List<PagoLetraResponseDTO> responses = pagoLetraService.registrarPagosMultiples(request, vouchers);
        return new ResponseEntity<>(responses, HttpStatus.CREATED);
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

    @DeleteMapping("/eliminar/{idPago}")
    public ResponseEntity<Void> eliminarPago(@PathVariable Integer idPago) throws IOException {
        pagoLetraService.eliminarPago(idPago);
        return ResponseEntity.noContent().build();
    }

    /**
     * Devuelve el siguiente número de comprobante disponible.
     * CORRECCIÓN: ahora llama a sugerirNumeroComprobante() que existe en la interfaz.
     */
    @GetMapping("/sugerir-numero")
    public ResponseEntity<SugerenciaNumeroComprobanteDTO> sugerirNumeroComprobante(
            @RequestParam TipoComprobante tipoComprobante) {
        SugerenciaNumeroComprobanteDTO sugerencia =
                pagoLetraService.sugerirNumeroComprobante(tipoComprobante); // ← CORREGIDO
        return ResponseEntity.ok(sugerencia);
    }

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

        byte[] pdf = ComprobantePagoLetraPdf.generar(pago, rolUsuario);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=comprobante-pago-" + idPago + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /**
     * Descarga el comprobante PDF de un pago múltiple por número de comprobante.
     * CORRECCIÓN: usa findByComprobanteNumeroCompleto() en lugar del antiguo
     * findByNumeroComprobante() que referenciaba un campo ya eliminado.
     */
    @GetMapping("/comprobante-multiple/{numeroComprobante}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> descargarComprobanteMultiple(
            @PathVariable String numeroComprobante) {

        // ── CORRECCIÓN: buscar por la relación Comprobante.numeroCompleto ──────
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

        byte[] pdf = ComprobantePagoLetraPdf.generarMultiple(pagos, rolUsuarioMultiple);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=comprobante-multiple-" + numeroComprobante + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}