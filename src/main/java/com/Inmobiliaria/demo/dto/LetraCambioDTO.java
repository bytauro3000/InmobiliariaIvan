package com.Inmobiliaria.demo.dto;

import java.math.BigDecimal;
import com.Inmobiliaria.demo.enums.Moneda;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LetraCambioDTO {

    private Integer idLetra;
    private LocalDate fechaGiro;
    private LocalDate fechaVencimiento;
    private BigDecimal importe;
    private String importeLetras;
    private String estadoLetra;
    private String numeroLetra;
    
    private Integer idContrato;
    private String nombreCliente;

    private Integer idDistrito;
    private String nombreDistrito;

    private LocalDate fechaPago;
    private String tipoComprobante;
    private String numeroComprobante;
    private String observaciones;

    // Moneda del contrato para mostrar símbolo correcto en el frontend
    private Moneda monedaContrato;

    // Indica si este pago fue parte de un pago múltiple (mismo número de comprobante)
    private boolean esMultiple;
}