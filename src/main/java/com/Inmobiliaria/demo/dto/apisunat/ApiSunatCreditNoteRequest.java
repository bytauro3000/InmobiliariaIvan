package com.Inmobiliaria.demo.dto.apisunat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payload de nota de crédito para la API SUNAT propia (POST /api/v1/notas-credito).
 * El emisor (RUC, razon social, domicilio, SOL, certificado) NO va en el JSON:
 * la plataforma lo toma de la configuracion del tenant identificado por
 * las cabeceras X-Api-Key / X-Api-Secret.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiSunatCreditNoteRequest {

    private String serie;
    private Integer correlativo;

    @JsonProperty("fecha_emision")
    private String fechaEmision;

    @JsonProperty("tipo_moneda")
    private String tipoMoneda;

    @JsonProperty("enviar_automatico")
    private Boolean enviarAutomatico;

    /** Observación / nota de la operación (cbc:Note). Ej: "OPERACION INAFECTA - VENTA DE TERRENO". */
    private String observacion;

    private Cliente cliente;

    @JsonProperty("doc_afectado_tipo")
    private String docAfectadoTipo;

    @JsonProperty("doc_afectado_serie")
    private String docAfectadoSerie;

    @JsonProperty("doc_afectado_correlativo")
    private String docAfectadoCorrelativo;

    @JsonProperty("cod_motivo")
    private String codMotivo;

    @JsonProperty("des_motivo")
    private String desMotivo;

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

        private String ubigeo;
        private String distrito;
        private String provincia;
        private String departamento;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        /** Código del producto/servicio (SellersItemIdentification). */
        private String codigo;

        private String descripcion;
        private String unidad;
        private BigDecimal cantidad;

        @JsonProperty("precio_unitario")
        private BigDecimal precioUnitario;

        @JsonProperty("tip_afe_igv")
        private String tipAfeIgv;
    }
}