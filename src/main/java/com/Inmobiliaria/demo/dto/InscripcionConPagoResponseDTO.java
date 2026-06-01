package com.Inmobiliaria.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InscripcionConPagoResponseDTO {

    private Integer idPagoInscripcionComprobante;
    private String  numeroComprobante;
    private String  tipoServicio;
    private Integer idContrato;
}