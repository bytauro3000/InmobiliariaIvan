package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.entity.Comprobante;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.enums.TipoOrigenComprobante;
import com.Inmobiliaria.demo.repository.ComprobanteRepository;
import com.Inmobiliaria.demo.service.EmpresaService;
import com.Inmobiliaria.demo.service.PagoLetraService;
import com.Inmobiliaria.demo.service.SunatIntegrationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sunat")
@RequiredArgsConstructor
public class SunatConsultaController {

    private static final Logger log = LoggerFactory.getLogger(SunatConsultaController.class);
    private final SunatIntegrationService sunatIntegrationService;
    private final ComprobanteRepository comprobanteRepository;
    private final EmpresaService empresaService;
    private final PagoLetraService pagoLetraService;

    @GetMapping("/consultar")
    public ResponseEntity<Map<String, Object>> consultarBoleta(
            @RequestParam String tipo,
            @RequestParam String serie,
            @RequestParam String numero,
            @RequestParam(required = false) String ruc) {
        Map<String, Object> resultado = sunatIntegrationService.consultarEstadoBoleta(tipo, serie, numero, ruc);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Sincroniza un comprobante con SUNAT después de un reenvío manual desde api-sunat.
     * Actualiza el estado en la BD según la respuesta real de SUNAT.
     * Si una NC pasa de PENDIENTE a ACEPTADA, procesa la anulación del pago.
     */
    @PostMapping("/sincronizar")
    public ResponseEntity<Map<String, Object>> sincronizarComprobante(
            @RequestParam(required = false) Long idComprobante,
            @RequestParam(required = false) String serie,
            @RequestParam(required = false) Integer numero) {

        Map<String, Object> response = new HashMap<>();

        try {
            Comprobante comp;
            if (idComprobante != null) {
                comp = comprobanteRepository.findById(idComprobante).orElse(null);
            } else if (serie != null && numero != null) {
                String numeroCompleto = serie.toUpperCase() + "-" + numero;
                comp = comprobanteRepository.findByNumeroCompleto(numeroCompleto).orElse(null);
            } else {
                response.put("success", false);
                response.put("mensaje", "Debe enviar idComprobante o serie+numero.");
                return ResponseEntity.badRequest().body(response);
            }

            if (comp == null) {
                response.put("success", false);
                response.put("mensaje", "Comprobante no encontrado.");
                return ResponseEntity.ok(response);
            }

            // Si ya está ACEPTADO, no reenviar a SUNAT (evita error 1033)
            if ("ACEPTADA".equals(comp.getEstadoSunat())) {
                response.put("success", true);
                response.put("idComprobante", comp.getIdComprobante());
                response.put("numeroCompleto", comp.getNumeroCompleto());
                response.put("estadoActual", comp.getEstadoSunat());
                response.put("mensaje", "Comprobante ya está ACEPTADO. No requiere sincronización.");
                return ResponseEntity.ok(response);
            }

            String estadoAnterior = comp.getEstadoSunat();

            // Consultar estado real en SUNAT
            String ruc = empresaService.obtenerActiva().getRuc();
            String tipoDoc = comp.getTipoComprobante() == TipoComprobante.BOLETA ? "03" : "07";
            Map<String, Object> estado = sunatIntegrationService.consultarEstadoBoleta(
                    tipoDoc, comp.getSerie(), String.valueOf(comp.getNumero()), ruc);

            String estadoSunat = (String) estado.getOrDefault("estadoSunat", "ERROR");

            // Actualizar comprobante
            if ("ACEPTADA".equals(estadoSunat)) {
                comp.setEstadoSunat("ACEPTADA");
                String cdrZip = (String) estado.get("cdrZip");
                if (cdrZip != null && !cdrZip.isBlank()) {
                    comp.setCdrBase64(cdrZip);
                }
            } else if ("CDR_PENDIENTE".equals(estadoSunat)) {
                comp.setEstadoSunat("PENDIENTE");
            } else {
                comp.setEstadoSunat("RECHAZADA");
            }

            comp = comprobanteRepository.save(comp);

            // Si una NC pasó de PENDIENTE a ACEPTADA, procesar la anulación del pago
            boolean ncRecienAceptada = comp.getTipoComprobante() == TipoComprobante.NOTA_CREDITO
                    && "PENDIENTE".equals(estadoAnterior)
                    && "ACEPTADA".equals(comp.getEstadoSunat());

            if (ncRecienAceptada) {
                log.info("NC {} sincronizada: PENDIENTE → ACEPTADA. Procesando anulación del pago...", comp.getNumeroCompleto());
                try {
                    pagoLetraService.procesarAnulacionPendiente(comp);
                } catch (Exception e) {
                    log.warn("No se pudo procesar anulación automática para NC {}: {}", comp.getNumeroCompleto(), e.getMessage());
                    response.put("warningAnulacion", "NC aceptada pero la anulación del pago debe procesarse manualmente: " + e.getMessage());
                }
            }

            response.put("success", true);
            response.put("idComprobante", comp.getIdComprobante());
            response.put("numeroCompleto", comp.getNumeroCompleto());
            response.put("estadoAnterior", estadoAnterior);
            response.put("estadoActual", comp.getEstadoSunat());
            response.put("mensaje", "Comprobante sincronizado. Estado: " + comp.getEstadoSunat());

        } catch (Exception e) {
            log.error("Error al sincronizar comprobante: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("mensaje", "Error al sincronizar: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
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
            String ruc = empresaService.obtenerActiva().getRuc();
            Map<String, Object> estado = sunatIntegrationService.consultarEstadoBoleta("03", serie, String.valueOf(numero), ruc);

            String estadoSunat = (String) estado.getOrDefault("estadoSunat", "ERROR");

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
