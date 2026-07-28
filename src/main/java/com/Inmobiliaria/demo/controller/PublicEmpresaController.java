package com.Inmobiliaria.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Inmobiliaria.demo.dto.PublicEmpresaDTO;
import com.Inmobiliaria.demo.entity.Empresa;
import com.Inmobiliaria.demo.service.EmpresaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public/empresa")
@RequiredArgsConstructor
public class PublicEmpresaController {

    private final EmpresaService empresaService;

    @GetMapping
    public ResponseEntity<PublicEmpresaDTO> obtenerEmpresa() {
        Empresa e = empresaService.obtenerActiva();

        PublicEmpresaDTO dto = new PublicEmpresaDTO();
        dto.setNombreLegal(e.getNombreLegal());
        dto.setNombreComercial(e.getNombreComercial());
        dto.setRuc(e.getRuc());
        dto.setDireccion(e.getDireccion());
        dto.setTelefono(e.getTelefono());
        dto.setCelular(e.getCelular());
        dto.setEmail(e.getEmail());
        dto.setLogoUrl(e.getLogoUrl());
        dto.setLogoSmallUrl(e.getLogoSmallUrl());
        dto.setPaginaWeb(e.getPaginaWeb());
        dto.setWhatsapp(e.getCelular());

        return ResponseEntity.ok(dto);
    }
}
