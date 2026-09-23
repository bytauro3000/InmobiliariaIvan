package com.Inmobiliaria.demo.scheduler;

import com.Inmobiliaria.demo.entity.Comprobante;
import com.Inmobiliaria.demo.repository.ComprobanteRepository;
import com.Inmobiliaria.demo.service.EmpresaService;
import com.Inmobiliaria.demo.service.SunatApiSunatClient;
import com.Inmobiliaria.demo.service.SunatIntegrationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class CdrPendienteScheduler {

    private static final Logger log = LoggerFactory.getLogger(CdrPendienteScheduler.class);
    private static final int SYNC_PAGE_SIZE = 100;

    private final ComprobanteRepository comprobanteRepository;
    private final SunatIntegrationService sunatIntegrationService;
    private final SunatApiSunatClient sunatApiSunatClient;
    private final EmpresaService empresaService;

    /** Evita ejecuciones concurrentes del scheduler de sincronización. */
    private final AtomicBoolean sincronizando = new AtomicBoolean(false);

    /** Proveedor de facturacion: apisperu (default) o apisunat. Se lee de env (SUNAT_PROVIDER). */
    @Value("${sunat.provider:apisperu}")
    private String sunatProvider;

    // ─── APIPERU: recuperar CDRs de comprobantes aceptados ────────────────────

    @Scheduled(fixedRate = 3_600_000, zone = "America/Lima")
    @Transactional
    public void recuperarCdrspendientes() {
        // Con api-sunat el CDR vive en la plataforma (api-sunat), no en el monolito:
        // consultar a APIPERU aquí daría falsos 404 ("Empresa no encontrada").
        if ("apisunat".equalsIgnoreCase(sunatProvider)) {
            log.debug("Proveedor apisunat: recuperación de CDR delegada a api-sunat. Se omite.");
            return;
        }

        int page = 0;
        int PAGE_SIZE = 50;
        long totalProcesados = 0;

        while (true) {
            Page<Comprobante> pendientesPage = comprobanteRepository
                    .findByEstadoSunatAndCdrBase64IsNull("ACEPTADA", PageRequest.of(page, PAGE_SIZE));

            if (pendientesPage.isEmpty()) break;
            if (totalProcesados == 0) {
                log.info("CDR Pendientes: {} comprobante(s) sin CDR", pendientesPage.getTotalElements());
            }

            for (Comprobante comp : pendientesPage.getContent()) {
                try {
                    String serie = comp.getSerie();
                    String numero = String.valueOf(comp.getNumero());
                    String ruc = empresaService.obtenerActiva().getRuc();
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

            totalProcesados += pendientesPage.getNumberOfElements();
            page++;
        }
    }

    // ─── API-SUNAT: sincronizar estado de boletas y NCs pendientes/rechazadas ─
    //
    // Flujo:
    // 1. Consulta la BD del monolito por comprobantes con estado PENDIENTE o RECHAZADA
    // 2. Para cada uno, consulta api-sunat por serie+correlativo (filtra por tenant)
    // 3. Si api-sunat dice ACEPTADA → actualiza monolito a ACEPTADA + guarda hash
    //    Si api-sunat dice RECHAZADA → actualiza monolito a RECHAZADA + guarda código
    //    Si api-sunat dice PENDIENTE → no hace nada (sigue esperando)
    //
    // Se ejecuta cada 20 minutos. No usa @Transactional a nivel de método para
    // que cada actualización se confirme de forma independiente.

    @Scheduled(fixedRate = 1_200_000, zone = "America/Lima")
    public void sincronizarComprobantesApisunat() {
        if (!"apisunat".equalsIgnoreCase(sunatProvider)) {
            return;
        }
        if (!sincronizando.compareAndSet(false, true)) {
            log.debug("Sincronización api-sunat ya en curso, se omite.");
            return;
        }

        try {
            Page<Comprobante> pendientes = comprobanteRepository
                    .findPendientesSincronizacion(PageRequest.of(0, SYNC_PAGE_SIZE));

            if (pendientes.isEmpty()) {
                log.debug("Sin comprobantes pendientes de sincronización con api-sunat.");
                return;
            }

            log.info("Sincronizando {} comprobante(s) con api-sunat...", pendientes.getTotalElements());

            long actualizados = 0;
            for (Comprobante comp : pendientes.getContent()) {
                try {
                    if (sincronizarComprobante(comp)) {
                        actualizados++;
                    }
                } catch (Exception e) {
                    log.warn("Error sincronizando {}: {}", comp.getNumeroCompleto(), e.getMessage());
                }
            }

            if (actualizados > 0) {
                log.info("Sincronización api-sunat completada: {} comprobante(s) actualizado(s)", actualizados);
            }
        } finally {
            sincronizando.set(false);
        }
    }

    /**
     * Sincroniza un comprobante individual con api-sunat.
     * Retorna true si el estado fue actualizado.
     */
    private boolean sincronizarComprobante(Comprobante comp) {
        Map<String, Object> estado = sunatApiSunatClient.consultarEstadoComprobante(
                comp.getSerie(), comp.getNumero(), comp.getTipoComprobante());

        String nuevoEstado = (String) estado.get("estadoSunat");

        // No actualizar si: error, no encontrado, o sin cambio
        if (nuevoEstado == null
                || "ERROR".equals(nuevoEstado)
                || "NO_ENCONTRADO".equals(nuevoEstado)
                || nuevoEstado.equals(comp.getEstadoSunat())) {
            return false;
        }

        String estadoAnterior = comp.getEstadoSunat();
        comp.setEstadoSunat(nuevoEstado);

        // Guardar hash del CDR si está aceptado y aún no lo tiene
        String hash = (String) estado.get("hash");
        if (hash != null && comp.getHashCdr() == null) {
            comp.setHashCdr(hash);
        }

        // Guardar código de error si está rechazada
        if ("RECHAZADA".equals(nuevoEstado)) {
            comp.setSunatError((String) estado.get("codigoError"));
        } else {
            comp.setSunatError(null);
        }

        comprobanteRepository.save(comp);
        log.info("Comprobante {} sincronizado: {} → {}",
                comp.getNumeroCompleto(), estadoAnterior, nuevoEstado);
        return true;
    }
}
