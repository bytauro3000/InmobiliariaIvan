package com.Inmobiliaria.demo.config;

import org.springframework.stereotype.Component;

import com.Inmobiliaria.demo.repository.EmpresaRepository;
import com.Inmobiliaria.demo.service.EmpresaService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmpresaContext {

    private final EmpresaService service;
    private final EmpresaRepository repository;

    public static EmpresaService empresaService;
    public static EmpresaRepository empresaRepository;

    @PostConstruct
    void init() {
        empresaService = service;
        empresaRepository = repository;
    }
}
