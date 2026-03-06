package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.PagoLetraRequestDTO;
import com.Inmobiliaria.demo.dto.PagoLetraResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface PagoLetraService {

    List<PagoLetraResponseDTO> listarPorContrato(Integer idContrato);

    List<PagoLetraResponseDTO> listarPorLetra(Integer idLetra);

    PagoLetraResponseDTO obtenerPorId(Integer idPago);

    PagoLetraResponseDTO registrarPago(PagoLetraRequestDTO request, MultipartFile voucher) throws IOException;

    PagoLetraResponseDTO actualizarPago(Integer idPago, PagoLetraRequestDTO request, MultipartFile voucher) throws IOException;

    void eliminarPago(Integer idPago);
}