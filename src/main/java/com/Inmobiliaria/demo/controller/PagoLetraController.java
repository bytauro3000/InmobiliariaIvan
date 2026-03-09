package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.PagoLetraRequestDTO;
import com.Inmobiliaria.demo.dto.PagoLetraResponseDTO;
import com.Inmobiliaria.demo.dto.PagosMultiplesRequestDTO;
import com.Inmobiliaria.demo.dto.SugerenciaNumeroComprobanteDTO;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.service.PagoLetraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoLetraController {

    private final PagoLetraService pagoLetraService;

    @GetMapping("/contrato/{idContrato}")
    public ResponseEntity<List<PagoLetraResponseDTO>> listarPorContrato(@PathVariable Integer idContrato) {
        return ResponseEntity.ok(pagoLetraService.listarPorContrato(idContrato));
    }

    @GetMapping("/letra/{idLetra}")
    public ResponseEntity<List<PagoLetraResponseDTO>> listarPorLetra(@PathVariable Integer idLetra) {
        return ResponseEntity.ok(pagoLetraService.listarPorLetra(idLetra));
    }

    @GetMapping("/{idPago}")
    public ResponseEntity<PagoLetraResponseDTO> obtenerPorId(@PathVariable Integer idPago) {
        return ResponseEntity.ok(pagoLetraService.obtenerPorId(idPago));
    }

    @PostMapping(value = "/registrar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PagoLetraResponseDTO> registrarPago(
            @RequestPart("pago") PagoLetraRequestDTO request,
            @RequestPart(value = "vouchers", required = false) List<MultipartFile> vouchers) throws IOException {
        PagoLetraResponseDTO response = pagoLetraService.registrarPago(request, vouchers);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping(value = "/registrar-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<PagoLetraResponseDTO>> registrarPagosMultiples(
            @RequestPart("pagos") PagosMultiplesRequestDTO request,
            @RequestPart(value = "vouchers", required = false) List<MultipartFile> vouchers) throws IOException {
        List<PagoLetraResponseDTO> responses = pagoLetraService.registrarPagosMultiples(request, vouchers);
        return new ResponseEntity<>(responses, HttpStatus.CREATED);
    }

    @PutMapping(value = "/actualizar/{idPago}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PagoLetraResponseDTO> actualizarPago(
            @PathVariable Integer idPago,
            @RequestPart("pago") PagoLetraRequestDTO request,
            @RequestPart(value = "vouchers", required = false) List<MultipartFile> vouchers) throws IOException {
        PagoLetraResponseDTO response = pagoLetraService.actualizarPago(idPago, request, vouchers);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/eliminar/{idPago}")
    public ResponseEntity<Void> eliminarPago(@PathVariable Integer idPago) throws IOException {
        pagoLetraService.eliminarPago(idPago);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/sugerir-numero")
    public ResponseEntity<SugerenciaNumeroComprobanteDTO> sugerirNumeroComprobante(
            @RequestParam TipoComprobante tipoComprobante) {
        SugerenciaNumeroComprobanteDTO sugerencia = pagoLetraService.sugerirNumeroComprobante(tipoComprobante);
        return ResponseEntity.ok(sugerencia);
    }
}