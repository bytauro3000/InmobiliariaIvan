package com.Inmobiliaria.demo.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import com.Inmobiliaria.demo.entity.Mensaje;
import com.Inmobiliaria.demo.enums.EstadoMensaje;
import com.Inmobiliaria.demo.repository.MensajeRepository;
import com.Inmobiliaria.demo.service.MensajeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MensajeServiceImpl implements MensajeService{

	
	private final MensajeRepository mensajeRepository;
	
	
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

	@Override
	public long contarNoLeidos(Long userId) {
		return mensajeRepository.countByDestinatarioIdAndEstado(userId, EstadoMensaje.ENVIADO);
	}
	
}
