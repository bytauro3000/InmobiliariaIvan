package com.Inmobiliaria.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnulacionRequestDTO {

    @NotBlank(message = "El motivo de anulación es obligatorio.")
    private String motivo;
}