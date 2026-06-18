package com.Inmobiliaria.demo.dto.apisperu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApisperuAddress {
    private String direccion;
    @JsonProperty("ubigueo")
    private String ubigueo;
    private String distrito;
    private String provincia;
    private String departamento;
}