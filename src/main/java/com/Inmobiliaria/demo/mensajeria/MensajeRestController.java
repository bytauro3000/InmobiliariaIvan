package com.Inmobiliaria.demo.mensajeria;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Inmobiliaria.demo.dto.MensajeDTO;

@RestController
@RequestMapping("/api/mensajes")
public class MensajeRestController {

    @Autowired
    private MensajeService mensajeService;

    // ✅ Endpoint para obtener el historial de chat entre dos personas
    @GetMapping("/historial/{id1}/{id2}")
    public ResponseEntity<List<Mensaje>> obtenerHistorial(@PathVariable Long id1, @PathVariable Long id2) {
        List<Mensaje> historial = mensajeService.obtenerConversacion(id1, id2);
        
        // Marcamos los mensajes como leídos si el usuario 1 está abriendo el chat del usuario 2
        mensajeService.marcarMensajesComoLeidos(id2, id1);
        
        return ResponseEntity.ok(historial);
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