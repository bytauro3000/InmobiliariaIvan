package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.ReporteClientesMoraDTO;
import com.Inmobiliaria.demo.service.ReporteMoraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;


@RestController
@RequestMapping("/api/reporte-mora")
@RequiredArgsConstructor
public class ReporteMoraController {

    private final ReporteMoraService reporteMoraService;

  
    @GetMapping("/clientes")
    public ResponseEntity<List<ReporteClientesMoraDTO>> obtenerClientesEnMora() {
        List<ReporteClientesMoraDTO> reporte = reporteMoraService.obtenerClientesEnMora();
        return ResponseEntity.ok(reporte);
    }


    @GetMapping("/clientes/pdf")
    public ResponseEntity<byte[]> descargarPdfClientesEnMora() {
        byte[] pdf = reporteMoraService.generarPdfClientesEnMora();

        String nombreArchivo = "reporte-mora-" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}