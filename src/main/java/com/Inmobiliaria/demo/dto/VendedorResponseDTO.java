package com.Inmobiliaria.demo.dto;
 
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendedorResponseDTO {
    private Integer idVendedor;
    private String nombre;
    private String apellidos;
    private String dni;
    private String celular;
}
 