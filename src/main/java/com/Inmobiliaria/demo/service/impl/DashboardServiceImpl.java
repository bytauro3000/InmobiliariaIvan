package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.repository.ContratoRepository;
import com.Inmobiliaria.demo.repository.PagoInicialRepository;
import com.Inmobiliaria.demo.repository.PagoLetraRepository;
import com.Inmobiliaria.demo.repository.PagoMoraRepository;
import com.Inmobiliaria.demo.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ContratoRepository contratoRepository;
    private final PagoLetraRepository pagoLetraRepository;
    private final PagoMoraRepository pagoMoraRepository;
    private final PagoInicialRepository pagoInicialRepository;

    @Override
    public List<Object[]> contarContratosPorProgramaYTipo() {
        return contratoRepository.contarContratosPorProgramaYTipo();
    }

    @Override
    public BigDecimal sumPagoLetrasByFecha(LocalDate fecha) {
        return pagoLetraRepository.sumImportePagadoByFecha(fecha);
    }

    @Override
    public long countPagoLetrasByFecha(LocalDate fecha) {
        return pagoLetraRepository.countByFechaPago(fecha);
    }

    @Override
    public BigDecimal sumPagoMorasByFecha(LocalDate fecha) {
        return pagoMoraRepository.sumImportePagadoByFecha(fecha);
    }

    @Override
    public long countPagoMorasByFecha(LocalDate fecha) {
        return pagoMoraRepository.countByFechaPago(fecha);
    }

    @Override
    public BigDecimal sumPagoInicialesByFecha(LocalDate fecha) {
        return pagoInicialRepository.sumImportePagadoByFecha(fecha);
    }

    @Override
    public long countPagoInicialesByFecha(LocalDate fecha) {
        return pagoInicialRepository.countByFechaPago(fecha);
    }
}
