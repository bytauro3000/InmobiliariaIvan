package com.Inmobiliaria.demo.mensajeria;

import java.util.List;

public interface MensajeService {

	Mensaje guardarMensaje(Mensaje mensaje);
	
	List<Mensaje> obtenerMensajesRecibidos(Long idUsuario);
	
	List<Mensaje> obtenerConversacion(Long remitenteId, Long destinatarioId);
	
	
	//METODO MENSAJE LEIDO
	void marcarMensajesComoLeidos(Long remitenteId, Long destinatarioId);
	
	
}
 