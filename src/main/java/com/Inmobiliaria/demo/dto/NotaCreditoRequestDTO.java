package com.Inmobiliaria.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotaCreditoRequestDTO {

    @NotNull(message = "El ID del pago es obligatorio.")
    private Integer idPago;

    @NotBlank(message = "El tipo de pago es obligatorio.")
    private String tipoPago;

    @NotBlank(message = "El código de motivo es obligatorio.")
    private String codMotivo;

    @NotBlank(message = "La descripción del motivo es obligatoria.")
    private String desMotivo;
}
