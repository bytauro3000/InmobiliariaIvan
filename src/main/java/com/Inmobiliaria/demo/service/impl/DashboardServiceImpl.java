package com.Inmobiliaria.demo.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
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

        // ── Totales por fuente (letras, moras, iniciales) ────────────────
        // Object[]: [mes (int), anio (int), total (BigDecimal)]
        cargarTotalesPorFuente(mapa, pagoLetraRepository.sumImportePagadoGroupedByMonth(desde, hasta),
            (dto, total) -> dto.setTotalPagoLetras(total));
        cargarTotalesPorFuente(mapa, pagoMoraRepository.sumImportePagadoGroupedByMonth(desde, hasta),
            (dto, total) -> dto.setTotalPagoMoras(total));
        cargarTotalesPorFuente(mapa, pagoInicialRepository.sumImportePagadoGroupedByMonth(desde, hasta),
            (dto, total) -> dto.setTotalPagoIniciales(total));

        // ── Desglose por tipo de comprobante ─────────────────────────────
        // Object[]: [mes (int), anio (int), tipo (String: 'BOLETA'|'RECIBO'), total (BigDecimal)]
        cargarDesglose(mapa, pagoLetraRepository.sumByMonthAndComprobanteType(desde, hasta));
        cargarDesglose(mapa, pagoMoraRepository.sumByMonthAndComprobanteType(desde, hasta));
        cargarDesglose(mapa, pagoInicialRepository.sumByMonthAndComprobanteType(desde, hasta));

        // ── Desglose por medio de pago ───────────────────────────────────
        // Object[]: [mes (int), anio (int), tipo (String: 'EFECTIVO'|'BANCARIO'), total (BigDecimal)]
        cargarDesgloseMedioPago(mapa, pagoLetraRepository.sumByMonthAndMedioPago(desde, hasta));
        cargarDesgloseMedioPago(mapa, pagoMoraRepository.sumByMonthAndMedioPago(desde, hasta));
        cargarDesgloseMedioPago(mapa, pagoInicialRepository.sumByMonthAndMedioPago(desde, hasta));

        // ── Calcular totalGeneral y generar etiquetas ────────────────────
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

    @FunctionalInterface
    private interface SetterTotal {
        void set(IngresoMensualDTO dto, BigDecimal total);
    }

    private void cargarTotalesPorFuente(Map<String, IngresoMensualDTO> mapa,
                                          List<Object[]> resultados, SetterTotal setter) {
        for (Object[] fila : resultados) {
            int mes = ((Number) fila[0]).intValue();
            int anio = ((Number) fila[1]).intValue();
            BigDecimal total = (BigDecimal) fila[2];
            String clave = anio + "-" + mes;
            setter.set(mapa.computeIfAbsent(clave, k -> crearVacio(mes, anio)), total);
        }
    }

    private void cargarDesglose(Map<String, IngresoMensualDTO> mapa, List<Object[]> resultados) {
        for (Object[] fila : resultados) {
            int mes = ((Number) fila[0]).intValue();
            int anio = ((Number) fila[1]).intValue();
            String tipo = (String) fila[2];
            BigDecimal total = (BigDecimal) fila[3];
            String clave = anio + "-" + mes;
            IngresoMensualDTO dto = mapa.computeIfAbsent(clave, k -> crearVacio(mes, anio));
            if ("BOLETA".equals(tipo)) {
                dto.setTotalBoleta(dto.getTotalBoleta().add(total));
            } else {
                dto.setTotalRecibo(dto.getTotalRecibo().add(total));
            }
        }
    }

    private void cargarDesgloseMedioPago(Map<String, IngresoMensualDTO> mapa, List<Object[]> resultados) {
        for (Object[] fila : resultados) {
            int mes = ((Number) fila[0]).intValue();
            int anio = ((Number) fila[1]).intValue();
            String tipo = (String) fila[2];
            BigDecimal total = (BigDecimal) fila[3];
            String clave = anio + "-" + mes;
            IngresoMensualDTO dto = mapa.computeIfAbsent(clave, k -> crearVacio(mes, anio));
            if ("EFECTIVO".equals(tipo)) {
                dto.setTotalEfectivo(dto.getTotalEfectivo().add(total));
            } else {
                dto.setTotalBancario(dto.getTotalBancario().add(total));
            }
        }
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
                .totalBoleta(BigDecimal.ZERO)
                .totalRecibo(BigDecimal.ZERO)
                .totalEfectivo(BigDecimal.ZERO)
                .totalBancario(BigDecimal.ZERO)
                .build();
    }
}
