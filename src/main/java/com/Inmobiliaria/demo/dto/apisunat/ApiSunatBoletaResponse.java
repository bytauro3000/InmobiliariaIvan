package com.Inmobiliaria.demo.dto.apisunat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Respuesta de la API SUNAT propia al crear una boleta (POST /api/v1/boletas).
 * Formato: { "estado": "exito", "mensaje": "...", "datos": { id, numero_completo, sunat: {...} } }
 */
@Data
public class ApiSunatBoletaResponse {

    private String estado;
    private String mensaje;
    private Datos datos;

    @Data
    public static class Datos {
        private Integer id;

        @JsonProperty("numero_completo")
        private String numeroCompleto;

        private Sunat sunat;
    }

    @Data
    public static class Sunat {
        private String estado;
        private String codigo;
        private String descripcion;

        @JsonProperty("hash_cpe")
        private String hashCpe;
    }
}
