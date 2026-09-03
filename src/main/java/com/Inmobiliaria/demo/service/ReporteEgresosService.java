package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.ResumenEgresosRangoDTO;

import java.time.LocalDate;

public interface ReporteEgresosService {

    /** Resumen de egresos (recibos EG01) por rango de fechas. */
    ResumenEgresosRangoDTO obtenerEgresosPorRango(LocalDate desde, LocalDate hasta);
}