package com.Inmobiliaria.demo.dto.apisunat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payload de boleta para la API SUNAT propia (POST /api/v1/boletas).
 * El emisor (RUC, razon social, domicilio, SOL, certificado) NO va en el JSON:
 * la plataforma lo toma de la configuracion del tenant identificado por
 * las cabeceras X-Api-Key / X-Api-Secret.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiSunatBoletaRequest {

    private String serie;
    private Integer correlativo;

    @JsonProperty("fecha_emision")
    private String fechaEmision;

    @JsonProperty("tipo_moneda")
    private String tipoMoneda;

    @JsonProperty("forma_pago")
    private String formaPago;

    @JsonProperty("enviar_automatico")
    private Boolean enviarAutomatico;

    private Cliente cliente;
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cliente {
        @JsonProperty("tipo_doc")
        private String tipoDoc;

        @JsonProperty("num_doc")
        private String numDoc;

        @JsonProperty("razon_social")
        private String razonSocial;

        private String direccion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String descripcion;
        private String unidad;
        private BigDecimal cantidad;

        @JsonProperty("precio_unitario")
        private BigDecimal precioUnitario;

        @JsonProperty("tip_afe_igv")
        private String tipAfeIgv;
    }
}
