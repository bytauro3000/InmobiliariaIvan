package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.*;

import java.util.List;

public interface MoraService {

    /**
     * Calcula la mora para una letra vencida SIN guardarla.
     * Usado por el frontend para mostrar el preview antes de pagar.
     */
    CalculoMoraDTO calcularMora(Integer idLetra);

    /**
     * Retorna todas las moras (cualquier estado) de un contrato.
     */
    List<MoraResponseDTO> listarPorContrato(Integer idContrato);

    /**
     * Retorna solo las moras PENDIENTES de un contrato.
     */
    List<MoraResponseDTO> listarPendientesPorContrato(Integer idContrato);

    /**
     * Retorna las moras de una letra específica.
     */
    List<MoraResponseDTO> listarPorLetra(Integer idLetra);

    /**
     * Retorna una mora por su ID.
     */
    MoraResponseDTO obtenerPorId(Integer idMora);

    /**
     * Resumen de mora pendiente de un contrato (cantidad + total).
     */
    MoraResumenContratoDTO obtenerResumenPorContrato(Integer idContrato);

    /**
     * Registra el pago de una mora.
     * Cambia el estado de la mora a PAGADO.
     */
    PagoMoraResponseDTO pagarMora(PagoMoraRequestDTO request);

    /**
     * Anula una mora (estado ANULADO).
     * Solo para correcciones administrativas.
     */
    MoraResponseDTO anularMora(Integer idMora, String motivo);
    
    MoraResponseDTO crearMoraPendiente(Integer idLetra);
}