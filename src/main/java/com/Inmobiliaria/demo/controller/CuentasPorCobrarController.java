package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.CuentasPorCobrarDTO;
import com.Inmobiliaria.demo.service.CuentasPorCobrarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cuentas-por-cobrar")
@RequiredArgsConstructor
public class CuentasPorCobrarController {

    private final CuentasPorCobrarService cuentasPorCobrarService;

    @GetMapping
    public ResponseEntity<CuentasPorCobrarDTO> obtenerCuentasPorCobrar() {
        return ResponseEntity.ok(cuentasPorCobrarService.obtenerCuentasPorCobrar());
    }
}