package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.entity.PagoComisionVendedor;
import com.Inmobiliaria.demo.entity.ReciboEgreso;
import com.Inmobiliaria.demo.entity.SerieEgreso;
import com.Inmobiliaria.demo.entity.Voucher;
import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.PagoComisionVendedorRepository;
import com.Inmobiliaria.demo.repository.ReciboEgresoRepository;
import com.Inmobiliaria.demo.repository.SerieEgresoRepository;
import com.Inmobiliaria.demo.repository.VoucherRepository;
import com.Inmobiliaria.demo.service.ReciboEgresoService;
import com.Inmobiliaria.demo.util.ReciboEgresoPdf;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReciboEgresoServiceImpl implements ReciboEgresoService {

    public static final String SERIE_EGRESO = "EG01";
    public static final String ORIGEN_VOUCHER = "PAGO_COMISION";

    private final SerieEgresoRepository serieEgresoRepository;
    private final ReciboEgresoRepository reciboEgresoRepository;
    private final VoucherRepository voucherRepository;
    private final PagoComisionVendedorRepository pagoComisionRepository;
    private final Cloudinary cloudinary;

    @Override
    @Transactional
    public ReciboEgreso generarEgreso(
            String concepto,
            String beneficiario,
            Integer idContrato,
            BigDecimal monto,
            String moneda) {
        return generarEgreso(concepto, beneficiario, idContrato, monto, moneda,
                null, null, null);
    }

    @Override
    @Transactional
    public ReciboEgreso generarEgreso(
            String concepto,
            String beneficiario,
            Integer idContrato,
            BigDecimal monto,
            String moneda,
            MedioPago medioPago,
            String numeroOperacion,
            LocalDate fechaOperacion) {

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
        egreso.setMedioPago(medioPago != null ? medioPago.name() : null);
        egreso.setNumeroOperacion(numeroOperacion);
        egreso.setFechaOperacion(fechaOperacion);

        return reciboEgresoRepository.save(egreso);
    }

    @Override
    @Transactional
    public ReciboEgreso generarEgresoConVouchers(
            String concepto,
            String beneficiario,
            Integer idContrato,
            BigDecimal monto,
            String moneda,
            MedioPago medioPago,
            String numeroOperacion,
            LocalDate fechaOperacion,
            List<MultipartFile> vouchers) {
        return generarEgresoConVouchers(concepto, beneficiario, null, null,
                idContrato, monto, moneda, medioPago, numeroOperacion, fechaOperacion, vouchers);
    }

    @Override
    @Transactional
    public ReciboEgreso generarEgresoConVouchers(
            String concepto,
            String beneficiario,
            String dniBeneficiario,
            String usuarioRegistro,
            Integer idContrato,
            BigDecimal monto,
            String moneda,
            MedioPago medioPago,
            String numeroOperacion,
            LocalDate fechaOperacion,
            List<MultipartFile> vouchers) {

        ReciboEgreso egreso = generarEgreso(concepto, beneficiario, idContrato, monto,
                moneda, medioPago, numeroOperacion, fechaOperacion);
        egreso.setDniBeneficiario(dniBeneficiario);
        egreso.setUsuarioRegistro(usuarioRegistro);
        egreso = reciboEgresoRepository.save(egreso);

        if (vouchers != null && !vouchers.isEmpty()) {
            for (MultipartFile file : vouchers) {
                Voucher v = new Voucher();
                v.setTipoOrigen(ORIGEN_VOUCHER);
                v.setReferenciaId(egreso.getIdReciboEgreso().intValue());
                v.setUrl(subirImagen(file, idContrato));
                voucherRepository.save(v);
            }
        }
        return egreso;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarPdf(String numeroCompleto) {
        ReciboEgreso egreso = reciboEgresoRepository.findByNumeroCompleto(numeroCompleto)
                .orElseThrow(() -> new NegocioException("Recibo de egreso no encontrado: " + numeroCompleto));

        // Vouchers asociados al egreso (referenciaId = id del recibo de egreso)
        List<Voucher> vouchers = voucherRepository
                .findByTipoOrigenAndReferenciaId(ORIGEN_VOUCHER, egreso.getIdReciboEgreso().intValue());
        return ReciboEgresoPdf.generar(egreso, vouchers);
    }

    @Override
    @Transactional(readOnly = true)
    public String previewSiguienteNumero() {
        // Mayor entre el contador interno y el número más alto real en la BD
        int desdeContador = serieEgresoRepository.findBySerie(SERIE_EGRESO)
                .map(com.Inmobiliaria.demo.entity.SerieEgreso::getUltimoNumero)
                .orElse(0);
        Integer desdeTablaRaw = reciboEgresoRepository.findMaxNumeroBySerie(SERIE_EGRESO);
        int desdeTabla = (desdeTablaRaw != null) ? desdeTablaRaw : 0;
        int siguiente = Math.max(desdeContador, desdeTabla) + 1;
        return SERIE_EGRESO + "-" + siguiente;
    }

    private String subirImagen(MultipartFile file, Integer idContrato) throws RuntimeException {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
            String publicId = "egreso-" + timestamp;
            Map<?, ?> params = ObjectUtils.asMap(
                    "folder", "egresos/contrato-" + idContrato, "public_id", publicId);
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), params);
            return result.get("url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Error al subir el voucher a Cloudinary", e);
        }
    }
}