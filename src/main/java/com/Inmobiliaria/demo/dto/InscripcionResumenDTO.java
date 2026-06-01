package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class InscripcionResumenDTO {

    private Integer idContrato;

    /** Nombre completo del primer cliente del contrato */
    private String nombreCliente;

    private String manzana;
    private String numeroLote;

    /** true si el contrato tiene inscripción de LUZ activa/completada */
    private boolean tieneLuz;

    /** true si el contrato tiene inscripción de AGUA activa/completada */
    private boolean tieneAgua;

    /** true si tiene una inscripción de LUZ en estado PENDIENTE_PAGO */
    private boolean tienePendienteLuz;

    /** true si tiene una inscripción de AGUA en estado PENDIENTE_PAGO */
    private boolean tienePendienteAgua;

    /** Datos de la inscripción de LUZ pendiente (nulo si no existe) */
    private PendienteInscripcionDTO pendienteLuz;

    /** Datos de la inscripción de AGUA pendiente (nulo si no existe) */
    private PendienteInscripcionDTO pendienteAgua;
}