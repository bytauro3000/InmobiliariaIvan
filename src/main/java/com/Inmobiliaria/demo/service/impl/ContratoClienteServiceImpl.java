package com.Inmobiliaria.demo.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.Inmobiliaria.demo.entity.ContratoCliente;
import com.Inmobiliaria.demo.repository.ContratoClienteRepository;
import com.Inmobiliaria.demo.service.ContratoClienteService;

@Service
public class ContratoClienteServiceImpl implements ContratoClienteService {

    @Autowired
    private ContratoClienteRepository contratoClienteRepository;

    @Override
    public void guardar(ContratoCliente contratoCliente) {
        contratoClienteRepository.save(contratoCliente);
    }
}
