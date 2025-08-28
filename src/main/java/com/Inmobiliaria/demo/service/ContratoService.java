// ContratoService.java
package com.Inmobiliaria.demo.service;

import java.security.Principal;
import java.util.List;

import com.Inmobiliaria.demo.dto.ContratoRequestDTO; // 👈 Importa el DTO de entrada
import com.Inmobiliaria.demo.dto.ContratoResponseDTO;


public interface ContratoService {
    
    ContratoResponseDTO guardarContrato(ContratoRequestDTO requestDTO, Principal principal);
    
    List<ContratoResponseDTO> listarContratos();

    void eliminarContrato(Integer idContrato);
    
 
    ContratoResponseDTO buscarPorId(Integer idContrato);
}