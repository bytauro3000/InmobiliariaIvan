package com.Inmobiliaria.demo.dto.apisperu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApisperuCompany {
    private Long ruc;
    private String razonSocial;
    private String nombreComercial;
    private ApisperuAddress address;
}