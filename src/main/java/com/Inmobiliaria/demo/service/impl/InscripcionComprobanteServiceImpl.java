package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.entity.PagoInscripcionComprobante;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.PagoInscripcionComprobanteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class InscripcionComprobanteServiceImpl {

    private final PagoInscripcionComprobanteRepository pagoInscripcionComprobanteRepository;

    @Transactional(readOnly = true)
    public PagoInscripcionComprobante obtenerPagoConDetalle(Integer idPago) {


        PagoInscripcionComprobante pago = pagoInscripcionComprobanteRepository
                .findByIdConClientesYComprobante(idPago)
                .orElseThrow(() -> new NegocioException(
                        "Pago de inscripcion no encontrado con ID: " + idPago));

        pagoInscripcionComprobanteRepository.findByIdConLotes(idPago);

        return pago;
    }
}