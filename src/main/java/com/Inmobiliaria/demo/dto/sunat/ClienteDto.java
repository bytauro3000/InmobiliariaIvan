package com.Inmobiliaria.demo.dto.sunat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteDto {
    private String tipoDocumento;
    private String numeroDocumento;
    private String razonSocial;
    private String direccion;
    private String ubigeo;
    private String email;
}
