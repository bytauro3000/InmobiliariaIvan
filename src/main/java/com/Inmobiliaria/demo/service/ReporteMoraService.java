package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.DetalleLetraVencidaDTO;
import com.Inmobiliaria.demo.dto.ReporteClientesMoraDTO;

import java.util.List;

public interface ReporteMoraService {

    List<ReporteClientesMoraDTO> obtenerClientesEnMora();

    List<ReporteClientesMoraDTO> obtenerClientesLetrasVencidas();

    List<DetalleLetraVencidaDTO> obtenerDetalleLetrasVencidas(Integer idContrato);

    byte[] generarPdfClientesEnMora();
}