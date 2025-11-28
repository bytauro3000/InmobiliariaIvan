package com.Inmobiliaria.demo.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Inmobiliaria.demo.dto.LoteProgramaResponseDTO;
import com.Inmobiliaria.demo.entity.Lote;
import com.Inmobiliaria.demo.enums.EstadoLote;
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
	public List<LoteProgramaResponseDTO> listarLotesPorPrograma(Integer idPrograma) {
	    // ✅ MODIFICACIÓN: Llamas al nuevo método del repositorio que filtra por estado.
		List<Lote> lotes = loteRepository.findByProgramaIdProgramaAndEstadoEquals(idPrograma, EstadoLote.Disponible);
	    return lotes.stream().map(lote -> {
	        LoteProgramaResponseDTO dto = new LoteProgramaResponseDTO();
	        dto.setIdLote(lote.getIdLote());
	        dto.setManzana(lote.getManzana());
	        dto.setNumeroLote(lote.getNumeroLote());
	        dto.setArea(lote.getArea());

	        if (lote.getPrograma() != null) {
	            dto.setIdPrograma(lote.getPrograma().getIdPrograma());
	            dto.setNombrePrograma(lote.getPrograma().getNombrePrograma());
	            dto.setPrecioM2(lote.getPrograma().getPrecioM2());
	        }
	        return dto;
	    }).toList();
	}


    @Override
    public Lote actualizarLote(Lote lote) {
    	
    	Lote loteAct = obtenerLotePorId(lote.getIdLote());
    	if (loteAct.getEstado() == EstadoLote.Separado) {
    		return null;
    	}else {
    		return loteRepository.save(lote);
    	}
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
