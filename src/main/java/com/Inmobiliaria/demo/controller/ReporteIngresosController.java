package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.ResumenIngresosRangoDTO;
import com.Inmobiliaria.demo.service.ReporteIngresosService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reporte-ingresos")
@RequiredArgsConstructor
public class ReporteIngresosController {

    private final ReporteIngresosService reporteIngresosService;

    
    @GetMapping
    public ResponseEntity<ResumenIngresosRangoDTO> obtenerIngresosPorRango(
            @RequestParam("desde")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam("hasta")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta) {

        if (desde.isAfter(hasta)) {
            return ResponseEntity.badRequest().build();
        }

        ResumenIngresosRangoDTO resultado =
                reporteIngresosService.obtenerIngresosPorRango(desde, hasta);

        return ResponseEntity.ok(resultado);
    }
}