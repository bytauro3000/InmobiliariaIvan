package com.Inmobiliaria.demo.service.impl;
import org.springframework.stereotype.Service;
import com.Inmobiliaria.demo.entity.ContratoLote;
import com.Inmobiliaria.demo.repository.ContratoLoteRepository;
import com.Inmobiliaria.demo.service.ContratoLoteService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class ContratoLoteServiceImpl implements ContratoLoteService {

    
    private final ContratoLoteRepository contratoLoteRepository;

    @Override
    public void guardar(ContratoLote contratoLote) {
        contratoLoteRepository.save(contratoLote);
    }
}
