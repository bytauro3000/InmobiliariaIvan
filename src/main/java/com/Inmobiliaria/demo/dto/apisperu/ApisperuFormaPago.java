package com.Inmobiliaria.demo.dto.apisperu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApisperuFormaPago {
    private String moneda;
    private String tipo;
}