package com.Inmobiliaria.demo.dto;

import java.time.LocalDateTime;

import com.Inmobiliaria.demo.enums.EstadoCivil;
import com.Inmobiliaria.demo.enums.EstadoCliente;
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
    private String telefono;
    private String email;
    private DistritoDTO distrito;
    private Genero genero;
    private TipoCliente tipoCliente;
    private String nacionalidad;
    private EstadoCliente estado;
    private LocalDateTime fechaRegistro;
}