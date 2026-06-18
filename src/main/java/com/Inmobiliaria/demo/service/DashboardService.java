package com.Inmobiliaria.demo.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DashboardService {
    List<Object[]> contarContratosPorProgramaYTipo();
    BigDecimal sumPagoLetrasByFecha(LocalDate fecha);
    long countPagoLetrasByFecha(LocalDate fecha);
    BigDecimal sumPagoMorasByFecha(LocalDate fecha);
    long countPagoMorasByFecha(LocalDate fecha);
    BigDecimal sumPagoInicialesByFecha(LocalDate fecha);
    long countPagoInicialesByFecha(LocalDate fecha);
}
