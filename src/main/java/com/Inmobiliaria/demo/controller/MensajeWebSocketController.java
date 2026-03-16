package com.Inmobiliaria.demo.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import com.Inmobiliaria.demo.dto.MensajeDTO;
import com.Inmobiliaria.demo.entity.Mensaje;
import com.Inmobiliaria.demo.entity.Usuario;
import com.Inmobiliaria.demo.repository.UsuarioRepository;
import com.Inmobiliaria.demo.service.MensajeService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MensajeWebSocketController {

	
	private final MensajeService mensajeService;
	private final SimpMessagingTemplate messagingTemplate;
    private final UsuarioRepository usuarioRepository;
	
	
	@MessageMapping("/enviar")
	public void enviarMensaje(MensajeDTO mensajeDTO) {
		
        // 🔹 Validación para evitar errores si la lista viene null o vacía
        if (mensajeDTO.getDestinatariosIds() == null || mensajeDTO.getDestinatariosIds().isEmpty()) {
            return;
        }
		
		
		for (Long destinatarioId : mensajeDTO.getDestinatariosIds()) {
			
			// Obtener usuario destinatario
            Usuario destinatario = usuarioRepository
                    .findById(destinatarioId.intValue())
                    .orElse(null);

            if (destinatario == null) continue;
            
            
            String emailDestinatario = destinatario.getCorreo();
            
            
            
			//CREAR MENSAJE
			Mensaje mensaje = new Mensaje();
			mensaje.setRemitenteId(mensajeDTO.getRemitenteId());
			mensaje.setDestinatarioId(destinatarioId);
			mensaje.setContenido(mensajeDTO.getContenido());
			
			//GUARDAR EN BD
			Mensaje mensajeGuardado = mensajeService.guardarMensaje(mensaje);
			
			//ENVIAR MENSAJE EN TIEMPO REAL AL USUARIO ESPECIFICO
            messagingTemplate.convertAndSendToUser(
                    emailDestinatario,
                    "/privado/mensajes",
                    mensajeGuardado
            );
			
            //TAMBIEN ENVIARR MENSAJE AL REMITENTE
            Usuario remitente = usuarioRepository
                    .findById(mensajeDTO.getRemitenteId().intValue())
                    .orElse(null);
            
            
            if (remitente != null) {
                messagingTemplate.convertAndSendToUser(
                        remitente.getCorreo(),
                        "/privado/mensajes",
                        mensajeGuardado
                );
            }
		}
	}
}
