package com.Inmobiliaria.demo.service;

import java.util.List;

import com.Inmobiliaria.demo.entity.Empresa;

public interface EmpresaService {

    Empresa obtenerActiva();
    List<Empresa> listarTodas();
    Empresa obtenerPorId(Long id);
    Empresa crear(Empresa empresa);
    Empresa actualizar(Long id, Empresa datos);
    Empresa activarEmpresa(Long id);
    void eliminar(Long id);
}
