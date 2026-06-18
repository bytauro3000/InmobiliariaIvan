package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistritoDTO {
    private Integer idDistrito;
    private String nombre;
    private String codigoUbigeo;
    private String provincia;
    private String departamento;
}
