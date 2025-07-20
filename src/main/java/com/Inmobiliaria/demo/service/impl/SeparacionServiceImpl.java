package com.Inmobiliaria.demo.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Inmobiliaria.demo.dto.SeparacionDTO;
import com.Inmobiliaria.demo.repository.SeparacionRepository;
import com.Inmobiliaria.demo.service.SeparacionService;

@Service
public class SeparacionServiceImpl implements SeparacionService {

    @Autowired
    private SeparacionRepository separacionRepository;

    @Override
    public List<SeparacionDTO> buscarPorDniOApellido(String filtro) {
        return separacionRepository.buscarPorDniOApellido(filtro);
    }
}
