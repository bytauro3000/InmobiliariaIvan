package com.Inmobiliaria.demo.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.Inmobiliaria.demo.dto.IngresoMensualDTO;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import com.Inmobiliaria.demo.repository.PagoInicialRepository;
import com.Inmobiliaria.demo.repository.PagoLetraRepository;
import com.Inmobiliaria.demo.repository.PagoMoraRepository;
import com.Inmobiliaria.demo.service.DashboardService;

import lombok.RequiredArgsConstructor;

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

    @Override
    public List<IngresoMensualDTO> getIngresosPorMes(LocalDate desde, LocalDate hasta) {
        Map<String, IngresoMensualDTO> mapa = new HashMap<>();

        // Función helper para generar clave mes-año y obtener/acumular DTO
        // Formato de Object[] de los repositorios: [mes (int), anio (int), total (BigDecimal)]

        // 1. Pago letras
        for (Object[] fila : pagoLetraRepository.sumImportePagadoGroupedByMonth(desde, hasta)) {
            int mes = ((Number) fila[0]).intValue();
            int anio = ((Number) fila[1]).intValue();
            BigDecimal total = (BigDecimal) fila[2];
            String clave = anio + "-" + mes;
            IngresoMensualDTO dto = mapa.computeIfAbsent(clave, k -> crearVacio(mes, anio));
            dto.setTotalPagoLetras(total);
        }

        // 2. Pago moras
        for (Object[] fila : pagoMoraRepository.sumImportePagadoGroupedByMonth(desde, hasta)) {
            int mes = ((Number) fila[0]).intValue();
            int anio = ((Number) fila[1]).intValue();
            BigDecimal total = (BigDecimal) fila[2];
            String clave = anio + "-" + mes;
            IngresoMensualDTO dto = mapa.computeIfAbsent(clave, k -> crearVacio(mes, anio));
            dto.setTotalPagoMoras(total);
        }

        // 3. Pago iniciales
        for (Object[] fila : pagoInicialRepository.sumImportePagadoGroupedByMonth(desde, hasta)) {
            int mes = ((Number) fila[0]).intValue();
            int anio = ((Number) fila[1]).intValue();
            BigDecimal total = (BigDecimal) fila[2];
            String clave = anio + "-" + mes;
            IngresoMensualDTO dto = mapa.computeIfAbsent(clave, k -> crearVacio(mes, anio));
            dto.setTotalPagoIniciales(total);
        }

        // Calcular totalGeneral y generar etiquetas
        String[] mesesEspanol = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};

        return mapa.values().stream()
            .peek(dto -> {
                dto.setEtiqueta(mesesEspanol[dto.getMes() - 1] + " " + dto.getAnio());
                dto.setTotalGeneral(
                    dto.getTotalPagoLetras()
                        .add(dto.getTotalPagoMoras())
                        .add(dto.getTotalPagoIniciales())
                        .add(dto.getTotalInscripcionesServicios())
                );
            })
            .sorted((a, b) -> {
                int cmp = Integer.compare(a.getAnio(), b.getAnio());
                return cmp != 0 ? cmp : Integer.compare(a.getMes(), b.getMes());
            })
            .collect(Collectors.toList());
    }

    private IngresoMensualDTO crearVacio(int mes, int anio) {
        return IngresoMensualDTO.builder()
                .mes(mes)
                .anio(anio)
                .etiqueta("")
                .totalPagoLetras(BigDecimal.ZERO)
                .totalPagoMoras(BigDecimal.ZERO)
                .totalPagoIniciales(BigDecimal.ZERO)
                .totalInscripcionesServicios(BigDecimal.ZERO)
                .totalGeneral(BigDecimal.ZERO)
                .build();
    }
}
