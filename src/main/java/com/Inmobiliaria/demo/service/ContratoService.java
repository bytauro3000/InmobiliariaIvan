package com.Inmobiliaria.demo.service;

import java.security.Principal;
import java.util.List;

import com.Inmobiliaria.demo.dto.TransferenciaResponseDTO;

import com.Inmobiliaria.demo.dto.ContratoRequestDTO; // 👈 Importa el DTO de entrada
import com.Inmobiliaria.demo.dto.ContratoResponseDTO;


public interface ContratoService {
    
    ContratoResponseDTO guardarContrato(ContratoRequestDTO requestDTO, Principal principal);
    
    List<ContratoResponseDTO> listarContratos();

    void eliminarContrato(Integer idContrato);
 
    ContratoResponseDTO buscarPorId(Integer idContrato);
    
 // Método para generar el PDF desde el Service
    byte[] generarPdf(Integer idContrato);
    
    ContratoResponseDTO actualizarContrato(Integer id, ContratoRequestDTO requestDTO);
    
    ContratoResponseDTO buscarPorProgramaManzanaLote(Integer idPrograma, String manzana, String numeroLote);

    ContratoResponseDTO cambiarEstado(Integer idContrato, String nuevoEstado);

    /**
     * Marca el contrato como RENUNCIA.
     * Libera el/los lotes a Disponible y cancela las letras pendientes.
     */
    ContratoResponseDTO registrarRenuncia(Integer idContrato);

    /**
     * Marca el contrato como TRANSFERIDO.
     * Devuelve los datos calculados para pre-llenar el nuevo contrato.
     */
    TransferenciaResponseDTO registrarTransferencia(Integer idContrato);
    
    List<ContratoResponseDTO> buscarPorNombreCliente(String termino);
}