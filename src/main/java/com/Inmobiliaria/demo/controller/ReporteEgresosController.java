package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.ResumenEgresosRangoDTO;
import com.Inmobiliaria.demo.service.ReporteEgresosService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reporte-egresos")
@RequiredArgsConstructor
public class ReporteEgresosController {

    private final ReporteEgresosService reporteEgresosService;

    @GetMapping
    public ResponseEntity<ResumenEgresosRangoDTO> obtenerEgresosPorRango(
            @RequestParam("desde")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam("hasta")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta) {

        if (desde.isAfter(hasta)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(reporteEgresosService.obtenerEgresosPorRango(desde, hasta));
    }
}