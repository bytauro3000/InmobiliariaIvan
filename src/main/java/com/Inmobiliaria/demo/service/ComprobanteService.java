package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.ComprobanteResponseDTO;
import com.Inmobiliaria.demo.entity.Comprobante;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.enums.TipoOrigenComprobante;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ComprobanteService {

    Comprobante generarComprobante(
            TipoComprobante tipoComprobante,
            TipoOrigenComprobante tipoOrigen,
            Integer referenciaId,
            BigDecimal monto,
            LocalDate fechaEmision);

    Comprobante generarComprobanteConNumero(
            TipoComprobante tipoComprobante,
            TipoOrigenComprobante tipoOrigen,
            Integer referenciaId,
            BigDecimal monto,
            LocalDate fechaEmision,
            String numeroPersonalizado);
    ComprobanteResponseDTO obtenerPorId(Long idComprobante);
    ComprobanteResponseDTO obtenerPorNumeroCompleto(String numeroCompleto);
    List<ComprobanteResponseDTO> listarPorTipo(TipoComprobante tipoComprobante);
    List<ComprobanteResponseDTO> listarPorOrigen(TipoOrigenComprobante tipoOrigen);
    List<ComprobanteResponseDTO> listarPorRangoFecha(LocalDate desde, LocalDate hasta);
    String previewSiguienteNumero(TipoComprobante tipoComprobante);

    void eliminarComprobante(Long idComprobante);
}