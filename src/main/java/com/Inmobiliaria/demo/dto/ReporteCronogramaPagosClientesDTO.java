package com.Inmobiliaria.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteCronogramaPagosClientesDTO {
    private Integer idLetra;
    private Integer cantidadLetras;
    private BigDecimal montoTotal;
    private BigDecimal inicial;
    private BigDecimal saldo;
    private String vendedorNombre;
    private String vendedorApellidos;
    private String numeroLetra;
    private LocalDate fechaVencimiento;
    private BigDecimal importe;
    private String cliente1Nombre;
    private String cliente1Apellidos;
    private String cliente1NumDocumento;
    private String cliente1Celular;
    private String cliente1Telefono;
    private String cliente1Direccion;
    private String cliente1Distrito;
    private String cliente2Nombre;
    private String cliente2Apellidos;
    private String cliente2NumDocumento;
    private String lote1Manzana;
    private String lote1NumeroLote;
    private BigDecimal lote1Area;
    private String lote2Manzana;
    private String lote2NumeroLote;
    private BigDecimal lote2Area;
    private String programaNombre;
}