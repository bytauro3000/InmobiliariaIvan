package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface MoraService {

    /** Calcula mora usando la fecha actual como referencia. */
    CalculoMoraDTO calcularMora(Integer idLetra);

    /** Calcula mora usando una fecha de referencia específica (ej: fecha de operación del pago). */
    CalculoMoraDTO calcularMora(Integer idLetra, LocalDate fechaReferencia);
    List<MoraResponseDTO> listarPorContrato(Integer idContrato);
    List<MoraResponseDTO> listarPendientesPorContrato(Integer idContrato);
    List<MoraResponseDTO> listarPorLetra(Integer idLetra);
    MoraResponseDTO obtenerPorId(Integer idMora);
    MoraResumenContratoDTO obtenerResumenPorContrato(Integer idContrato);
    PagoMoraResponseDTO pagarMora(PagoMoraRequestDTO request, List<MultipartFile> vouchers) throws IOException;
    MoraResponseDTO anularMora(Integer idMora, String motivo);
    MoraResponseDTO crearMoraPendiente(Integer idLetra);
}