package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.ComprobanteResponseDTO;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.enums.TipoOrigenComprobante;
import com.Inmobiliaria.demo.service.ComprobanteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/comprobantes")
@RequiredArgsConstructor
public class ComprobanteController {

    private final ComprobanteService comprobanteService;

    @GetMapping("/{id}")
    public ResponseEntity<ComprobanteResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(comprobanteService.obtenerPorId(id));
    }

    @GetMapping("/buscar")
    public ResponseEntity<ComprobanteResponseDTO> buscarPorNumero(
            @RequestParam String numero) {
        return ResponseEntity.ok(comprobanteService.obtenerPorNumeroCompleto(numero));
    }

    @GetMapping("/por-tipo")
    public ResponseEntity<List<ComprobanteResponseDTO>> listarPorTipo(
            @RequestParam TipoComprobante tipo) {
        return ResponseEntity.ok(comprobanteService.listarPorTipo(tipo));
    }

    @GetMapping("/por-origen")
    public ResponseEntity<List<ComprobanteResponseDTO>> listarPorOrigen(
            @RequestParam TipoOrigenComprobante origen) {
        return ResponseEntity.ok(comprobanteService.listarPorOrigen(origen));
    }

    @GetMapping("/por-fecha")
    public ResponseEntity<List<ComprobanteResponseDTO>> listarPorFecha(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(comprobanteService.listarPorRangoFecha(desde, hasta));
    }

    @GetMapping(value = "/preview-siguiente", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> previewSiguienteNumero(
            @RequestParam TipoComprobante tipo) {
        return ResponseEntity.ok(comprobanteService.previewSiguienteNumero(tipo));
    }
}