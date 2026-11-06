package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.ReporteIngresosDetalladoDTO;
import com.Inmobiliaria.demo.dto.ResumenIngresosRangoDTO;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.service.ReporteIngresosDetalladoService;
import com.Inmobiliaria.demo.service.ReporteIngresosService;
import com.Inmobiliaria.demo.util.IngresosDetalladosExcel;
import com.Inmobiliaria.demo.util.ReporteIngresosDetalladoPdf;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reporte-ingresos")
@RequiredArgsConstructor
public class ReporteIngresosController {

    private final ReporteIngresosService reporteIngresosService;
    private final ReporteIngresosDetalladoService reporteIngresosDetalladoService;

    
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

    /**
     * Reporte de ingresos detallado por comprobante (BOLETA/RECIBO).
     * Filtra por rango de fecha_emision y tipo (TODOS|BOLETA|RECIBO).
     */
    @GetMapping("/detallado")
    public ResponseEntity<ReporteIngresosDetalladoDTO> obtenerDetallado(
            @RequestParam("desde")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam("hasta")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta,

            @RequestParam(value = "tipo", defaultValue = "TODOS")
            String tipo) {

        if (desde.isAfter(hasta)) {
            return ResponseEntity.badRequest().build();
        }

        List<TipoComprobante> tipos = resolverTipos(tipo);
        if (tipos == null) {
            return ResponseEntity.badRequest().build();
        }

        ReporteIngresosDetalladoDTO resultado = reporteIngresosDetalladoService
                .obtenerReporteDetallado(desde, hasta, tipos, tipo.toUpperCase());

        return ResponseEntity.ok(resultado);
    }

    /** Descarga en PDF del reporte de ingresos detallado. */
    @GetMapping("/detallado/pdf")
    public ResponseEntity<byte[]> descargarDetalladoPdf(
            @RequestParam("desde")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam("hasta")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta,

            @RequestParam(value = "tipo", defaultValue = "TODOS")
            String tipo) {

        if (desde.isAfter(hasta)) {
            return ResponseEntity.badRequest().build();
        }

        List<TipoComprobante> tipos = resolverTipos(tipo);
        if (tipos == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            ReporteIngresosDetalladoDTO dto = reporteIngresosDetalladoService
                    .obtenerReporteDetallado(desde, hasta, tipos, tipo.toUpperCase());
            byte[] pdf = ReporteIngresosDetalladoPdf.generar(dto);

            String filename = "ingreso-detallado-" + LocalDate.now() + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Error generando PDF de ingreso detallado (desde={}, hasta={}, tipo={}): ",
                    desde, hasta, tipo, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /** Descarga en Excel del reporte de ingresos detallado. */
    @GetMapping("/detallado/excel")
    public ResponseEntity<byte[]> descargarDetalladoExcel(
            @RequestParam("desde")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam("hasta")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta,

            @RequestParam(value = "tipo", defaultValue = "TODOS")
            String tipo) {

        if (desde.isAfter(hasta)) {
            return ResponseEntity.badRequest().build();
        }

        List<TipoComprobante> tipos = resolverTipos(tipo);
        if (tipos == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            ReporteIngresosDetalladoDTO dto = reporteIngresosDetalladoService
                    .obtenerReporteDetallado(desde, hasta, tipos, tipo.toUpperCase());
            byte[] excel = IngresosDetalladosExcel.generar(dto);

            String filename = "ingreso-detallado-" + LocalDate.now() + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excel);
        } catch (Exception e) {
            log.error("Error generando Excel de ingreso detallado (desde={}, hasta={}, tipo={}): ",
                    desde, hasta, tipo, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Convierte el filtro del reporte (TODOS|BOLETA|RECIBO) a tipos de
     * comprobante. Devuelve null si el valor es inválido.
     */
    private List<TipoComprobante> resolverTipos(String tipo) {
        return switch (tipo.toUpperCase()) {
            case "TODOS"  -> List.of(TipoComprobante.BOLETA, TipoComprobante.RECIBO);
            case "BOLETA" -> List.of(TipoComprobante.BOLETA);
            case "RECIBO" -> List.of(TipoComprobante.RECIBO);
            default       -> null;
        };
    }
}