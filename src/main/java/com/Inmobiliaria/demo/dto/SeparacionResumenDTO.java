package com.Inmobiliaria.demo.dto;

import java.util.Date;

import com.Inmobiliaria.demo.enums.EstadoSeparacion;

public record SeparacionResumenDTO(	
		Integer idSeparacion,
	    String dni,
	    String nomApeCli,
	    String manLote,
	    Double monto,
	    Date fechaSepara,
	    Date fechaLimite,
	    EstadoSeparacion estadoSeparacion) 
{} 