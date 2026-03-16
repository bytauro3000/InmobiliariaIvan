package com.Inmobiliaria.demo.service;

import java.util.List;

import com.Inmobiliaria.demo.entity.Mensaje;

public interface MensajeService {

	Mensaje guardarMensaje(Mensaje mensaje);
	
	List<Mensaje> obtenerMensajesRecibidos(Long idUsuario);
	
	List<Mensaje> obtenerConversacion(Long remitenteId, Long destinatarioId);
	
	
	//METODO MENSAJE LEIDO
	void marcarMensajesComoLeidos(Long remitenteId, Long destinatarioId);
	
	
}
 