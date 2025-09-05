package com.Inmobiliaria.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Inmobiliaria.demo.dto.GenerarLetrasRequest;
import com.Inmobiliaria.demo.dto.LetraCambioDTO;
import com.Inmobiliaria.demo.dto.ReporteLetraCambioDTO;
import com.Inmobiliaria.demo.service.LetraCambioService;

@RestController
@RequestMapping("/api/letras")
@CrossOrigin(origins = "*")
public class LetrasCambioController {

    @Autowired
    private LetraCambioService letraCambioService;

 // Listar letras por contrato
    @GetMapping("/listar/{idContrato}")
    public ResponseEntity<List<LetraCambioDTO>> listarPorContrato(@PathVariable Integer idContrato) {
        List<LetraCambioDTO> listaLetras = letraCambioService.listarPorContrato(idContrato);
        return new ResponseEntity<>(listaLetras, HttpStatus.OK);
    }
    
    // Generar letras para un contrato dado
    @PostMapping("/contrato/{idContrato}")
    public ResponseEntity<Void> generarLetras(
        @PathVariable Integer idContrato,
        @RequestBody GenerarLetrasRequest request
    ) {
        letraCambioService.generarLetrasDesdeContrato(idContrato, request); // Corregido
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
    
    //Actualizar una letra de cambio por su ID (no por contrato)
    @PutMapping("/actualizar/{idLetra}")
    public ResponseEntity<LetraCambioDTO> actualizarLetra(
        @PathVariable Integer idLetra,
        @RequestBody LetraCambioDTO letraCambioDTO
    ) {
        LetraCambioDTO letraActualizada = letraCambioService.actualizarLetra(idLetra, letraCambioDTO);
        return new ResponseEntity<>(letraActualizada, HttpStatus.OK);
    }
    
    //Eliminar todas las letras de un contrato
    @DeleteMapping("/eliminar/{idContrato}")
    public ResponseEntity<Void> eliminarPorContrato(@PathVariable Integer idContrato) {
        letraCambioService.eliminarPorContrato(idContrato);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    
 // Obtener el reporte de letras por contrato
    @GetMapping("/reporte/{idContrato}")
    public ResponseEntity<List<ReporteLetraCambioDTO>> obtenerReportePorContrato(@PathVariable Integer idContrato) {
        List<ReporteLetraCambioDTO> reporte = letraCambioService.obtenerReportePorContrato(idContrato);
        return new ResponseEntity<>(reporte, HttpStatus.OK);
    }
    
}
