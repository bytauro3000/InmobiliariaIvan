package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.entity.Cliente;
import com.Inmobiliaria.demo.entity.Comprobante;
import com.Inmobiliaria.demo.entity.Contrato;
import java.math.BigDecimal;
import java.util.Map;

public interface SunatIntegrationService {

    Map<String, Object> enviarBoleta(Cliente cliente, Contrato contrato,
                                     Comprobante comprobante, BigDecimal monto,
                                     String descripcionDetalle);

    Map<String, Object> enviarNotaCredito(Cliente cliente, Contrato contrato,
                                          Comprobante notaCredito,
                                          Comprobante comprobanteOriginal,
                                          BigDecimal monto,
                                          String descripcionDetalle,
                                          String codMotivo,
                                          String desMotivo);
}
