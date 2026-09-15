package com.Inmobiliaria.demo.service;

public interface GeminiService {

    /**
     * Envía un mensaje al asistente IA y retorna la respuesta generada.
     *
     * @param mensaje del usuario
     * @return respuesta generada por Gemini
     * @throws RuntimeException si ocurre un error al comunicarse con la API
     */
    String consultar(String mensaje);
}
