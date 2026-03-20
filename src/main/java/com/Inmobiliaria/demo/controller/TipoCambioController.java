package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.service.ConfiguracionSistemaService;
import com.Inmobiliaria.demo.service.TipoCambioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TipoCambioController {

    private final TipoCambioService           tipoCambioService;
    private final ConfiguracionSistemaService configService;

    // GET /api/public/tipo-cambio — público, sin login
    @GetMapping("/api/public/tipo-cambio")
    public ResponseEntity<Map<String, BigDecimal>> obtenerTipoCambio() {
        return ResponseEntity.ok(Map.of(
            "oficial",  tipoCambioService.obtenerTipoCambioOficial(),  // precio de mercado API
            "empresa",  tipoCambioService.obtenerTipoCambioEmpresa(),  // oficial + 0.02  (contratos)
            "compra",   tipoCambioService.obtenerTipoCambioCompra(),   // oficial + 0.0206 (cliente compra $)
            "venta",    tipoCambioService.obtenerTipoCambioVenta(),    // oficial - 0.0054 (cliente vende $)
            "respaldo", configService.getTipoCambioRespaldo()
        ));
    }

    // PUT /api/tipo-cambio/respaldo — solo ROLE_SECRETARIA
    @PutMapping("/api/tipo-cambio/respaldo")
    public ResponseEntity<Map<String, Object>> actualizarRespaldo(@RequestBody Map<String, BigDecimal> body) {
        BigDecimal nuevoValor = body.get("valor");
        if (nuevoValor == null || nuevoValor.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El valor debe ser mayor a cero"));
        }
        configService.setTipoCambioRespaldo(nuevoValor);
        return ResponseEntity.ok(Map.of(
            "mensaje", "Tipo de cambio de respaldo actualizado correctamente",
            "valor",   nuevoValor
        ));
    }
}