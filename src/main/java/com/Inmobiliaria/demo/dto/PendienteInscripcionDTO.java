package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendienteInscripcionDTO {
    private Integer    idInscripcion;
    private BigDecimal montoTotal;
    private BigDecimal montoAcumulado;
}