package com.Inmobiliaria.demo.dto;

import lombok.Data;

@Data
public class ConsultaDniDTO {
    private String first_name;      // Nombres
    private String first_last_name; // Apellido Paterno
    private String second_last_name;// Apellido Materno
    private String full_name;       // Nombre Completo
    private String document_number; // DNI
    private boolean success;        // Para validar si se encontró
}