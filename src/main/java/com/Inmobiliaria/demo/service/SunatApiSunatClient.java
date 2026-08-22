package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.apisunat.ApiSunatBoletaRequest;
import com.Inmobiliaria.demo.dto.apisunat.ApiSunatBoletaResponse;
import com.Inmobiliaria.demo.dto.apisunat.ApiSunatCreditNoteRequest;
import com.Inmobiliaria.demo.entity.Cliente;
import com.Inmobiliaria.demo.entity.Comprobante;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.enums.TipoCliente;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Cliente de la API SUNAT propia (plataforma multi-tenant desplegada aparte).
 *
 * Envia boletas a POST /api/v1/boletas identificandose como la empresa del tenant
 * mediante las cabeceras X-Api-Key / X-Api-Secret (cada empresa tiene las suyas).
 * El envio a SUNAT es asincrono en la plataforma: aqui solo se registra la boleta
 * (estado ENVIADO); el CDR/aceptacion se consulta despues en la propia plataforma.
 */
@Service
public class SunatApiSunatClient {

    private static final Logger log = LoggerFactory.getLogger(SunatApiSunatClient.class);
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RestTemplate restTemplate = buildRestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${apisunat.base-url:https://api-sunat-4c91.onrender.com/api/v1}")
    private String baseUrl;

    @Value("${apisunat.api-key:}")
    private String apiKey;

    @Value("${apisunat.api-secret:}")
    private String apiSecret;

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        return new RestTemplate(factory);
    }

    /**
     * Registra una boleta en la API SUNAT propia (que luego la envia a SUNAT).
     * Devuelve un Map con estadoSunat = ENVIADO | ACEPTADA | ERROR y el mensaje.
     */
    public Map<String, Object> enviarBoleta(Cliente cliente, Contrato contrato,
                                            Comprobante comprobante, BigDecimal monto,
                                            String descripcionDetalle) {
        ApiSunatBoletaRequest request = buildRequest(cliente, contrato, comprobante, monto, descripcionDetalle);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", apiKey);
        headers.set("X-Api-Secret", apiSecret);
        HttpEntity<ApiSunatBoletaRequest> entity = new HttpEntity<>(request, headers);

        String url = baseUrl + "/boletas";
        log.info("Enviando boleta {} a API SUNAT: {}", comprobante.getNumeroCompleto(), url);

        try {
            ResponseEntity<ApiSunatBoletaResponse> response =
                    restTemplate.postForEntity(url, entity, ApiSunatBoletaResponse.class);
            return parseSuccess(response);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("API SUNAT respondio error: status={}, body={}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            return error(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error al enviar boleta a API SUNAT: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("estadoSunat", "ERROR");
            result.put("mensaje", e.getMessage());
            return result;
        }
    }

    /**
     * Registra una nota de crédito en la API SUNAT propia (que luego la envia a SUNAT).
     * Solo aplica a notas de crédito contra boletas (doc_afectado_tipo = 03).
     * Devuelve un Map con estadoSunat = ENVIADO | ACEPTADA | ERROR y el mensaje.
     */
    public Map<String, Object> enviarNotaCredito(Cliente cliente, Contrato contrato,
                                                 Comprobante notaCredito,
                                                 Comprobante comprobanteOriginal,
                                                 BigDecimal monto,
                                                 String descripcionDetalle,
                                                 String codMotivo,
                                                 String desMotivo) {
        ApiSunatCreditNoteRequest request = buildCreditNoteRequest(
                cliente, contrato, notaCredito, comprobanteOriginal, monto, descripcionDetalle, codMotivo, desMotivo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", apiKey);
        headers.set("X-Api-Secret", apiSecret);
        HttpEntity<ApiSunatCreditNoteRequest> entity = new HttpEntity<>(request, headers);

        String url = baseUrl + "/notas-credito";
        log.info("Enviando nota de credito {} a API SUNAT (anula {}): {}",
                notaCredito.getNumeroCompleto(), comprobanteOriginal.getNumeroCompleto(), url);

        try {
            ResponseEntity<ApiSunatBoletaResponse> response =
                    restTemplate.postForEntity(url, entity, ApiSunatBoletaResponse.class);
            return parseSuccess(response);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("API SUNAT respondio error: status={}, body={}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            return error(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error al enviar nota de credito a API SUNAT: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("estadoSunat", "ERROR");
            result.put("mensaje", e.getMessage());
            return result;
        }
    }

    private ApiSunatCreditNoteRequest buildCreditNoteRequest(Cliente cliente, Contrato contrato,
                                                             Comprobante notaCredito,
                                                             Comprobante comprobanteOriginal,
                                                             BigDecimal monto,
                                                             String descripcionDetalle,
                                                             String codMotivo,
                                                             String desMotivo) {
        String moneda = contrato.getMoneda() != null ? contrato.getMoneda().name() : "PEN";
        String fecha = notaCredito.getFechaEmision() instanceof LocalDate
                ? notaCredito.getFechaEmision().format(FECHA_FMT)
                : String.valueOf(notaCredito.getFechaEmision());

        ApiSunatCreditNoteRequest.Cliente clienteApi = ApiSunatCreditNoteRequest.Cliente.builder()
                .tipoDoc(mapTipoDocumento(cliente.getTipoCliente()))
                .numDoc(cliente.getNumDoc())
                .razonSocial(buildRazonSocial(cliente))
                .direccion(cliente.getDireccion())
                .build();

        ApiSunatCreditNoteRequest.Item item = ApiSunatCreditNoteRequest.Item.builder()
                .descripcion(descripcionDetalle)
                .unidad("NIU")
                .cantidad(BigDecimal.ONE)
                .precioUnitario(monto)
                .tipAfeIgv("30") // inafecto — venta de terreno
                .build();

        return ApiSunatCreditNoteRequest.builder()
                .serie(notaCredito.getSerie())
                .correlativo(notaCredito.getNumero())
                .fechaEmision(fecha)
                .tipoMoneda(moneda)
                .enviarAutomatico(Boolean.TRUE)
                .cliente(clienteApi)
                .docAfectadoTipo("03") // boleta — las NC de MERRUIC siempre son contra boletas
                .docAfectadoSerie(comprobanteOriginal.getSerie())
                .docAfectadoCorrelativo(String.valueOf(comprobanteOriginal.getNumero()))
                .codMotivo(codMotivo)
                .desMotivo(desMotivo)
                .items(Collections.singletonList(item))
                .build();
    }

    private Map<String, Object> parseSuccess(ResponseEntity<ApiSunatBoletaResponse> response) {
        Map<String, Object> result = new HashMap<>();
        ApiSunatBoletaResponse body = response.getBody();

        if (!response.getStatusCode().is2xxSuccessful() || body == null) {
            result.put("estadoSunat", "ERROR");
            result.put("mensaje", "Error HTTP: " + response.getStatusCode());
            return result;
        }

        ApiSunatBoletaResponse.Datos datos = body.getDatos();
        ApiSunatBoletaResponse.Sunat sunat = datos != null ? datos.getSunat() : null;
        String estado = sunat != null ? sunat.getEstado() : null;
        String numero = datos != null ? datos.getNumeroCompleto() : null;
        String mensaje = body.getMensaje() != null
                ? body.getMensaje()
                : "Boleta creada y encolada en API SUNAT";

        if ("aceptado".equalsIgnoreCase(estado)) {
            result.put("estadoSunat", "ACEPTADA");
        } else if ("rechazado".equalsIgnoreCase(estado)) {
            result.put("estadoSunat", "ERROR");
            if (sunat != null && sunat.getDescripcion() != null) {
                mensaje = sunat.getDescripcion();
            }
        } else {
            // "enviado" (o cualquier otro) → la plataforma la manda a SUNAT en segundo plano
            result.put("estadoSunat", "ENVIADO");
        }

        result.put("mensaje", mensaje);
        result.put("numeroCompleto", numero);
        if (datos != null) {
            result.put("apiSunatId", datos.getId());
        }

        log.info("API SUNAT resultado: estado={}, numero={}, mensaje={}", estado, numero, mensaje);
        return result;
    }

    private Map<String, Object> error(String responseBody) {
        Map<String, Object> result = new HashMap<>();
        result.put("estadoSunat", "ERROR");
        result.put("mensaje", extraerMensajeError(responseBody));
        return result;
    }

    private String extraerMensajeError(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "Error de API SUNAT";
        }
        try {
            Map<?, ?> parsed = objectMapper.readValue(responseBody, Map.class);
            if (parsed.get("mensaje") != null) {
                return parsed.get("mensaje").toString();
            }
            if (parsed.get("errores") != null) {
                return parsed.get("errores").toString();
            }
        } catch (Exception ignored) {
            // se devuelve el body crudo
        }
        return responseBody;
    }

    private ApiSunatBoletaRequest buildRequest(Cliente cliente, Contrato contrato,
                                               Comprobante comprobante, BigDecimal monto,
                                               String descripcionDetalle) {
        String moneda = contrato.getMoneda() != null ? contrato.getMoneda().name() : "PEN";
        String fecha = comprobante.getFechaEmision() instanceof LocalDate
                ? comprobante.getFechaEmision().format(FECHA_FMT)
                : String.valueOf(comprobante.getFechaEmision());

        ApiSunatBoletaRequest.Cliente clienteApi = ApiSunatBoletaRequest.Cliente.builder()
                .tipoDoc(mapTipoDocumento(cliente.getTipoCliente()))
                .numDoc(cliente.getNumDoc())
                .razonSocial(buildRazonSocial(cliente))
                .direccion(cliente.getDireccion())
                .build();

        ApiSunatBoletaRequest.Item item = ApiSunatBoletaRequest.Item.builder()
                .descripcion(descripcionDetalle)
                .unidad("NIU")
                .cantidad(BigDecimal.ONE)
                .precioUnitario(monto)
                .tipAfeIgv("30") // inafecto — venta de terreno
                .build();

        return ApiSunatBoletaRequest.builder()
                .serie(comprobante.getSerie())
                .correlativo(comprobante.getNumero())
                .fechaEmision(fecha)
                .tipoMoneda(moneda)
                .formaPago("Contado")
                .enviarAutomatico(Boolean.TRUE)
                .cliente(clienteApi)
                .items(Collections.singletonList(item))
                .build();
    }

    private String mapTipoDocumento(TipoCliente tipoCliente) {
        if (tipoCliente == null) {
            return "1";
        }
        return switch (tipoCliente) {
            case NATURAL -> "1";
            case JURIDICO -> "6";
            case CE -> "4";
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
}
