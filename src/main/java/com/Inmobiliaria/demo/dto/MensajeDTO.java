package com.Inmobiliaria.demo.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MensajeDTO {

	private Long remitenteId;
	private List<Long> destinatariosIds;
	private String contenido;
	
}
