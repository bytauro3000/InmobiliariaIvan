package com.Inmobiliaria.demo.service;

import java.util.List;
import com.Inmobiliaria.demo.dto.GenerarLetrasRequest;
import com.Inmobiliaria.demo.dto.LetraCambioDTO;


public interface LetraCambioService {
	List<LetraCambioDTO> listarPorContrato(Integer idContrato);

    // Método corregido para aceptar un DTO
    void generarLetrasDesdeContrato(Integer idContrato, GenerarLetrasRequest generarLetrasRequest);
}
