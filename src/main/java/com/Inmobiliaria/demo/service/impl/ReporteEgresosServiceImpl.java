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
import java.time.LocalDateTime;
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

            detalle.add(ResumenEgresoItemDTO.builder()
                    .numeroEgreso((String) f[0])
                    .serie((String) f[1])
                    .numero(f[2] != null ? ((Number) f[2]).intValue() : null)
                    .fechaEmision(aFecha(f[3]))
                    .concepto((String) f[4])
                    .beneficiario((String) f[5])
                    .idContrato(f[6] != null ? ((Number) f[6]).intValue() : null)
                    .monto(monto)
                    .moneda((String) f[8])
                    .medioPago((String) f[9])
                    .numeroOperacion((String) f[10])
                    .fechaOperacion(aFecha(f[11]))
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

    /**
     * Convierte el valor devuelto por la query nativa a LocalDate sin casts forzados.
     * La columna fecha_doc mezcla COALESCE(pcv.fecha_pago DATETIME, r.fecha_emision DATE),
     * así que JDBC puede devolver Timestamp, Date, LocalDate o LocalDateTime.
     */
    private static LocalDate aFecha(Object valor) {
        if (valor == null) return null;
        if (valor instanceof LocalDate ld) return ld;
        if (valor instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        if (valor instanceof java.sql.Date d) return d.toLocalDate();
        if (valor instanceof LocalDateTime ldt) return ldt.toLocalDate();
        if (valor instanceof java.util.Date d) return new java.sql.Date(d.getTime()).toLocalDate();
        return null;
    }
}