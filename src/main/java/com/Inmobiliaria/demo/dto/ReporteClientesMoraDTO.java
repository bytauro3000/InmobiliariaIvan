package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteClientesMoraDTO {

    private String nombrePrograma;

    private List<FilaClienteMora> clientes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilaClienteMora {

        private String           nombreClientes;
        private List<String>     manzanas;
        private List<String>     numeroLotes;
        private List<BigDecimal> areas;
        private int              cantidadLetrasAtrasadas;
        private String           rangoLetras;
        private BigDecimal       importeTotal;
        private String           moneda;
        private String           celular;
        private Integer          idContrato;
        private String           nombrePrograma;
        private LocalDate        fechaVencimientoInicio; 
    }
}