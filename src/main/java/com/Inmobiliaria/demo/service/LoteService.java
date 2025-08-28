package com.Inmobiliaria.demo.service;

import java.util.List;

import com.Inmobiliaria.demo.entity.Lote;

public interface LoteService {

	List<Lote> listarLotes();
	
	List<Lote> listarLotesPorPrograma(Integer idPrograma);
	
	Lote actualizarLote(Lote reg);
	
	Lote obtenerLotePorId(Integer id);

	Lote crearLote(Lote reg);

	void eliminarLote(Integer id);
}
