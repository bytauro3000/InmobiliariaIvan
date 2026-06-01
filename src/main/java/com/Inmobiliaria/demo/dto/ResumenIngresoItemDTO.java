package com.Inmobiliaria.demo.dto;

import com.Inmobiliaria.demo.enums.MedioPago;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenIngresoItemDTO {

    private String tipoIngreso;
    private Integer idPago;
    private String numeroComprobante;
    private LocalDate fechaPago;
    private BigDecimal importePagado;
    private MedioPago medioPago;
    private String numeroOperacion;

    private String referencia;

    private Integer idContrato;
    
    private String nombreCliente;

    private String observaciones;
}