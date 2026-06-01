package com.Inmobiliaria.demo.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class InscripcionServicioDTO {

    
    private Integer    idInscripcion;
    private Integer    idContrato;
    private String     tipoServicio;
    private BigDecimal montoTotal;
    private BigDecimal montoAcumulado;
    private LocalDate  fechaInscripcion;
    private String     estado;
}