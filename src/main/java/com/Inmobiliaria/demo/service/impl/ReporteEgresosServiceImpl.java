package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.dto.ResumenEgresoItemDTO;
import com.Inmobiliaria.demo.dto.ResumenEgresosRangoDTO;
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

        // Usa fecha de PAGO para comisiones y fecha de EMISIÓN para otros egresos
        List<Object[]> filas = reciboEgresoRepository
                .findEgresosPorFechaPagoOrEmision(desde, hasta);

        List<ResumenEgresoItemDTO> detalle = new ArrayList<>();
        BigDecimal totalUsd = BigDecimal.ZERO;
        BigDecimal totalPen = BigDecimal.ZERO;

        for (Object[] f : filas) {
            BigDecimal monto = f[7] != null ? new BigDecimal(f[7].toString()) : BigDecimal.ZERO;
            if ("PEN".equalsIgnoreCase((String) f[8])) {
                totalPen = totalPen.add(monto);
            } else {
                totalUsd = totalUsd.add(monto);
            }

            LocalDate fechaDoc = f[3] != null ? ((java.sql.Date) f[3]).toLocalDate() : null;
            if (fechaDoc == null && f[3] instanceof LocalDate ld) {
                fechaDoc = ld;
            }

            detalle.add(ResumenEgresoItemDTO.builder()
                    .numeroEgreso((String) f[0])
                    .serie((String) f[1])
                    .numero(f[2] != null ? ((Number) f[2]).intValue() : null)
                    .fechaEmision(fechaDoc)
                    .concepto((String) f[4])
                    .beneficiario((String) f[5])
                    .idContrato(f[6] != null ? ((Number) f[6]).intValue() : null)
                    .monto(monto)
                    .moneda((String) f[8])
                    .medioPago((String) f[9])
                    .numeroOperacion((String) f[10])
                    .fechaOperacion(f[11] != null ? ((java.sql.Date) f[11]).toLocalDate() : null)
                    .usuarioRegistro((String) f[12])
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