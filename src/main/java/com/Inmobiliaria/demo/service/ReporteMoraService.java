package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.ReporteClientesMoraDTO;

import java.util.List;

public interface ReporteMoraService {

    List<ReporteClientesMoraDTO> obtenerClientesEnMora();

    byte[] generarPdfClientesEnMora();
}