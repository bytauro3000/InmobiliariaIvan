package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO liviano exclusivo para la pantalla de inscripciones.
 * Solo contiene los campos que esa pantalla necesita mostrar,
 * evitando transferir letras, comprobantes y demás datos pesados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InscripcionResumenDTO {

    private Integer idContrato;

    /** Nombre completo del primer cliente del contrato */
    private String nombreCliente;

    private String manzana;
    private String numeroLote;

    /** true si el contrato tiene inscripción de LUZ en el microservicio */
    private boolean tieneLuz;

    /** true si el contrato tiene inscripción de AGUA en el microservicio */
    private boolean tieneAgua;
}