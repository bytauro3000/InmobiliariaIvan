package com.Inmobiliaria.demo.service.impl;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Inmobiliaria.demo.entity.Empresa;
import com.Inmobiliaria.demo.enums.TipoCalculoMora;
import com.Inmobiliaria.demo.repository.EmpresaRepository;
import com.Inmobiliaria.demo.service.EmpresaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmpresaServiceImpl implements EmpresaService {

    private final EmpresaRepository empresaRepository;

    @Override
    @Cacheable(value = "empresaActiva")
    public Empresa obtenerActiva() {
        return empresaRepository.findByActivaTrue()
            .orElseThrow(() -> new RuntimeException("No hay una empresa activa configurada"));
    }

    @Override
    public List<Empresa> listarTodas() {
        return empresaRepository.findAll();
    }

    @Override
    public Empresa obtenerPorId(Long id) {
        return empresaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada con id: " + id));
    }

    @Override
    @Transactional
    @CacheEvict(value = "empresaActiva", allEntries = true)
    public Empresa crear(Empresa empresa) {
        if (empresa.getActiva() == null) {
            empresa.setActiva(false);
        }
        if (empresa.getTipoCalculoMora() == null) {
            empresa.setTipoCalculoMora(TipoCalculoMora.PORCENTAJE_MAS_DIARIO);
        }
        return empresaRepository.save(empresa);
    }

    @Override
    @Transactional
    @CacheEvict(value = "empresaActiva", allEntries = true)
    public Empresa actualizar(Long id, Empresa datos) {
        Empresa existente = obtenerPorId(id);
        if (datos.getNombreLegal() != null) existente.setNombreLegal(datos.getNombreLegal());
        if (datos.getNombreComercial() != null) existente.setNombreComercial(datos.getNombreComercial());
        if (datos.getRuc() != null) existente.setRuc(datos.getRuc());
        if (datos.getDireccion() != null) existente.setDireccion(datos.getDireccion());
        if (datos.getTelefono() != null) existente.setTelefono(datos.getTelefono());
        if (datos.getCelular() != null) existente.setCelular(datos.getCelular());
        if (datos.getEmail() != null) existente.setEmail(datos.getEmail());
        if (datos.getLogoUrl() != null) existente.setLogoUrl(datos.getLogoUrl());
        if (datos.getLogoSmallUrl() != null) existente.setLogoSmallUrl(datos.getLogoSmallUrl());
        if (datos.getPaginaWeb() != null) existente.setPaginaWeb(datos.getPaginaWeb());
        if (datos.getRepresentanteLegal() != null) existente.setRepresentanteLegal(datos.getRepresentanteLegal());
        if (datos.getRepresentanteDni() != null) existente.setRepresentanteDni(datos.getRepresentanteDni());
        if (datos.getPartidaElectronica() != null) existente.setPartidaElectronica(datos.getPartidaElectronica());
        if (datos.getUbigeo() != null) existente.setUbigeo(datos.getUbigeo());
        if (datos.getDistrito() != null) existente.setDistrito(datos.getDistrito());
        if (datos.getProvincia() != null) existente.setProvincia(datos.getProvincia());
        if (datos.getDepartamento() != null) existente.setDepartamento(datos.getDepartamento());
        if (datos.getTipoCalculoMora() != null) existente.setTipoCalculoMora(datos.getTipoCalculoMora());
        if (datos.getMoraPorcentaje() != null) existente.setMoraPorcentaje(datos.getMoraPorcentaje());
        if (datos.getMoraMontoDiario() != null) existente.setMoraMontoDiario(datos.getMoraMontoDiario());
        if (datos.getMoraTasaDiaria() != null) existente.setMoraTasaDiaria(datos.getMoraTasaDiaria());
        if (datos.getApisperuEnvironment() != null) existente.setApisperuEnvironment(datos.getApisperuEnvironment());
        if (datos.getWhatsappDeviceId() != null) existente.setWhatsappDeviceId(datos.getWhatsappDeviceId());
        if (datos.getNotificacionEmail() != null) existente.setNotificacionEmail(datos.getNotificacionEmail());
        if (datos.getActiva() != null) {
            if (Boolean.TRUE.equals(datos.getActiva())) {
                activarEmpresa(id);
            } else {
                existente.setActiva(false);
            }
        }
        return empresaRepository.save(existente);
    }

    @Override
    @Transactional
    @CacheEvict(value = "empresaActiva", allEntries = true)
    public Empresa activarEmpresa(Long id) {
        empresaRepository.findAll().forEach(e -> {
            e.setActiva(false);
            empresaRepository.save(e);
        });
        Empresa empresa = obtenerPorId(id);
        empresa.setActiva(true);
        return empresaRepository.save(empresa);
    }

    @Override
    @Transactional
    @CacheEvict(value = "empresaActiva", allEntries = true)
    public void eliminar(Long id) {
        Empresa empresa = obtenerPorId(id);
        empresaRepository.delete(empresa);
    }
}
