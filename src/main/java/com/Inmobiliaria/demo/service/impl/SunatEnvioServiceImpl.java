package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.entity.Cliente;
import com.Inmobiliaria.demo.entity.Comprobante;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.service.SunatApiSunatClient;
import com.Inmobiliaria.demo.service.SunatEnvioService;
import com.Inmobiliaria.demo.service.SunatIntegrationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SunatEnvioServiceImpl implements SunatEnvioService {

    private static final Logger log = LoggerFactory.getLogger(SunatEnvioServiceImpl.class);

    /** Proveedor de facturacion: apisperu (default) o apisunat. Se lee de env (SUNAT_PROVIDER). */
    @Value("${sunat.provider:apisperu}")
    private String sunatProvider;

    private final SunatIntegrationService sunatIntegrationService;
    private final SunatApiSunatClient sunatApiSunatClient;

    @Override
    public Map<String, Object> enviarBoleta(Cliente cliente, Contrato contrato,
                                            Comprobante comprobante, BigDecimal monto,
                                            String descripcionDetalle) {
        log.info("Enviando comprobante {} a SUNAT (proveedor={})...",
                comprobante.getNumeroCompleto(), sunatProvider);

        Map<String, Object> respuesta;
        if ("apisunat".equalsIgnoreCase(sunatProvider)) {
            respuesta = sunatApiSunatClient.enviarBoleta(
                    cliente, contrato, comprobante, monto, descripcionDetalle);
        } else {
            respuesta = sunatIntegrationService.enviarBoleta(
                    cliente, contrato, comprobante, monto, descripcionDetalle);
        }

        String estado = respuesta != null ? (String) respuesta.get("estadoSunat") : "ERROR";
        if ("ERROR".equals(estado)) {
            String msg = respuesta != null
                    ? (String) respuesta.getOrDefault("mensaje", "Error desconocido de SUNAT")
                    : "No se obtuvo respuesta del servicio SUNAT";
            log.error("SUNAT rechazo comprobante {}: {}", comprobante.getNumeroCompleto(), msg);
            throw new NegocioException("SUNAT rechazó la boleta: " + msg);
        }

        log.info("SUNAT acepto comprobante {} correctamente", comprobante.getNumeroCompleto());
        return respuesta;
    }

    @Override
    public Map<String, Object> enviarNotaCredito(Cliente cliente, Contrato contrato,
                                                  Comprobante notaCredito,
                                                  Comprobante comprobanteOriginal,
                                                  BigDecimal monto,
                                                  String descripcionDetalle,
                                                  String codMotivo,
                                                  String desMotivo) {
        log.info("Enviando nota de credito {} a SUNAT (anula {})...",
                notaCredito.getNumeroCompleto(), comprobanteOriginal.getNumeroCompleto());

        Map<String, Object> respuesta = sunatIntegrationService.enviarNotaCredito(
                cliente, contrato, notaCredito, comprobanteOriginal, monto,
                descripcionDetalle, codMotivo, desMotivo);

        String estado = respuesta != null ? (String) respuesta.get("estadoSunat") : "ERROR";
        if ("ERROR".equals(estado)) {
            String msg = respuesta != null
                    ? (String) respuesta.getOrDefault("mensaje", "Error desconocido de SUNAT")
                    : "No se obtuvo respuesta del servicio SUNAT";
            log.error("SUNAT rechazo nota de credito {}: {}", notaCredito.getNumeroCompleto(), msg);
            throw new NegocioException("SUNAT rechazó la nota de crédito: " + msg);
        }

        log.info("SUNAT acepto nota de credito {} correctamente", notaCredito.getNumeroCompleto());
        return respuesta;
    }
}
