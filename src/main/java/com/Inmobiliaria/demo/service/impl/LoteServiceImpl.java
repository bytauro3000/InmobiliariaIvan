package com.Inmobiliaria.demo.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Inmobiliaria.demo.entity.Lote;
import com.Inmobiliaria.demo.repository.LoteRepository;
import com.Inmobiliaria.demo.service.LoteService;

@Service
public class LoteServiceImpl implements LoteService{
	@Autowired
	private LoteRepository loteRepository;

	@Override
	public List<Lote> listarLotes() {
		return loteRepository.findAll();
	}
	
	@Override
    public List<Lote> listarLotesPorPrograma(Integer idPrograma) {
        return loteRepository.findByProgramaIdPrograma(idPrograma);
    }

    @Override
    public Lote actualizarLote(Lote lote) {
return loteRepository.save(lote);
    }

	@Override
	public Lote obtenerLotePorId(Integer id) {
		return loteRepository.findById(id).orElse(null);
	}

	@Override
	public Lote crearLote(Lote reg) {
 return loteRepository.save(reg);
	}

	@Override
	public void eliminarLote(Integer id) {
		 loteRepository.deleteById(id);
	}
	
	
}
