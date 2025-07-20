package com.Inmobiliaria.demo.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.Inmobiliaria.demo.entity.ContratoLote;
import com.Inmobiliaria.demo.repository.ContratoLoteRepository;
import com.Inmobiliaria.demo.service.ContratoLoteService;

@Service
public class ContratoLoteServiceImpl implements ContratoLoteService {

    @Autowired
    private ContratoLoteRepository contratoLoteRepository;

    @Override
    public void guardar(ContratoLote contratoLote) {
        contratoLoteRepository.save(contratoLote);
    }
}
