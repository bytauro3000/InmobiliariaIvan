package com.Inmobiliaria.demo.service;

import java.util.List;

import com.Inmobiliaria.demo.dto.LoteProgramaResponseDTO;
import com.Inmobiliaria.demo.entity.Lote;

public interface LoteService {

	List<Lote> listarLotes();
	
	List<Object[]> obtenerConteoPorEstadoYPrograma();
	
	List<LoteProgramaResponseDTO> listarLotesPorPrograma(Integer idPrograma);
	
	List<Lote> listarLotesPorProgramaGestion(Integer idPrograma);
	
	Lote actualizarLote(Lote reg);
	
	Lote obtenerLotePorId(Integer id);

	Lote crearLote(Lote reg);

	void eliminarLote(Integer id);
	
	List<Lote> buscarLotesPorGestion(Integer idPrograma, String manzana, String numeroLote);
	
	boolean existeLote(Integer idPrograma, String manzana, String numeroLote);
}
