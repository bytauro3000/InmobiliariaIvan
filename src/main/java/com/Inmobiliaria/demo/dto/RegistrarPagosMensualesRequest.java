package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Registro de uno o más pagos mensuales de comisión (multiselección).
 * Genera un único recibo de egresos (EG01) con todos los pagos seleccionados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarPagosMensualesRequest {
    private Integer idComision;
    private List<Integer> idLetras;
    private String observacion;
}