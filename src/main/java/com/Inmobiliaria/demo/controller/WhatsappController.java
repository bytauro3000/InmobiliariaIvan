package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.EnviarWhatsappRequest;
import com.Inmobiliaria.demo.service.WhatsappService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsappController {

    private final WhatsappService whatsappService;

    public WhatsappController(WhatsappService whatsappService) {
        this.whatsappService = whatsappService;
    }

    @GetMapping("/qr")
    public ResponseEntity<?> obtenerQr() {
        try {
            Map<String, Object> qrData = whatsappService.getQrLink();
            return ResponseEntity.ok(qrData);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "error", e.getMessage()
            );
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> obtenerStatus() {
        Map<String, Object> status = whatsappService.getStatus();
        return ResponseEntity.ok(status);
    }

    @PostMapping("/enviar")
    public ResponseEntity<?> enviarMensaje(@RequestBody EnviarWhatsappRequest request) {
        if (request.getCelular() == null || request.getCelular().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "El cliente no tiene número de celular registrado"
            ));
        }

        if (request.getCelular().matches("0+[-]?0+") || request.getCelular().matches("^0+$")) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "El celular del cliente no es válido (parece un valor por defecto)"
            ));
        }

        Map<String, Object> result = whatsappService.enviarMensaje(request);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(500).body(result);
        }
    }
}
