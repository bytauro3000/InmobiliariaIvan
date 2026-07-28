package com.Inmobiliaria.demo.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.Inmobiliaria.demo.dto.MensajeDTO;
import com.Inmobiliaria.demo.dto.UsuarioListadoDTO;
import com.Inmobiliaria.demo.entity.Mensaje;
import com.Inmobiliaria.demo.service.MensajeService;
import com.Inmobiliaria.demo.service.UsuarioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mensajes")
@RequiredArgsConstructor
public class MensajeRestController {

    
    private final MensajeService mensajeService;
    private final UsuarioService usuarioService;

    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioListadoDTO>> listarUsuariosChat() {
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }

    // ✅ Endpoint para obtener el historial de chat entre dos personas
    @GetMapping("/historial/{id1}/{id2}")
    public ResponseEntity<List<Mensaje>> obtenerHistorial(@PathVariable Long id1, @PathVariable Long id2) {
        List<Mensaje> historial = mensajeService.obtenerConversacion(id1, id2);
        
        // Marcamos los mensajes como leídos si el usuario 1 está abriendo el chat del usuario 2
        mensajeService.marcarMensajesComoLeidos(id2, id1);
        
        return ResponseEntity.ok(historial);
    }

    @GetMapping("/no-leidos/{userId}")
    public ResponseEntity<Long> obtenerNoLeidos(@PathVariable Long userId) {
        long count = mensajeService.contarNoLeidos(userId);
        return ResponseEntity.ok(count);
    }

    // ✅ Endpoint opcional por si decides enviar mensajes por HTTP en lugar de WebSocket
    @PostMapping("/enviar-rest")
    public ResponseEntity<Mensaje> enviarMensajeRest(@RequestBody MensajeDTO mensajeDTO) {
        if (mensajeDTO.getDestinatariosIds() == null || mensajeDTO.getDestinatariosIds().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Mensaje mensaje = new Mensaje();
        mensaje.setRemitenteId(mensajeDTO.getRemitenteId());
        mensaje.setDestinatarioId(mensajeDTO.getDestinatariosIds().get(0)); // Toma el primer destinatario
        mensaje.setContenido(mensajeDTO.getContenido());
        
        Mensaje guardado = mensajeService.guardarMensaje(mensaje);
        return ResponseEntity.ok(guardado);
    }
}