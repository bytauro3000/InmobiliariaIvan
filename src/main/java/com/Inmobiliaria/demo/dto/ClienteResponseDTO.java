package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.entity.Distrito;
import com.Inmobiliaria.demo.enums.EstadoCivil;
import com.Inmobiliaria.demo.enums.Genero;
import com.Inmobiliaria.demo.enums.TipoCliente;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponseDTO {
    private Integer idCliente;
    private String nombre;
    private String apellidos;
    private EstadoCivil estadoCivil;
    private String numDoc;
    private String direccion;
    private String celular;
    private Distrito distrito;
    private Genero genero;
    private TipoCliente tipoCliente;
    private String nacionalidad;
}