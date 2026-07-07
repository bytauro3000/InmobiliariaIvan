package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.dto.EnviarWhatsappRequest;
import com.Inmobiliaria.demo.service.WhatsappService;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import jakarta.annotation.PostConstruct;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

@Service
@Lazy
public class WhatsappServiceImpl implements WhatsappService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappServiceImpl.class);
    private static final DecimalFormat DF = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));

    private final RestTemplate restTemplate;
  

    private String token;
    private String deviceId;

    @Value("${whatsapp.base-url}")
    private String baseUrl;

    @Value("${whatsapp.username}")
    private String username;

    @Value("${whatsapp.password}")
    private String password;

    @Value("${whatsapp.device-id:InmobiliariaIVAN}")
    private String defaultDeviceId;

    public WhatsappServiceImpl() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(20_000);
        this.restTemplate = new RestTemplate(factory);
    }

    @PostConstruct
    public void init() {
        this.deviceId = defaultDeviceId;
        log.info("WhatsAppService inicializado. Device ID: {}", deviceId);
    }

    private synchronized void login() {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("username", username);
            body.put("password", password);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                baseUrl + "/autenticacion/inicio de sesion",
                request,
                JsonNode.class
            );

            JsonNode results = response.getBody();
            if (results != null && results.has("token")) {
                this.token = results.get("token").asText();
                log.info("WhatsApp: login exitoso, token obtenido");
            } else {
                log.error("WhatsApp: login falló, respuesta sin token: {}", results);
                throw new RuntimeException("No se pudo obtener token de WhatsApp");
            }
        } catch (Exception e) {
            log.error("WhatsApp: error en login: {}", e.getMessage());
            throw new RuntimeException("Error al autenticar con WhatsApp API", e);
        }
    }

    private void ensureToken() {
        if (token == null) {
            login();
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("device-id", deviceId);
        return headers;
    }

    @Override
    public Map<String, Object> getQrLink() {
        try {
            ensureToken();

            HttpEntity<Void> request = new HttpEntity<>(authHeaders());
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                baseUrl + "/app/login",
                HttpMethod.GET,
                request,
                JsonNode.class
            );

            JsonNode results = response.getBody();
            if (results != null && results.has("results")) {
                JsonNode data = results.get("results");
                Map<String, Object> map = new HashMap<>();
                map.put("qr_link", data.get("qr_link").asText());
                map.put("qr_duration", data.get("qr_duration").asInt());
                map.put("device_id", data.get("device_id").asText());
                return map;
            }
            throw new RuntimeException("Respuesta inesperada de WhatsApp API");
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                login();
                return getQrLink();
            }
            log.error("WhatsApp: error al obtener QR: {}", e.getMessage());
            throw new RuntimeException("Error al obtener QR de WhatsApp", e);
        } catch (Exception e) {
            log.error("WhatsApp: error al obtener QR: {}", e.getMessage());
            throw new RuntimeException("Error al obtener QR de WhatsApp", e);
        }
    }

    @Override
    public Map<String, Object> getStatus() {
        try {
            ensureToken();

            HttpEntity<Void> request = new HttpEntity<>(authHeaders());
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                baseUrl + "/app/estado",
                HttpMethod.GET,
                request,
                JsonNode.class
            );

            JsonNode results = response.getBody();
            Map<String, Object> map = new HashMap<>();
            if (results != null && results.has("results")) {
                JsonNode data = results.get("results");
                map.put("is_connected", data.get("is_connected").asBoolean());
                map.put("is_logged_in", data.get("is_logged_in").asBoolean());
                map.put("device_id", data.has("device_id") ? data.get("device_id").asText() : deviceId);
            } else {
                map.put("is_connected", false);
                map.put("is_logged_in", false);
                map.put("device_id", deviceId);
            }
            return map;
        } catch (Exception e) {
            log.error("WhatsApp: error al obtener estado: {}", e.getMessage());
            Map<String, Object> map = new HashMap<>();
            map.put("is_connected", false);
            map.put("is_logged_in", false);
            map.put("error", e.getMessage());
            return map;
        }
    }

    @Override
    public Map<String, Object> enviarMensaje(EnviarWhatsappRequest request) {
        try {
            ensureToken();

            String simbolo = "PEN".equals(request.getMoneda()) ? "S/" : "$";
            String totalStr = DF.format(request.getImporteTotal());
            String clienteNombre = request.getNombreClientes();
            int cantidad = request.getCantidadLetrasAtrasadas();

            String mensaje = String.format(
                "Hola %s, soy de INMOBILIARIA CONSTRUCTORA \"IVAN\" E.I.R.L. " +
                "Le recordamos que tiene %d letra(s) vencida(s) por un total de %s %s. " +
                "Le recomendamos pasar a regularizar su situación a la brevedad para evitar que sigan generándose más intereses y mora. " +
                "Agradecemos su atención y quedamos atentos.",
                clienteNombre, cantidad, simbolo, totalStr
            );

            String phoneJid = request.getCelular().startsWith("51")
                ? request.getCelular() + "@s.whatsapp.net"
                : "51" + request.getCelular() + "@s.whatsapp.net";

            Map<String, Object> body = new HashMap<>();
            body.put("phone", phoneJid);
            body.put("message", mensaje);

            HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(body, authHeaders());
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                baseUrl + "/enviar/mensaje",
                HttpMethod.POST,
                httpRequest,
                JsonNode.class
            );

            JsonNode responseBody = response.getBody();
            Map<String, Object> result = new HashMap<>();
            if (responseBody != null) {
                result.put("code", responseBody.get("code").asText());
                result.put("message", responseBody.get("message").asText());
                if (responseBody.has("results")) {
                    JsonNode res = responseBody.get("results");
                    result.put("message_id", res.has("message_id") ? res.get("message_id").asText() : "");
                }
            }
            result.put("success", true);
            result.put("destinatario", clienteNombre);
            return result;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                login();
                return enviarMensaje(request);
            }
            log.error("WhatsApp: error al enviar mensaje: {}", e.getResponseBodyAsString());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "Error al enviar mensaje: " + e.getMessage());
            return result;
        } catch (Exception e) {
            log.error("WhatsApp: error al enviar mensaje: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "Error al enviar mensaje: " + e.getMessage());
            return result;
        }
    }

    public void setTokenDirect(String token) {
        this.token = token;
    }
}
