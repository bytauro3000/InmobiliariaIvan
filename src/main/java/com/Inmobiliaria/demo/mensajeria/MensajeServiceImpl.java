package com.Inmobiliaria.demo.mensajeria;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Inmobiliaria.demo.enums.EstadoMensaje;

@Service
public class MensajeServiceImpl implements MensajeService{

	@Autowired
	private MensajeRepository mensajeRepository;
	
	
	//GUARDAR MENSAJE
	@Override
	public Mensaje guardarMensaje(Mensaje mensaje) {
		
		mensaje.setFecha(LocalDateTime.now());
		mensaje.setEstado(EstadoMensaje.ENVIADO);
		
		return mensajeRepository.save(mensaje);
		
	}
	
	
	//VER MENSAJES RECIBIDOS
	@Override
	public List<Mensaje> obtenerMensajesRecibidos(Long idUsuario){
		return mensajeRepository.findByDestinatarioId(idUsuario);
	}

	
	@Override
	public List<Mensaje> obtenerConversacion(Long remitenteId, Long destinatarioId){
		
		return mensajeRepository
				.findByRemitenteIdAndDestinatarioIdOrRemitenteIdAndDestinatarioIdOrderByFechaAsc(
						remitenteId, destinatarioId, 
						destinatarioId, remitenteId);
		
	}
	
	
	
	//MARCAR MENSAJES COMO LEIDOS
	@Override
	public void marcarMensajesComoLeidos(Long remitenteId, Long destinatarioId) {
		mensajeRepository.marcarComoLeidos(remitenteId, destinatarioId);
	}
	
}
