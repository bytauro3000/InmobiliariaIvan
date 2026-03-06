package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.PagoLetraRequestDTO;
import com.Inmobiliaria.demo.dto.PagoLetraResponseDTO;
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

    // Listar pagos por contrato
    @GetMapping("/contrato/{idContrato}")
    public ResponseEntity<List<PagoLetraResponseDTO>> listarPorContrato(@PathVariable Integer idContrato) {
        return ResponseEntity.ok(pagoLetraService.listarPorContrato(idContrato));
    }

    // Listar pagos por letra
    @GetMapping("/letra/{idLetra}")
    public ResponseEntity<List<PagoLetraResponseDTO>> listarPorLetra(@PathVariable Integer idLetra) {
        return ResponseEntity.ok(pagoLetraService.listarPorLetra(idLetra));
    }

    // Obtener un pago por ID
    @GetMapping("/{idPago}")
    public ResponseEntity<PagoLetraResponseDTO> obtenerPorId(@PathVariable Integer idPago) {
        return ResponseEntity.ok(pagoLetraService.obtenerPorId(idPago));
    }

    // Registrar un nuevo pago (con voucher opcional)
    @PostMapping(value = "/registrar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PagoLetraResponseDTO> registrarPago(
            @RequestPart("pago") PagoLetraRequestDTO request,
            @RequestPart(value = "voucher", required = false) MultipartFile voucher) throws IOException {

        PagoLetraResponseDTO response = pagoLetraService.registrarPago(request, voucher);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Actualizar un pago existente
    @PutMapping(value = "/actualizar/{idPago}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PagoLetraResponseDTO> actualizarPago(
            @PathVariable Integer idPago,
            @RequestPart("pago") PagoLetraRequestDTO request,
            @RequestPart(value = "voucher", required = false) MultipartFile voucher) throws IOException {

        PagoLetraResponseDTO response = pagoLetraService.actualizarPago(idPago, request, voucher);
        return ResponseEntity.ok(response);
    }

    // Eliminar un pago (cuidado con la consistencia)
    @DeleteMapping("/eliminar/{idPago}")
    public ResponseEntity<Void> eliminarPago(@PathVariable Integer idPago) {
        pagoLetraService.eliminarPago(idPago);
        return ResponseEntity.noContent().build();
    }
}