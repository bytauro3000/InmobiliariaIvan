package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.ResumenIngresosRangoDTO;

import java.time.LocalDate;

public interface ReporteIngresosService {

    ResumenIngresosRangoDTO obtenerIngresosPorRango(LocalDate desde, LocalDate hasta);
}