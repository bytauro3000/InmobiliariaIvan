package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.dto.ResumenEgresoItemDTO;
import com.Inmobiliaria.demo.dto.ResumenEgresosRangoDTO;
import com.Inmobiliaria.demo.entity.ReciboEgreso;
import com.Inmobiliaria.demo.repository.ReciboEgresoRepository;
import com.Inmobiliaria.demo.service.ReporteEgresosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteEgresosServiceImpl implements ReporteEgresosService {

    private final ReciboEgresoRepository reciboEgresoRepository;

    @Override
    @Transactional(readOnly = true)
    public ResumenEgresosRangoDTO obtenerEgresosPorRango(LocalDate desde, LocalDate hasta) {

        List<ReciboEgreso> egresos = reciboEgresoRepository
                .findByFechaEmisionBetweenOrderByNumeroAsc(desde, hasta);

        List<ResumenEgresoItemDTO> detalle = new ArrayList<>();
        BigDecimal totalUsd = BigDecimal.ZERO;
        BigDecimal totalPen = BigDecimal.ZERO;

        for (ReciboEgreso e : egresos) {
            BigDecimal monto = e.getMonto() != null ? e.getMonto() : BigDecimal.ZERO;
            if ("PEN".equalsIgnoreCase(e.getMoneda())) {
                totalPen = totalPen.add(monto);
            } else {
                totalUsd = totalUsd.add(monto);
            }

            detalle.add(ResumenEgresoItemDTO.builder()
                    .numeroEgreso(e.getNumeroCompleto())
                    .serie(e.getSerie())
                    .numero(e.getNumero())
                    .fechaEmision(e.getFechaEmision())
                    .concepto(e.getConcepto())
                    .beneficiario(e.getBeneficiario())
                    .idContrato(e.getIdContrato())
                    .monto(monto)
                    .moneda(e.getMoneda())
                    .medioPago(e.getMedioPago())
                    .numeroOperacion(e.getNumeroOperacion())
                    .fechaOperacion(e.getFechaOperacion())
                    .usuarioRegistro(e.getUsuarioRegistro())
                    .build());
        }

        return ResumenEgresosRangoDTO.builder()
                .fechaDesde(desde)
                .fechaHasta(hasta)
                .totalUsd(totalUsd)
                .totalPen(totalPen)
                .totalGeneral(totalUsd.add(totalPen))
                .cantidadTotal(detalle.size())
                .detalle(detalle)
                .build();
    }
}