package com.Inmobiliaria.demo.dto;

import java.math.BigDecimal;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteLetraCambioDTO {
	 private String numeroLetra;
	    private Date fechaGiro;
	    private Date fechaVencimiento;
	    private BigDecimal importe;
	    private String importeLetras;
	    private String distritoNombre;
	    private String clienteNombre;
	    private String clienteApellidos;
	    private String numDocumento;
	    private String direccion;
}
