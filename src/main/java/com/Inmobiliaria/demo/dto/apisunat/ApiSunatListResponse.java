package com.Inmobiliaria.demo.dto.apisunat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Respuesta de la API SUNAT propia al listar comprobantes
 * (GET /api/v1/boletas, GET /api/v1/notas-credito).
 * Formato: { "estado": "exito", "datos": [...], "paginacion": {...} }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiSunatListResponse {

    private String estado;
    private String mensaje;
    private List<Dato> datos;
    private Paginacion paginacion;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Dato {
        private Integer id;
        private String serie;
        private Integer correlativo;

        @JsonProperty("numero_completo")
        private String numeroCompleto;

        private Sunat sunat;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sunat {
        private String estado;
        private String codigo;
        private String descripcion;

        @JsonProperty("hash_cpe")
        private String hashCpe;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Paginacion {
        @JsonProperty("pagina_actual")
        private Integer paginaActual;

        @JsonProperty("ultima_pagina")
        private Integer ultimaPagina;

        @JsonProperty("por_pagina")
        private Integer porPagina;

        private Integer total;
    }
}
