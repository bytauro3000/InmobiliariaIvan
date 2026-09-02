package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.entity.ReciboEgreso;
import com.Inmobiliaria.demo.entity.SerieEgreso;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.ReciboEgresoRepository;
import com.Inmobiliaria.demo.repository.SerieEgresoRepository;
import com.Inmobiliaria.demo.service.ReciboEgresoService;
import com.Inmobiliaria.demo.util.ReciboEgresoPdf;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReciboEgresoServiceImpl implements ReciboEgresoService {

    public static final String SERIE_EGRESO = "EG01";

    private final SerieEgresoRepository serieEgresoRepository;
    private final ReciboEgresoRepository reciboEgresoRepository;

    @Override
    @Transactional
    public ReciboEgreso generarEgreso(
            String concepto,
            String beneficiario,
            Integer idContrato,
            BigDecimal monto,
            String moneda) {

        // ── 1. Bloquear el contador de la serie (SELECT FOR UPDATE) ───────────
        SerieEgreso contador = serieEgresoRepository
                .findBySerieForUpdate(SERIE_EGRESO)
                .orElseThrow(() -> new NegocioException(
                        "No existe serie configurada para egresos (" + SERIE_EGRESO
                        + "). Ejecute el SQL de inicialización de serie_egreso."));

        // ── 2. Incrementar el contador ────────────────────────────────────────
        int nuevoNumero = contador.getUltimoNumero() + 1;
        contador.setUltimoNumero(nuevoNumero);
        serieEgresoRepository.save(contador);

        // ── 3. Crear y persistir el recibo de egreso ──────────────────────────
        ReciboEgreso egreso = new ReciboEgreso();
        egreso.setSerie(SERIE_EGRESO);
        egreso.setNumero(nuevoNumero);
        egreso.setNumeroCompleto(SERIE_EGRESO + "-" + nuevoNumero);
        egreso.setFechaEmision(LocalDate.now());
        egreso.setConcepto(concepto);
        egreso.setBeneficiario(beneficiario);
        egreso.setIdContrato(idContrato);
        egreso.setMonto(monto);
        egreso.setMoneda(moneda);

        return reciboEgresoRepository.save(egreso);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarPdf(String numeroCompleto) {
        ReciboEgreso egreso = reciboEgresoRepository.findByNumeroCompleto(numeroCompleto)
                .orElseThrow(() -> new NegocioException("Recibo de egreso no encontrado: " + numeroCompleto));
        return ReciboEgresoPdf.generar(egreso);
    }
}