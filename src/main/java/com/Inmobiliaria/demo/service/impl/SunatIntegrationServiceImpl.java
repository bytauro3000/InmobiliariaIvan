package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.dto.sunat.BoletaRequest;
import com.Inmobiliaria.demo.dto.sunat.ClienteDto;
import com.Inmobiliaria.demo.dto.sunat.DetalleDto;
import com.Inmobiliaria.demo.entity.Cliente;
import com.Inmobiliaria.demo.entity.Comprobante;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.enums.TipoCliente;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.service.SunatIntegrationService;
import com.Inmobiliaria.demo.util.NumeroALetras;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SunatIntegrationServiceImpl implements SunatIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(SunatIntegrationServiceImpl.class);
    private static final String LEYENDA_DEFAULT = "OPERACION INAFECTA - VENTA DE TERRENO";

    private final RestTemplate restTemplate;
    private final String sunatSoapUrl;
    private final String gatewaySecretKey;

    public SunatIntegrationServiceImpl(
            @Value("${sunat.soap.url}") String sunatSoapUrl,
            @Value("${gateway.secret-key}") String gatewaySecretKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        this.restTemplate = new RestTemplate(factory);
        this.sunatSoapUrl = sunatSoapUrl;
        this.gatewaySecretKey = gatewaySecretKey;
    }

    @Override
    public Map<String, Object> enviarBoleta(Cliente cliente, Contrato contrato,
                                             Comprobante comprobante, BigDecimal monto,
                                             String descripcionDetalle) {
        BoletaRequest request = buildRequest(cliente, contrato, comprobante, monto, descripcionDetalle);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Gateway-Secret", gatewaySecretKey);
        HttpEntity<BoletaRequest> entity = new HttpEntity<>(request, headers);

        String url = sunatSoapUrl + "/api/sunat/enviar";
        log.info("Enviando boleta a SUNAT: {} {} - {}", comprobante.getSerie(), comprobante.getNumeroCompleto(), url);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            log.info("Respuesta SUNAT: {}", response.getStatusCode());
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Error HTTP {} del servicio SUNAT: {}", e.getStatusCode(), e.getResponseBodyAsString());
            Map<String, Object> error = new HashMap<>();
            error.put("codigo", "-1");
            error.put("mensaje", "Error del servicio SUNAT (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
            error.put("estadoSunat", "ERROR");
            try {
                String body = e.getResponseBodyAsString();
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> parsed = mapper.readValue(body, Map.class);
                error.putAll(parsed);
            } catch (Exception ignored) {}
            return error;
        } catch (Exception e) {
            log.error("Error al enviar boleta a SUNAT: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("codigo", "-1");
            error.put("mensaje", e.getMessage());
            error.put("estadoSunat", "ERROR");
            return error;
        }
    }

    private BoletaRequest buildRequest(Cliente cliente, Contrato contrato,
                                        Comprobante comprobante, BigDecimal monto,
                                        String descripcionDetalle) {
        Moneda moneda = contrato.getMoneda();
        String monedaCodigo = moneda != null ? moneda.name() : "PEN";

        ClienteDto clienteDto = ClienteDto.builder()
                .tipoDocumento(mapTipoDocumento(cliente.getTipoCliente()))
                .numeroDocumento(cliente.getNumDoc())
                .razonSocial(buildRazonSocial(cliente))
                .direccion(buildDireccion(cliente))
                .ubigeo(cliente.getDistrito() != null ? cliente.getDistrito().getCodigoUbigeo() : null)
                .email(cliente.getEmail())
                .build();

        List<DetalleDto> detalles = Collections.singletonList(DetalleDto.builder()
                .descripcion(descripcionDetalle)
                .cantidad(BigDecimal.ONE)
                .precioUnitario(monto)
                .subtotal(monto)
                .codigoAfectacionIgv("30")
                .build());

        BigDecimal totalGravado = BigDecimal.ZERO;
        BigDecimal totalIgv = BigDecimal.ZERO;

        String fechaStr = comprobante.getFechaEmision().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String horaStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String montoLetras = NumeroALetras.convertir(monto, moneda);

        return BoletaRequest.builder()
                .tipoDocumento(mapTipoComprobante(comprobante.getTipoComprobante()))
                .serie(comprobante.getSerie())
                .numero(comprobante.getNumero())
                .fechaEmision(fechaStr)
                .horaEmision(horaStr)
                .moneda(monedaCodigo)
                .cliente(clienteDto)
                .detalles(detalles)
                .totalGravado(totalGravado)
                .totalIgv(totalIgv)
                .total(monto)
                .leyenda(LEYENDA_DEFAULT)
                .montoLetras(montoLetras)
                .build();
    }

    private String mapTipoComprobante(TipoComprobante tipo) {
        return switch (tipo) {
            case BOLETA -> "03";
            case FACTURA -> "01";
            case RECIBO -> "03";
        };
    }

    private String mapTipoDocumento(TipoCliente tipoCliente) {
        if (tipoCliente == null) return "1";
        return switch (tipoCliente) {
            case NATURAL -> "1";
            case JURIDICO -> "6";
            case CE -> "7";
        };
    }

    private String buildRazonSocial(Cliente cliente) {
        if (cliente.getTipoCliente() == TipoCliente.JURIDICO) {
            return cliente.getNombre();
        }
        String nombre = cliente.getNombre() != null ? cliente.getNombre() : "";
        String apellidos = cliente.getApellidos() != null ? " " + cliente.getApellidos() : "";
        return (nombre + apellidos).trim();
    }

    private String buildDireccion(Cliente cliente) {
        StringBuilder dir = new StringBuilder(cliente.getDireccion() != null ? cliente.getDireccion() : "");
        if (cliente.getDistrito() != null) {
            dir.append(", ").append(cliente.getDistrito().getNombre());
            if (cliente.getDistrito().getProvincia() != null) {
                dir.append(", ").append(cliente.getDistrito().getProvincia());
            }
        }
        return dir.toString();
    }
}
