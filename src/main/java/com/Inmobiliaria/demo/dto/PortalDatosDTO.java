package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos personales del cliente en el portal (solo lectura).
 * DNI y teléfonos llegan ENMASCARADOS: el portal nunca expone los valores completos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortalDatosDTO {

    private String nombre;
    private String apellidos;
    private String numDocEnmascarado;
    private String celularEnmascarado;
    private String telefonoEnmascarado;
    private String email;
    private String direccion;
    private String distrito;
    private String provincia;
    private String departamento;
    private String genero;
    private String estadoCivil;
    private String tipoCliente;
}
