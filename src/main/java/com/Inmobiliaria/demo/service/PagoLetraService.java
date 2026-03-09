package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.PagoLetraRequestDTO;
import com.Inmobiliaria.demo.dto.PagoLetraResponseDTO;
import com.Inmobiliaria.demo.dto.PagosMultiplesRequestDTO;
import com.Inmobiliaria.demo.dto.SugerenciaNumeroComprobanteDTO;
import com.Inmobiliaria.demo.enums.TipoComprobante;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface PagoLetraService {

    List<PagoLetraResponseDTO> listarPorContrato(Integer idContrato);

    List<PagoLetraResponseDTO> listarPorLetra(Integer idLetra);

    PagoLetraResponseDTO obtenerPorId(Integer idPago);

    PagoLetraResponseDTO registrarPago(PagoLetraRequestDTO request, List<MultipartFile> vouchers) throws IOException;

    PagoLetraResponseDTO actualizarPago(Integer idPago, PagoLetraRequestDTO request, List<MultipartFile> vouchers) throws IOException;

    void eliminarPago(Integer idPago) throws IOException;

    List<PagoLetraResponseDTO> registrarPagosMultiples(PagosMultiplesRequestDTO request, List<MultipartFile> vouchers) throws IOException;
    
    SugerenciaNumeroComprobanteDTO sugerirNumeroComprobante(TipoComprobante tipoComprobante);
}