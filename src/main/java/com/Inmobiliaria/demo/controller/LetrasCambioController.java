package com.Inmobiliaria.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Inmobiliaria.demo.dto.GenerarLetrasRequest;
import com.Inmobiliaria.demo.dto.LetraCambioDTO;
import com.Inmobiliaria.demo.service.LetraCambioService;

@RestController
@RequestMapping("/api/letras")
@CrossOrigin(origins = "*")
public class LetrasCambioController {

    @Autowired
    private LetraCambioService letraCambioService;

 // Listar letras por contrato
    @GetMapping("/contrato/{idContrato}")
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
}
