package com.Inmobiliaria.demo.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.Inmobiliaria.demo.entity.Distrito;
import com.Inmobiliaria.demo.repository.DistritoRepository;
import com.Inmobiliaria.demo.service.DistritoService;

@Service
public class DistritoServiceImpl implements DistritoService {

    @Autowired
    private DistritoRepository distritoRepository;

    @Override
    public List<Distrito> listarDistritos() {
        return distritoRepository.findAll();
    }

	@Override
	public Distrito obtenerPorId(Integer idDistrito) {
		
		return distritoRepository.findById(idDistrito).orElse(null);
	}
    
}
