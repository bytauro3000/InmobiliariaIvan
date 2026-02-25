package com.Inmobiliaria.demo.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InscripcionServicioDTO {

    private Integer idContrato;
    private String tipoServicio; // "LUZ" o "AGUA"
    private BigDecimal montoPagado;
    private LocalDate fechaInscripcion;
    private String estado; // "PENDIENTE_CONEXION", "ACTIVO", etc.
}