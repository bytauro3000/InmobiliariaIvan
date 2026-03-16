package com.Inmobiliaria.demo.service.impl;

import org.springframework.stereotype.Service;
import com.Inmobiliaria.demo.entity.ContratoCliente;

import com.Inmobiliaria.demo.repository.ContratoClienteRepository;
import com.Inmobiliaria.demo.service.ContratoClienteService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class ContratoClienteServiceImpl implements ContratoClienteService {

   
    private final ContratoClienteRepository contratoClienteRepository;

    @Override
    public void guardar(ContratoCliente contratoCliente) {
        contratoClienteRepository.save(contratoCliente);
    }
}
