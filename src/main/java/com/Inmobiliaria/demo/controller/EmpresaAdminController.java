package com.Inmobiliaria.demo.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Inmobiliaria.demo.dto.EmpresaRequestDTO;
import com.Inmobiliaria.demo.dto.EmpresaResponseDTO;
import com.Inmobiliaria.demo.entity.Empresa;
import com.Inmobiliaria.demo.enums.TipoCalculoMora;
import com.Inmobiliaria.demo.service.EmpresaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/empresa")
@RequiredArgsConstructor
public class EmpresaAdminController {

    private final EmpresaService empresaService;

    @GetMapping
    public ResponseEntity<List<EmpresaResponseDTO>> listarTodas() {
        List<EmpresaResponseDTO> list = empresaService.listarTodas().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpresaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(empresaService.obtenerPorId(id)));
    }

    @PostMapping
    public ResponseEntity<EmpresaResponseDTO> crear(@RequestBody EmpresaRequestDTO dto) {
        Empresa empresa = toEntity(dto);
        return ResponseEntity.ok(toResponse(empresaService.crear(empresa)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmpresaResponseDTO> actualizar(@PathVariable Long id, @RequestBody EmpresaRequestDTO dto) {
        return ResponseEntity.ok(toResponse(empresaService.actualizar(id, toEntity(dto))));
    }

    @PutMapping("/{id}/activar")
    public ResponseEntity<EmpresaResponseDTO> activar(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(empresaService.activarEmpresa(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        empresaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private EmpresaResponseDTO toResponse(Empresa e) {
        if (e == null) return null;
        EmpresaResponseDTO dto = new EmpresaResponseDTO();
        dto.setId(e.getId());
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
        dto.setRepresentanteLegal(e.getRepresentanteLegal());
        dto.setRepresentanteDni(e.getRepresentanteDni());
        dto.setPartidaElectronica(e.getPartidaElectronica());
        dto.setUbigeo(e.getUbigeo());
        dto.setDistrito(e.getDistrito());
        dto.setProvincia(e.getProvincia());
        dto.setDepartamento(e.getDepartamento());
        dto.setTipoCalculoMora(e.getTipoCalculoMora() != null ? e.getTipoCalculoMora().name() : null);
        dto.setMoraPorcentaje(e.getMoraPorcentaje());
        dto.setMoraMontoDiario(e.getMoraMontoDiario());
        dto.setMoraTasaDiaria(e.getMoraTasaDiaria());
        dto.setApisperuEnvironment(e.getApisperuEnvironment());
        dto.setWhatsappDeviceId(e.getWhatsappDeviceId());
        dto.setNotificacionEmail(e.getNotificacionEmail());
        dto.setActiva(e.getActiva());
        dto.setFechaRegistro(e.getFechaRegistro());
        dto.setFechaActualizacion(e.getFechaActualizacion());
        return dto;
    }

    private Empresa toEntity(EmpresaRequestDTO dto) {
        if (dto == null) return null;
        Empresa e = new Empresa();
        e.setNombreLegal(dto.getNombreLegal());
        e.setNombreComercial(dto.getNombreComercial());
        e.setRuc(dto.getRuc());
        e.setDireccion(dto.getDireccion());
        e.setTelefono(dto.getTelefono());
        e.setCelular(dto.getCelular());
        e.setEmail(dto.getEmail());
        e.setLogoUrl(dto.getLogoUrl());
        e.setLogoSmallUrl(dto.getLogoSmallUrl());
        e.setPaginaWeb(dto.getPaginaWeb());
        e.setRepresentanteLegal(dto.getRepresentanteLegal());
        e.setRepresentanteDni(dto.getRepresentanteDni());
        e.setPartidaElectronica(dto.getPartidaElectronica());
        e.setUbigeo(dto.getUbigeo());
        e.setDistrito(dto.getDistrito());
        e.setProvincia(dto.getProvincia());
        e.setDepartamento(dto.getDepartamento());
        if (dto.getTipoCalculoMora() != null) {
            e.setTipoCalculoMora(TipoCalculoMora.valueOf(dto.getTipoCalculoMora()));
        }
        e.setMoraPorcentaje(dto.getMoraPorcentaje());
        e.setMoraMontoDiario(dto.getMoraMontoDiario());
        e.setMoraTasaDiaria(dto.getMoraTasaDiaria());
        e.setApisperuEnvironment(dto.getApisperuEnvironment());
        e.setWhatsappDeviceId(dto.getWhatsappDeviceId());
        e.setNotificacionEmail(dto.getNotificacionEmail());
        return e;
    }
}
