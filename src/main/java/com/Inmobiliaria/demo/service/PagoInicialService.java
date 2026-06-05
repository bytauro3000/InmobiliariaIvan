package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.PagoInicialResponseDTO;

import java.time.LocalDate;
import java.util.List;

public interface PagoInicialService {

    PagoInicialResponseDTO obtenerPorContrato(Integer idContrato);

    /** Anulación lógica. Solo ROLE_ADMINISTRADOR. */
    PagoInicialResponseDTO anularPagoInicial(Integer idContrato, String motivo, String anuladoPor);

    /** Elimina físicamente el pago inicial. Solo ROLE_ADMINISTRADOR. */
    void eliminarPagoInicial(Integer idContrato);

    /**
     * Listado general para el panel admin con filtros opcionales.
     * Cualquier parámetro puede ser null (sin filtro).
     */
    List<PagoInicialResponseDTO> listarTodos(
            String numeroComprobante,
            String manzana,
            String numeroLote,
            Integer idPrograma,
            LocalDate desde,
            LocalDate hasta);
}