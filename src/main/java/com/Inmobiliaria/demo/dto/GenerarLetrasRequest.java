package com.Inmobiliaria.demo.dto;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerarLetrasRequest {
    private Integer idDistrito;
    private Date fechaGiro;
    private Date fechaVencimientoInicial;
    private String importe;
    private String importeLetras;
}
