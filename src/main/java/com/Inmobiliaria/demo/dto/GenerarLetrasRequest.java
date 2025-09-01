package com.Inmobiliaria.demo.dto;




import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerarLetrasRequest {
    private Integer idDistrito;
    private LocalDate fechaGiro;
    private LocalDate fechaVencimientoInicial;
    private String importe;
    private String importeLetras;
}
