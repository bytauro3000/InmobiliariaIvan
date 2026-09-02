package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.entity.ReciboEgreso;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ReciboEgresoService {

    /** Genera un recibo de egreso con la serie EG01 (correlativo de 1 en 1). */
    ReciboEgreso generarEgreso(
            String concepto,
            String beneficiario,
            Integer idContrato,
            BigDecimal monto,
            String moneda);

    byte[] generarPdf(String numeroCompleto);
}