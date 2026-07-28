package com.Inmobiliaria.demo.config;

import org.springframework.stereotype.Component;

import com.Inmobiliaria.demo.service.EmpresaService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmpresaContext {

    private final EmpresaService service;

    public static EmpresaService empresaService;

    @PostConstruct
    void init() {
        empresaService = service;
    }
}
