package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponseDTO {
    private Integer idCliente; // Opcional, si lo necesitas en el frontend
    private String nombre;
    private String apellidos;
    private String numDoc; // Esto es el DNI
}