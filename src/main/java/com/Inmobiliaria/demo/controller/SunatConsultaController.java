package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.entity.Comprobante;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.enums.TipoOrigenComprobante;
import com.Inmobiliaria.demo.repository.ComprobanteRepository;
import com.Inmobiliaria.demo.service.SunatIntegrationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sunat")
@RequiredArgsConstructor
public class SunatConsultaController {

    private static final Logger log = LoggerFactory.getLogger(SunatConsultaController.class);
    private final SunatIntegrationService sunatIntegrationService;
    private final ComprobanteRepository comprobanteRepository;

    @GetMapping("/consultar")
    public ResponseEntity<Map<String, Object>> consultarBoleta(
            @RequestParam String tipo,
            @RequestParam String serie,
            @RequestParam String numero,
            @RequestParam(required = false) String ruc) {
        Map<String, Object> resultado = sunatIntegrationService.consultarEstadoBoleta(tipo, serie, numero, ruc);
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/recuperar")
    public ResponseEntity<Map<String, Object>> recuperarBoleta(
            @RequestParam String serie,
            @RequestParam Integer numero,
            @RequestParam String fechaEmision,
            @RequestParam Double monto,
            @RequestParam String tipoOrigen,
            @RequestParam(required = false) String descripcion) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Consultar estado actual en Apisperu
            String ruc = "20537853108";
            Map<String, Object> estado = sunatIntegrationService.consultarEstadoBoleta("03", serie, String.valueOf(numero), ruc);

            String estadoSunat = (String) estado.getOrDefault("estadoSunat", "ERROR");

            // Crear comprobante aunque el CDR no esté disponible aún
            Comprobante comp = new Comprobante();
            comp.setTipoComprobante(TipoComprobante.BOLETA);
            comp.setSerie(serie.toUpperCase());
            comp.setNumero(numero);
            comp.setNumeroCompleto(serie.toUpperCase() + "-" + numero);
            comp.setFechaEmision(LocalDate.parse(fechaEmision));
            comp.setMonto(java.math.BigDecimal.valueOf(monto));
            comp.setTipoOrigen(TipoOrigenComprobante.valueOf(tipoOrigen));
            comp.setDescripcion(descripcion);
            comp.setEmailEnviado(false);
            comp.setEstadoSunat("ACEPTADA");

            // Si hay CDR disponible, guardarlo
            String cdrZip = (String) estado.get("cdrZip");
            if (cdrZip != null && !cdrZip.isBlank()) {
                comp.setCdrBase64(cdrZip);
            }

            comp = comprobanteRepository.save(comp);

            response.put("success", true);
            response.put("mensaje", "Boleta recuperada exitosamente.");
            response.put("idComprobante", comp.getIdComprobante());
            response.put("numeroCompleto", comp.getNumeroCompleto());
            response.put("cdrDisponible", cdrZip != null && !cdrZip.isBlank());

        } catch (Exception e) {
            log.error("Error al recuperar boleta: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("mensaje", "Error al recuperar boleta: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}
