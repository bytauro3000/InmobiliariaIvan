package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.entity.ReciboEgreso;
import com.Inmobiliaria.demo.enums.MedioPago;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ReciboEgresoService {

    /** Genera un recibo de egreso con la serie EG01 (correlativo de 1 en 1). */
    ReciboEgreso generarEgreso(
            String concepto,
            String beneficiario,
            Integer idContrato,
            BigDecimal monto,
            String moneda);

    /** Genera un recibo de egreso incluyendo medio de pago y datos de la operación. */
    ReciboEgreso generarEgreso(
            String concepto,
            String beneficiario,
            Integer idContrato,
            BigDecimal monto,
            String moneda,
            MedioPago medioPago,
            String numeroOperacion,
            LocalDate fechaOperacion);

    /** Genera un egreso y adjunta los vouchers (PDF con reverso). */
    ReciboEgreso generarEgresoConVouchers(
            String concepto,
            String beneficiario,
            Integer idContrato,
            BigDecimal monto,
            String moneda,
            MedioPago medioPago,
            String numeroOperacion,
            LocalDate fechaOperacion,
            List<MultipartFile> vouchers);

    byte[] generarPdf(String numeroCompleto);
}