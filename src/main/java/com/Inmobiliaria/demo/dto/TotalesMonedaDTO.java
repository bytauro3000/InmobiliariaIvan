package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotalesMonedaDTO {

    private String moneda;      // USD | PEN | N/D
    private BigDecimal bruto;   // suma de todos los comprobantes del grupo
    private BigDecimal anulada; // suma de los anulados (NC o pago anulado)
    private BigDecimal neto;    // bruto - anulada
}
