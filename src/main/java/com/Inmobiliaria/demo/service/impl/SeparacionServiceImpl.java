package com.Inmobiliaria.demo.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Inmobiliaria.demo.dto.SeparacionDTO;
import com.Inmobiliaria.demo.entity.Separacion;
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
<<<<<<< HEAD
    
    // Implementación del método buscarPorId
    @Override
    public Separacion buscarPorId(Integer idSeparacion) {
        return separacionRepository.findById(idSeparacion).orElse(null);
    }
=======

	@Override
	public List<Separacion> listadoSeparacion(){
		return separacionRepository.findAll();
	}

	@Override
	public Separacion obtenerPorId(Integer id) {
		return separacionRepository.findById(id).orElse(null);
	}

	@Override
	public Separacion crearSeparacion(Separacion reg) {
		return separacionRepository.save(reg);
	}

	@Override
	public void eliminarSeparacion(Integer id) {
		separacionRepository.deleteById(id);
		
	}

	@Override
	public Separacion actualizarSeparacion(Separacion reg) {
		// TODO Auto-generated method stub
		return separacionRepository.save(reg);
	}
>>>>>>> branch 'main' of https://github.com/bytauro3000/InmobiliariaIvan
}
