package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.config.GeminiConfig;
import com.Inmobiliaria.demo.data.KnowledgeBase;
import com.Inmobiliaria.demo.service.GeminiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiServiceImpl implements GeminiService {

    private final RestTemplate geminiRestTemplate;
    private final GeminiConfig geminiConfig;
    private final ObjectMapper objectMapper;

    @Override
    public String consultar(String mensaje) {
        try {
            String url = geminiConfig.getApiUrl() + "?key=" + geminiConfig.getApiKey();

            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(Map.of("text", mensaje)))
                ),
                "systemInstruction", Map.of(
                    "parts", List.of(Map.of("text", KnowledgeBase.SYSTEM_PROMPT))
                ),
                "generationConfig", Map.of(
                    "temperature", 0.4,
                    "maxOutputTokens", 2048,
                    "topP", 0.8,
                    "topK", 40
                )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = geminiRestTemplate.exchange(
                url, HttpMethod.POST, entity, String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return extraerTextoRespuesta(response.getBody());
            }

            log.error("Respuesta inesperada de Gemini API: {}", response.getStatusCode());
            return "Lo siento, no pude procesar tu pregunta en este momento. Intenta de nuevo más tarde.";

        } catch (Exception e) {
            log.error("Error al comunicarse con Gemini API: {}", e.getMessage());
            return "Lo siento, el asistente no está disponible en este momento. Si el problema persiste, contacta al administrador.";
        }
    }

    private String extraerTextoRespuesta(String jsonBody) {
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode candidates = root.path("candidates");

            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.path("content").path("parts");

                if (content.isArray() && content.size() > 0) {
                    return content.get(0).path("text").asText();
                }
            }

            log.error("Estructura de respuesta inesperada de Gemini: {}", jsonBody);
            return "No pude generar una respuesta. Intenta reformular tu pregunta.";

        } catch (Exception e) {
            log.error("Error al parsear respuesta de Gemini: {}", e.getMessage());
            return "Error al procesar la respuesta del asistente.";
        }
    }
}
