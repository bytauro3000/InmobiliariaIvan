package com.Inmobiliaria.demo.scheduler;

import com.Inmobiliaria.demo.entity.Comprobante;
import com.Inmobiliaria.demo.repository.ComprobanteRepository;
import com.Inmobiliaria.demo.service.SunatIntegrationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CdrPendienteScheduler {

    private static final Logger log = LoggerFactory.getLogger(CdrPendienteScheduler.class);
    private final ComprobanteRepository comprobanteRepository;
    private final SunatIntegrationService sunatIntegrationService;

    @Scheduled(fixedRate = 3_600_000, zone = "America/Lima")
    @Transactional
    public void recuperarCdrspendientes() {
        List<Comprobante> pendientes = comprobanteRepository
                .findByEstadoSunatAndCdrBase64IsNull("ACEPTADA");

        if (pendientes.isEmpty()) return;
        log.info("CDR Pendientes: {} comprobante(s) sin CDR", pendientes.size());

        for (Comprobante comp : pendientes) {
            try {
                String serie = comp.getSerie();
                String numero = String.valueOf(comp.getNumero());
                String ruc = "20537853108";
                String tipo = "03";

                if (comp.getTipoComprobante().name().contains("FACTURA")) {
                    tipo = "01";
                }

                Map<String, Object> estado = sunatIntegrationService.consultarEstadoBoleta(tipo, serie, numero, ruc);
                String cdrZip = (String) estado.get("cdrZip");

                if (cdrZip != null && !cdrZip.isBlank()) {
                    comp.setCdrBase64(cdrZip);
                    comprobanteRepository.save(comp);
                    log.info("CDR recuperado para {}", comp.getNumeroCompleto());
                }
            } catch (Exception e) {
                log.warn("Error al recuperar CDR de {}: {}", comp.getNumeroCompleto(), e.getMessage());
            }
        }
    }
}
