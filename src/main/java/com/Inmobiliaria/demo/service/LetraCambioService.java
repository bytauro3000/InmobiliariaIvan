package com.Inmobiliaria.demo.service;

import java.util.List;
import com.Inmobiliaria.demo.dto.GenerarLetrasRequest;
import com.Inmobiliaria.demo.dto.LetraCambioDTO;
import com.Inmobiliaria.demo.dto.ReporteLetraCambioDTO;


public interface LetraCambioService {
	
	List<LetraCambioDTO> listarPorContrato(Integer idContrato);
	
    List<ReporteLetraCambioDTO> obtenerReportePorContrato(Integer idContrato);
   
    void generarLetrasDesdeContrato(Integer idContrato, GenerarLetrasRequest generarLetrasRequest);
    
    LetraCambioDTO actualizarLetra(Integer id, LetraCambioDTO letraCambioDTO);
    
    void eliminarPorContrato(Integer idContrato);
}
