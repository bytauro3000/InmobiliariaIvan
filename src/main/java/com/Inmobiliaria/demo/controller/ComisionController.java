package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.ComisionVendedorDTO;
import com.Inmobiliaria.demo.dto.PagoComisionMensualDTO;
import com.Inmobiliaria.demo.dto.PagoComisionRequestDTO;
import com.Inmobiliaria.demo.dto.PagoComisionResponseDTO;
import com.Inmobiliaria.demo.dto.PagoComisionResultadoDTO;
import com.Inmobiliaria.demo.dto.RegistrarAdelantoRequest;
import com.Inmobiliaria.demo.dto.RegistrarPagosMensualesRequest;
import com.Inmobiliaria.demo.dto.ActualizarMontoComisionRequest;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.service.ComisionVendedorService;
import com.Inmobiliaria.demo.service.ReciboEgresoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/comisiones")
@RequiredArgsConstructor
public class ComisionController {

    private final ComisionVendedorService comisionService;
    private final ReciboEgresoService reciboEgresoService;

    // ─── Listado de comisiones (secretaría) ───────────────────────────────────

    @GetMapping
    public ResponseEntity<List<ComisionVendedorDTO>> listarComisiones() {
        return ResponseEntity.ok(comisionService.listarComisiones());
    }

    // ─── Migración (backfill) de contratos existentes ─────────────────────────

    @PostMapping("/migrar")
    public ResponseEntity<?> migrarComisiones() {
        try {
            return ResponseEntity.ok(comisionService.migrarComisiones());
        } catch (Exception e) {
            log.error("Error en la migración de comisiones: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error en la migración: " + e.getMessage());
        }
    }

    // ─── Pagos mensuales habilitados de una comisión ──────────────────────────

    @GetMapping("/{idComision}/pagos-habilitados")
    public ResponseEntity<List<PagoComisionMensualDTO>> pagosMensualesHabilitados(
            @PathVariable Integer idComision) {
        return ResponseEntity.ok(comisionService.pagosMensualesHabilitados(idComision));
    }

    // ─── Actualizar monto de comisión acordado (negociación) ─────────────────

    @PutMapping("/monto")
    public ResponseEntity<?> actualizarMontoComision(@RequestBody ActualizarMontoComisionRequest request) {
        try {
            return ResponseEntity.ok(comisionService.actualizarMontoComision(request));
        } catch (NegocioException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // ─── Registrar adelanto ───────────────────────────────────────────────────

    @PostMapping("/adelantos")
    public ResponseEntity<?> registrarAdelanto(@RequestBody RegistrarAdelantoRequest request) {
        try {
            return ResponseEntity.ok(comisionService.registrarAdelanto(request));
        } catch (NegocioException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // ─── Registrar pago de comisión (adelanto o mensual multi-lote) con vouchers ─

    @PostMapping(value = "/pagos/registrar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registrarPagoComision(
            @RequestPart("pago") PagoComisionRequestDTO request,
            @RequestPart(value = "vouchers", required = false) List<MultipartFile> vouchers) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(comisionService.registrarPagoComision(request, vouchers));
        } catch (NegocioException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error registrando pago de comisión: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error registrando el pago: " + e.getMessage());
        }
    }

    // ─── Registrar pagos mensuales (multiselección) ───────────────────────────

    @PostMapping("/pagos")
    public ResponseEntity<?> registrarPagosMensuales(@RequestBody RegistrarPagosMensualesRequest request) {
        try {
            return ResponseEntity.ok(comisionService.registrarPagosMensuales(request));
        } catch (NegocioException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // ─── Descargar PDF del recibo de egresos ──────────────────────────────────

    @GetMapping("/egresos/{numeroEgreso}/pdf")
    public ResponseEntity<byte[]> descargarEgresoPdf(@PathVariable String numeroEgreso) {
        try {
            byte[] pdf = reciboEgresoService.generarPdf(numeroEgreso);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"egreso-" + numeroEgreso + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (NegocioException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error generando recibo de egreso {}: ", numeroEgreso, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error generando PDF: " + e.getMessage()).getBytes());
        }
    }
}