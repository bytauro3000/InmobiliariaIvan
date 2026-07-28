package com.Inmobiliaria.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Inmobiliaria.demo.dto.PublicMensajeDTO;
import com.Inmobiliaria.demo.entity.Mensaje;
import com.Inmobiliaria.demo.entity.Usuario;
import com.Inmobiliaria.demo.repository.UsuarioRepository;
import com.Inmobiliaria.demo.service.MensajeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public/mensajes")
@RequiredArgsConstructor
public class PublicMensajeController {

    private final MensajeService mensajeService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping
    public ResponseEntity<?> enviarMensajePublico(@RequestBody PublicMensajeDTO dto) {
        if (dto.getContenido() == null || dto.getContenido().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("El mensaje es requerido");
        }
        if (dto.getNombres() == null || dto.getNombres().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("El nombre es requerido");
        }

        List<Usuario> secretarias = usuarioRepository.findByRol_RolUsuario("SECRETARIA");

        String contenido = "💬 Mensaje de " + dto.getNombres().trim()
            + (dto.getCorreo() != null && !dto.getCorreo().isBlank() ? " (" + dto.getCorreo().trim() + ")" : "")
            + ":\n" + dto.getContenido().trim();

        for (Usuario secretaria : secretarias) {
            Mensaje mensaje = new Mensaje();
            mensaje.setRemitenteId(-1L);
            mensaje.setDestinatarioId(secretaria.getId().longValue());
            mensaje.setContenido(contenido);
            mensajeService.guardarMensaje(mensaje);
        }

        return ResponseEntity.ok().body("Mensaje enviado correctamente");
    }
}
