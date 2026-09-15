package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.AsistenteRequestDTO;
import com.Inmobiliaria.demo.dto.AsistenteResponseDTO;
import com.Inmobiliaria.demo.service.GeminiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AsistenteIAController {

    private final GeminiService geminiService;

    @PostMapping("/asistente")
    public ResponseEntity<AsistenteResponseDTO> consultar(
            @RequestBody @Valid AsistenteRequestDTO request) {

        String respuesta = geminiService.consultar(request.getMensaje());
        return ResponseEntity.ok(new AsistenteResponseDTO(respuesta));
    }
}
