package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.dto.apisperu.*;
import com.Inmobiliaria.demo.entity.Cliente;
import com.Inmobiliaria.demo.entity.Comprobante;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.enums.TipoCliente;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.service.SunatIntegrationService;
import com.Inmobiliaria.demo.util.NumeroALetras;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@Lazy
public class SunatIntegrationServiceImpl implements SunatIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(SunatIntegrationServiceImpl.class);
    private static final String LEYENDA_DEFAULT = "OPERACION INAFECTA - VENTA DE TERRENO";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Value("${apisperu.base-url:https://facturacion.apisperu.com/api/v1}")
    private String apisperuBaseUrl;

    @Value("${apisperu.company-token}")
    private String companyToken;

    @Value("${apisperu.ruc:20537853108}")
    private String rucEmisor;

    @Value("${apisperu.environment:produccion}")
    private String environment;

    @PostConstruct
    public void init() {
        log.info("APIPERU configurado en ambiente: {}", environment);
    }

    @Override
    public Map<String, Object> enviarBoleta(Cliente cliente, Contrato contrato,
                                             Comprobante comprobante, BigDecimal monto,
                                             String descripcionDetalle) {
        // Solo enviar si es BOLETA
        if (comprobante.getTipoComprobante() != TipoComprobante.BOLETA) {
            Map<String, Object> skip = new HashMap<>();
            skip.put("estadoSunat", "NO_ENVIADO");
            skip.put("mensaje", "Solo se envían boletas (tipoDoc=03)");
            return skip;
        }

        ApisperuInvoiceRequest request = buildApisperuRequest(cliente, contrato, comprobante, monto, descripcionDetalle);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(companyToken);
        HttpEntity<ApisperuInvoiceRequest> entity = new HttpEntity<>(request, headers);

        // LOG: JSON request completo para debug
        try {
            String requestJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
            log.debug("=== REQUEST JSON A APIPERU ===\n{}", requestJson);
        } catch (Exception e) {
            log.warn("No se pudo serializar request para log: {}", e.getMessage());
        }

        String url = apisperuBaseUrl + "/invoice/send";
        log.info("Enviando boleta a APIPERU: {} {} - {}", comprobante.getSerie(), comprobante.getNumeroCompleto(), url);

        try {
            ResponseEntity<ApisperuResponse> response = restTemplate.postForEntity(url, entity, ApisperuResponse.class);
            
            Map<String, Object> result = new HashMap<>();
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ApisperuResponse body = response.getBody();
                ApisperuSunatResponse sunat = body.getSunatResponse();
                
                log.debug("=== RESPUESTA COMPLETA APIPERU ===");
                log.debug("success: {}", sunat != null ? sunat.getSuccess() : "null");
                log.debug("description: {}", sunat != null ? sunat.getDescription() : "null");
                log.debug("error: {}", sunat != null ? sunat.getError() : "null");
                log.debug("cdrResponse: {}", sunat != null ? sunat.getCdrResponse() : "null");
                log.debug("note: {}", sunat != null ? sunat.getNote() : "null");
                
                boolean isSuccess = sunat != null && Boolean.TRUE.equals(sunat.getSuccess());
                boolean cdrPendiente = false;

                // Si SUNAT aceptó pero no devolvió CDR, igual lo tratamos como aceptado
                if (!isSuccess && sunat != null && sunat.getError() instanceof Map) {
                    Map<?, ?> errorMap = (Map<?, ?>) sunat.getError();
                    String errorCode = errorMap.get("code") != null ? errorMap.get("code").toString() : "";
                    if ("CDR".equals(errorCode)) {
                        isSuccess = true;
                        cdrPendiente = true;
                    }
                }

                result.put("estadoSunat", isSuccess ? "ACEPTADA" : "ERROR");
                result.put("cdrPendiente", cdrPendiente);

                String mensaje = "Sin mensaje";
                if (sunat != null) {
                    if (sunat.getDescription() != null && !sunat.getDescription().isEmpty()) {
                        mensaje = sunat.getDescription();
                    } else if (sunat.getError() != null) {
                        if (cdrPendiente) {
                            mensaje = "Boleta aceptada por SUNAT, CDR pendiente de generación";
                        } else {
                            mensaje = sunat.getError().toString();
                        }
                    } else if (sunat.getCdrResponse() != null) {
                        Object cdr = sunat.getCdrResponse();
                        if (cdr instanceof Map) {
                            Object desc = ((Map<?, ?>) cdr).get("description");
                            if (desc != null) mensaje = desc.toString();
                        }
                    } else if (sunat.getNote() != null) {
                        mensaje = sunat.getNote();
                    }
                }
                result.put("mensaje", mensaje);
                result.put("xml", body.getXml());
                result.put("hash", body.getHash());
                result.put("cdrZip", sunat != null ? sunat.getCdrZip() : null);
                result.put("sunatResponse", sunat);
                log.info("APIPERU resultado: success={}, cdrPendiente={}, mensaje={}", isSuccess, cdrPendiente, mensaje);
            } else {
                result.put("estadoSunat", "ERROR");
                result.put("mensaje", "Error HTTP: " + response.getStatusCode());
            }
            return result;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("=== ERROR RESPONSE APIPERU ===");
            log.error("Status: {}", e.getStatusCode());
            log.error("Body: {}", responseBody);
            Map<String, Object> error = parseErrorResponse(responseBody);
            error.put("estadoSunat", "ERROR");
            return error;
        } catch (Exception e) {
            log.error("Error al enviar boleta a APIPERU: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("estadoSunat", "ERROR");
            error.put("mensaje", e.getMessage());
            return error;
        }
    }

    @Override
    public Map<String, Object> consultarEstadoBoleta(String tipo, String serie, String numero, String ruc) {
        String urlStr = apisperuBaseUrl + "/invoice/status?tipo=" + tipo
                + "&serie=" + serie + "&numero=" + numero
                + (ruc != null ? "&ruc=" + ruc : "");

        log.info("Consultando estado boleta: {}", urlStr);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(companyToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(urlStr, HttpMethod.GET, entity, new ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> result = new HashMap<>();
            result.put("httpStatus", response.getStatusCode().value());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                result.put("success", body.get("success"));
                result.put("code", body.get("code"));
                result.put("message", body.get("message"));
                result.put("cdrZip", body.get("cdrZip"));
                result.put("cdrResponse", body.get("cdrResponse"));
                result.put("error", body.get("error"));

                boolean cdrDisponible = body.get("cdrZip") != null
                        && body.get("cdrZip") instanceof String
                        && !((String) body.get("cdrZip")).isBlank();

                if (Boolean.TRUE.equals(body.get("success")) && cdrDisponible) {
                    result.put("estadoSunat", "ACEPTADA");
                    result.put("mensaje", "CDR disponible");
                } else {
                    result.put("estadoSunat", "CDR_PENDIENTE");
                    result.put("mensaje", body.get("message"));
                }
            } else {
                result.put("estadoSunat", "ERROR");
                result.put("mensaje", "Error HTTP: " + response.getStatusCode());
            }
            return result;
        } catch (Exception e) {
            log.error("Error al consultar estado boleta: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("estadoSunat", "ERROR");
            error.put("mensaje", e.getMessage());
            return error;
        }
    }

    private Map<String, Object> parseErrorResponse(String body) {
        Map<String, Object> error = new HashMap<>();
        error.put("mensaje", "Error de APIPERU");
        try {
            Map<String, Object> parsed = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
            if (parsed.containsKey("message")) {
                error.put("mensaje", parsed.get("message"));
            } else if (parsed.containsKey("errors")) {
                error.put("mensaje", parsed.get("errors").toString());
            }
            error.putAll(parsed);
        } catch (Exception ignored) {
            error.put("mensaje", body);
        }
        return error;
    }

    @Override
    public Map<String, Object> enviarNotaCredito(Cliente cliente, Contrato contrato,
                                                  Comprobante notaCredito,
                                                  Comprobante comprobanteOriginal,
                                                  BigDecimal monto,
                                                  String descripcionDetalle,
                                                  String codMotivo,
                                                  String desMotivo) {

        ApisperuCreditNoteRequest request = buildCreditNoteRequest(cliente, contrato,
                notaCredito, comprobanteOriginal, monto, descripcionDetalle, codMotivo, desMotivo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(companyToken);
        HttpEntity<ApisperuCreditNoteRequest> entity = new HttpEntity<>(request, headers);

        try {
            String requestJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
            log.debug("=== REQUEST NOTA CREDITO A APIPERU ===\n{}", requestJson);
        } catch (Exception e) {
            log.warn("No se pudo serializar request NC para log: {}", e.getMessage());
        }

        String url = apisperuBaseUrl + "/note/send";
        log.info("Enviando nota de credito a APIPERU: {} {} - {}", notaCredito.getSerie(), notaCredito.getNumeroCompleto(), url);

        try {
            ResponseEntity<ApisperuResponse> response = restTemplate.postForEntity(url, entity, ApisperuResponse.class);

            Map<String, Object> result = new HashMap<>();
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ApisperuResponse body = response.getBody();
                ApisperuSunatResponse sunat = body.getSunatResponse();

                log.debug("=== RESPUESTA NOTA CREDITO APIPERU ===");
                log.debug("success: {}", sunat != null ? sunat.getSuccess() : "null");
                log.debug("description: {}", sunat != null ? sunat.getDescription() : "null");
                log.debug("error: {}", sunat != null ? sunat.getError() : "null");
                log.debug("cdrResponse: {}", sunat != null ? sunat.getCdrResponse() : "null");

                boolean isSuccess = sunat != null && Boolean.TRUE.equals(sunat.getSuccess());
                result.put("estadoSunat", isSuccess ? "ACEPTADA" : "ERROR");

                String mensaje = "Sin mensaje";
                if (sunat != null) {
                    if (sunat.getDescription() != null && !sunat.getDescription().isEmpty()) {
                        mensaje = sunat.getDescription();
                    } else if (sunat.getError() != null) {
                        mensaje = sunat.getError().toString();
                    } else if (sunat.getCdrResponse() != null) {
                        Object cdr = sunat.getCdrResponse();
                        if (cdr instanceof Map) {
                            Object desc = ((Map<?, ?>) cdr).get("description");
                            if (desc != null) mensaje = desc.toString();
                        }
                    } else if (sunat.getNote() != null) {
                        mensaje = sunat.getNote();
                    }
                }
                result.put("mensaje", mensaje);
                result.put("xml", body.getXml());
                result.put("hash", body.getHash());
                result.put("cdrZip", sunat != null ? sunat.getCdrZip() : null);
                result.put("sunatResponse", sunat);
                log.info("APIPERU NC resultado: success={}, mensaje={}", isSuccess, mensaje);
            } else {
                result.put("estadoSunat", "ERROR");
                result.put("mensaje", "Error HTTP: " + response.getStatusCode());
            }
            return result;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("=== ERROR RESPONSE NOTA CREDITO APIPERU ===");
            log.error("Status: {}", e.getStatusCode());
            log.error("Body: {}", responseBody);
            Map<String, Object> error = parseErrorResponse(responseBody);
            error.put("estadoSunat", "ERROR");
            return error;
        } catch (Exception e) {
            log.error("Error al enviar nota de credito a APIPERU: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("estadoSunat", "ERROR");
            error.put("mensaje", e.getMessage());
            return error;
        }
    }

    private ApisperuCreditNoteRequest buildCreditNoteRequest(Cliente cliente, Contrato contrato,
                                                              Comprobante notaCredito,
                                                              Comprobante comprobanteOriginal,
                                                              BigDecimal monto,
                                                              String descripcionDetalle,
                                                              String codMotivo,
                                                              String desMotivo) {
        Moneda moneda = contrato.getMoneda();
        String monedaCodigo = moneda != null ? moneda.name() : "PEN";

        BigDecimal igv = BigDecimal.ZERO;
        BigDecimal valorVenta = monto;
        BigDecimal mtoOperGravadas = BigDecimal.ZERO;
        BigDecimal mtoOperInafectas = monto;

        ApisperuClient client = ApisperuClient.builder()
                .tipoDoc(mapTipoDocumento(cliente.getTipoCliente()))
                .numDoc(cliente.getNumDoc())
                .rznSocial(buildRazonSocial(cliente))
                .address(buildAddress(cliente))
                .build();

        ApisperuDetail detail = ApisperuDetail.builder()
                .unidad("NIU")
                .descripcion(descripcionDetalle)
                .cantidad(BigDecimal.ONE)
                .mtoValorUnitario(monto)
                .mtoValorVenta(monto)
                .mtoBaseIgv(monto)
                .porcentajeIgv(BigDecimal.ZERO)
                .igv(BigDecimal.ZERO)
                .tipAfeIgv("30")
                .totalImpuestos(BigDecimal.ZERO)
                .mtoPrecioUnitario(monto)
                .codProducto("SERV001")
                .build();

        String fechaEmision = notaCredito.getFechaEmision().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String horaEmision = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String fechaHoraEmision = fechaEmision + "T" + horaEmision + "-05:00";

        return ApisperuCreditNoteRequest.builder()
                .ublVersion("2.1")
                .tipoDoc("07")
                .serie(notaCredito.getSerie())
                .correlativo(notaCredito.getNumero().toString())
                .fechaEmision(fechaHoraEmision)
                .tipDocAfectado("03")
                .numDocfectado(comprobanteOriginal.getNumeroCompleto())
                .codMotivo(codMotivo)
                .desMotivo(desMotivo)
                .tipoMoneda(monedaCodigo)
                .client(client)
                .company(ApisperuCompany.builder()
                        .ruc(Long.valueOf(rucEmisor))
                        .razonSocial("INMOBILIARIA CONSTRUCTORA IVAN E.I.R.L.")
                        .nombreComercial("INMOBILIARIA CONSTRUCTORA IVAN E.I.R.L.")
                        .address(ApisperuAddress.builder()
                                .direccion("AV. ALFREDO MENDIOLA 3623 INT. 3A")
                                .ubigueo("150117")
                                .distrito("LOS OLIVOS")
                                .provincia("LIMA")
                                .departamento("LIMA")
                                .build())
                        .build())
                .mtoOperGravadas(mtoOperGravadas)
                .mtoOperInafectas(mtoOperInafectas)
                .mtoIGV(igv)
                .valorVenta(valorVenta)
                .totalImpuestos(igv)
                .subTotal(monto)
                .mtoImpVenta(monto)
                .details(Collections.singletonList(detail))
                .legends(Collections.singletonList(
                        ApisperuLegend.builder()
                                .code("1000")
                                .value(LEYENDA_DEFAULT)
                                .build()))
                .montoLetras(NumeroALetras.convertir(monto, moneda))
                .build();
    }

    private ApisperuInvoiceRequest buildApisperuRequest(Cliente cliente, Contrato contrato,
                                                         Comprobante comprobante, BigDecimal monto,
                                                         String descripcionDetalle) {
        Moneda moneda = contrato.getMoneda();
        String monedaCodigo = moneda != null ? moneda.name() : "PEN";

        // VENTA DE TERRENO = OPERACION INAFECTA (código 30) - SIN IGV
        BigDecimal igv = BigDecimal.ZERO;
        BigDecimal valorVenta = monto; // El monto total ES la valor venta para inafecta
        BigDecimal mtoOperGravadas = BigDecimal.ZERO;
        BigDecimal mtoOperInafectas = monto;

        // Cliente
        ApisperuClient client = ApisperuClient.builder()
                .tipoDoc(mapTipoDocumento(cliente.getTipoCliente()))
                .numDoc(cliente.getNumDoc())
                .rznSocial(buildRazonSocial(cliente))
                .address(buildAddress(cliente))
                .build();

        // Detalle - INAFECTA (código 30)
        ApisperuDetail detail = ApisperuDetail.builder()
                .unidad("NIU")
                .descripcion(descripcionDetalle)
                .cantidad(BigDecimal.ONE)
                .mtoValorUnitario(monto)
                .mtoValorVenta(monto)
                .mtoBaseIgv(monto)
                .porcentajeIgv(BigDecimal.ZERO)
                .igv(BigDecimal.ZERO)
                .tipAfeIgv("30")
                .totalImpuestos(BigDecimal.ZERO)
                .mtoPrecioUnitario(monto)
                .codProducto("SERV001")
                .build();

        // Forma de pago
        ApisperuFormaPago formaPago = ApisperuFormaPago.builder()
                .moneda(monedaCodigo)
                .tipo("Contado")
                .build();

        // Fecha emisión con hora
        String fechaEmision = comprobante.getFechaEmision().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String horaEmision = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String fechaHoraEmision = fechaEmision + "T" + horaEmision + "-05:00";

        return ApisperuInvoiceRequest.builder()
                .ublVersion("2.1")
                .tipoOperacion("0101")
                .tipoDoc("03") // Boleta
                .serie(comprobante.getSerie())
                .correlativo(comprobante.getNumero().toString())
                .fechaEmision(fechaHoraEmision)
                .formaPago(formaPago)
                .tipoMoneda(monedaCodigo)
                .client(client)
                .company(ApisperuCompany.builder()
                        .ruc(Long.valueOf(rucEmisor))
                        .razonSocial("INMOBILIARIA CONSTRUCTORA IVAN E.I.R.L.")
                        .nombreComercial("INMOBILIARIA CONSTRUCTORA IVAN E.I.R.L.")
                        .address(ApisperuAddress.builder()
                                .direccion("AV. ALFREDO MENDIOLA 3623 INT. 3A")
                                .ubigueo("150117")
                                .distrito("LOS OLIVOS")
                                .provincia("LIMA")
                                .departamento("LIMA")
                                .build())
                        .build())
                .mtoOperGravadas(mtoOperGravadas)
                .mtoOperInafectas(mtoOperInafectas)
                .mtoIGV(igv)
                .valorVenta(valorVenta)
                .totalImpuestos(igv)
                .subTotal(monto)
                .mtoImpVenta(monto)
                .details(Collections.singletonList(detail))
                .legends(Collections.singletonList(
                        ApisperuLegend.builder()
                                .code("1000")
                                .value(LEYENDA_DEFAULT)
                                .build()))
                .montoLetras(NumeroALetras.convertir(monto, moneda))
                .build();
    }

    private String mapTipoDocumento(TipoCliente tipoCliente) {
        if (tipoCliente == null) return "1";
        return switch (tipoCliente) {
            case NATURAL -> "1";
            case JURIDICO -> "6";
            case CE -> "4"; // 4 = Carné de Extranjería en catálogo SUNAT
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

    private ApisperuAddress buildAddress(Cliente cliente) {
        ApisperuAddress.ApisperuAddressBuilder builder = ApisperuAddress.builder();
        
        StringBuilder direccion = new StringBuilder();
        if (cliente.getDireccion() != null) {
            direccion.append(cliente.getDireccion());
        }
        if (cliente.getDistrito() != null) {
            if (direccion.length() > 0) direccion.append(", ");
            direccion.append(cliente.getDistrito().getNombre());
            if (cliente.getDistrito().getProvincia() != null) {
                direccion.append(", ").append(cliente.getDistrito().getProvincia());
            }
        }
        builder.direccion(direccion.toString());
        
        if (cliente.getDistrito() != null) {
            String codigoUbigeo = cliente.getDistrito().getCodigoUbigeo();
            builder.ubigueo(codigoUbigeo);
            builder.distrito(cliente.getDistrito().getNombre());
            if (cliente.getDistrito().getProvincia() != null) {
                builder.provincia(cliente.getDistrito().getProvincia());
            }
            // Derivar departamento del ubigeo (primeros 2 dígitos)
            if (codigoUbigeo != null && codigoUbigeo.length() >= 2) {
                String codDept = codigoUbigeo.substring(0, 2);
                String nombreDept = switch (codDept) {
                    case "01" -> "AMAZONAS";
                    case "02" -> "ANCASH";
                    case "03" -> "APURIMAC";
                    case "04" -> "AREQUIPA";
                    case "05" -> "AYACUCHO";
                    case "06" -> "CAJAMARCA";
                    case "07" -> "CALLAO";
                    case "08" -> "CUSCO";
                    case "09" -> "HUANCAVELICA";
                    case "10" -> "HUANUCO";
                    case "11" -> "ICA";
                    case "12" -> "JUNIN";
                    case "13" -> "LA LIBERTAD";
                    case "14" -> "LAMBAYEQUE";
                    case "15" -> "LIMA";
                    case "16" -> "LORETO";
                    case "17" -> "MADRE DE DIOS";
                    case "18" -> "MOQUEGUA";
                    case "19" -> "PASCO";
                    case "20" -> "PIURA";
                    case "21" -> "PUNO";
                    case "22" -> "SAN MARTIN";
                    case "23" -> "TACNA";
                    case "24" -> "TUMBES";
                    case "25" -> "UCAYALI";
                    default -> "LIMA";
                };
                builder.departamento(nombreDept);
            }
        }
        
        return builder.build();
    }
}