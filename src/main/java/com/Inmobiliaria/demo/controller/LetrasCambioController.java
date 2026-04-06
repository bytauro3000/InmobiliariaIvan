package com.Inmobiliaria.demo.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Inmobiliaria.demo.dto.GenerarLetrasRequest;
import com.Inmobiliaria.demo.dto.LetraCambioDTO;
import com.Inmobiliaria.demo.dto.ReporteCronogramaPagosClientesDTO;
import com.Inmobiliaria.demo.dto.ReporteLetraCambioDTO;
import com.Inmobiliaria.demo.service.LetraCambioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/letras")
@RequiredArgsConstructor
public class LetrasCambioController {

    private final LetraCambioService letraCambioService;

    // ── Listar letras por contrato ────────────────────────────────────────────
    @GetMapping("/listar/{idContrato}")
    public ResponseEntity<List<LetraCambioDTO>> listarPorContrato(
            @PathVariable Integer idContrato) {
        List<LetraCambioDTO> listaLetras = letraCambioService.listarPorContrato(idContrato);
        return new ResponseEntity<>(listaLetras, HttpStatus.OK);
    }

    // ── Generar letras para un contrato dado ──────────────────────────────────
    @PostMapping("/contrato/{idContrato}")
    public ResponseEntity<Void> generarLetras(
            @PathVariable Integer idContrato,
            @RequestBody GenerarLetrasRequest request) {
        letraCambioService.generarLetrasDesdeContrato(idContrato, request);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    // ── Actualizar una letra de cambio por su ID ──────────────────────────────
    @PutMapping("/actualizar/{idLetra}")
    public ResponseEntity<LetraCambioDTO> actualizarLetra(
            @PathVariable Integer idLetra,
            @RequestBody LetraCambioDTO letraCambioDTO) {
        LetraCambioDTO letraActualizada = letraCambioService.actualizarLetra(idLetra, letraCambioDTO);
        return new ResponseEntity<>(letraActualizada, HttpStatus.OK);
    }

    // ── Eliminar todas las letras de un contrato ──────────────────────────────
    @DeleteMapping("/eliminar/{idContrato}")
    public ResponseEntity<Void> eliminarPorContrato(@PathVariable Integer idContrato) {
        letraCambioService.eliminarPorContrato(idContrato);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // ── Obtener el reporte JSON de letras por contrato ────────────────────────
    @GetMapping("/reporte/{idContrato}")
    public ResponseEntity<List<ReporteLetraCambioDTO>> obtenerReportePorContrato(
            @PathVariable Integer idContrato) {
        List<ReporteLetraCambioDTO> reporte = letraCambioService.obtenerReportePorContrato(idContrato);
        return new ResponseEntity<>(reporte, HttpStatus.OK);
    }

    // ── Reporte de cronograma de pagos clientes ───────────────────────────────
    @GetMapping("/repcronograma/{idContrato}")
    public ResponseEntity<List<ReporteCronogramaPagosClientesDTO>> obtenerReporteCronogramaPagosPorContrato(
            @PathVariable Integer idContrato) {
        List<ReporteCronogramaPagosClientesDTO> reporte =
                letraCambioService.obtenerReporteCronogramaPagosPorContrato(idContrato);
        return new ResponseEntity<>(reporte, HttpStatus.OK);
    }

    // ── Verificar si existen letras para un contrato ──────────────────────────
    @GetMapping("/existe/{idContrato}")
    public ResponseEntity<Boolean> existenLetras(@PathVariable Integer idContrato) {
        return ResponseEntity.ok(letraCambioService.existenLetrasPorContrato(idContrato));
    }


    @GetMapping("/pdf/{idContrato}")
    public ResponseEntity<byte[]> generarPdfLetras(@PathVariable Integer idContrato) {
        byte[] pdfBytes = letraCambioService.generarPdfLetras(idContrato);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        // "inline" → el navegador lo abre en una nueva pestaña (igual que window.open en jsPDF)
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"letras-contrato-" + idContrato + ".pdf\"");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}