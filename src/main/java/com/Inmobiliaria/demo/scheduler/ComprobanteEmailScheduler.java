package com.Inmobiliaria.demo.scheduler;

import com.Inmobiliaria.demo.entity.ContratoCliente;
import com.Inmobiliaria.demo.entity.PagoLetras;
import com.Inmobiliaria.demo.repository.PagoLetraRepository;
import com.Inmobiliaria.demo.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
@Component
@RequiredArgsConstructor
public class ComprobanteEmailScheduler {

    private final ComprobanteEmailHelper helper;
    private final AtomicBoolean ejecutando = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    public void enviarAlArrancar() {
        log.info(">>> ComprobanteEmailScheduler: verificando comprobantes al arrancar...");
        if (!ejecutando.compareAndSet(false, true)) {
            log.warn(">>> ComprobanteEmailScheduler: ya hay un envío en curso, se omite.");
            return;
        }
        try {
            helper.enviarComprobantesDelDiaAnterior(ejecutando);
        } finally {
            ejecutando.set(false);
        }
    }

    // Ejecutar cada día a las 10:00 AM hora Lima
    @Scheduled(cron = "0 0 10 * * *", zone = "America/Lima")
    public void enviarPorCron() {
        log.info(">>> ComprobanteEmailScheduler: enviando comprobantes por cron diario...");
        if (!ejecutando.compareAndSet(false, true)) {
            log.warn(">>> ComprobanteEmailScheduler: ya hay un envío en curso, se omite.");
            return;
        }
        try {
            helper.enviarComprobantesDelDiaAnterior(ejecutando);
        } finally {
            ejecutando.set(false);
        }
    }

    @Slf4j
    @Service
    @RequiredArgsConstructor
    public static class ComprobanteEmailHelper {

        private final PagoLetraRepository pagoLetraRepository;
        private final EmailService emailService;

        @Transactional
        public void enviarComprobantesDelDiaAnterior(AtomicBoolean ejecutando) {
            try {
                LocalDate ayer = LocalDate.now().minusDays(1);

                // Solo traer pagos que AÚN NO tuvieron email enviado
                List<PagoLetras> pagos = pagoLetraRepository.findByFechaPagoAndEmailEnviadoFalse(ayer);

                if (pagos.isEmpty()) {
                    log.info(">>> ComprobanteEmailScheduler: no hay pagos pendientes de envío del {}.", ayer);
                    return;
                }

                log.info(">>> ComprobanteEmailScheduler: {} pago(s) del {} pendientes de envío.", pagos.size(), ayer);

                // Agrupar por número de comprobante para enviar UN solo correo por comprobante
                // Los pagos sin número de comprobante se tratan individualmente
                Map<String, List<PagoLetras>> grupos = new LinkedHashMap<>();
                for (PagoLetras pago : pagos) {
                    String clave = (pago.getNumeroComprobante() != null && !pago.getNumeroComprobante().isBlank())
                            ? pago.getNumeroComprobante()
                            : "SIN_COMP_" + pago.getIdPago();
                    grupos.computeIfAbsent(clave, k -> new ArrayList<>()).add(pago);
                }

                int grupos_enviados = 0;
                int grupos_sin_email = 0;

                for (Map.Entry<String, List<PagoLetras>> entry : grupos.entrySet()) {
                    List<PagoLetras> grupo = entry.getValue();
                    PagoLetras primero = grupo.get(0);

                    try {
                        // Obtener todos los emails únicos de los clientes del contrato
                        var contrato = primero.getLetra().getContrato();
                        List<String> emails = new ArrayList<>();

                        if (contrato.getClientes() != null) {
                            for (ContratoCliente cc : contrato.getClientes()) {
                                String email = cc.getCliente().getEmail();
                                if (email != null && !email.isBlank() && !emails.contains(email)) {
                                    emails.add(email);
                                }
                            }
                        }

                        if (emails.isEmpty()) {
                            log.warn("Contrato #{} sin emails — comprobante {} omitido.",
                                    contrato.getIdContrato(), entry.getKey());
                            grupos_sin_email++;
                            // Marcar como enviado igual para no reintentar indefinidamente
                            grupo.forEach(p -> p.setEmailEnviado(true));
                            pagoLetraRepository.saveAll(grupo);
                            continue;
                        }

                        // ── MODO PRUEBA ──────────────────────────────────────────────────────
                        // Solo se envía al correo de prueba. Para producción:
                        // 1. Elimina las dos líneas siguientes
                        // 2. Descomenta: log.info("Enviando comprobante {} a: {}", ...)
                        List<String> emailsDestino = List.of("bytauro3000@gmail.com");
                        log.info("MODO PRUEBA — comprobante {} redirigido a: {} (originales: {})",
                                entry.getKey(), emailsDestino, emails);
                        // ── FIN MODO PRUEBA ──────────────────────────────────────────────────
                        // List<String> emailsDestino = emails;
                        // log.info("Enviando comprobante {} a: {}", entry.getKey(), emails);

                        emailService.enviarComprobanteATodos(grupo, emailsDestino);

                        // Marcar TODOS los pagos del grupo como enviados
                        grupo.forEach(p -> p.setEmailEnviado(true));
                        pagoLetraRepository.saveAll(grupo);
                        grupos_enviados++;

                    } catch (Exception e) {
                        log.error("Error procesando comprobante {}: {}", entry.getKey(), e.getMessage());
                    }
                }

                log.info(">>> ComprobanteEmailScheduler: {} grupo(s) enviado(s), {} sin email.",
                        grupos_enviados, grupos_sin_email);

            } finally {
                ejecutando.set(false);
            }
        }
    }
}