package com.Inmobiliaria.demo.scheduler;

import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.LetraCambio;
import com.Inmobiliaria.demo.enums.EstadoContrato;
import com.Inmobiliaria.demo.enums.EstadoLetra;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContratoEstadoScheduler {

    private final ContratoRepository  contratoRepository;
   
    // Semáforo en memoria: garantiza que solo una ejecución corra a la vez.
    private final AtomicBoolean ejecutando = new AtomicBoolean(false);

    /**
     * SE EJECUTA AUTOMÁTICAMENTE AL ARRANCAR EL SERVIDOR.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ejecutarAlArrancar() {
        log.info(">>> Scheduler EstadoContrato: ejecución inicial al arrancar el servidor...");
        actualizarEstadosContratos();
    }

    /**
     * Corre todos los días a las 3:00 PM UTC = 10:00 AM Lima (UTC-5).
     * Corre también a las 6:15 PM UTC = 1:15 PM Lima (UTC-5).
     * También puede ejecutarse manualmente vía POST /api/contratos/scheduler/ejecutar.
     */
    @Scheduled(cron = "0 0 15 * * *")   // 10:00 AM Lima
    @Scheduled(cron = "0 15 18 * * *")   //  1:15 PM Lima
    @Transactional
    public void actualizarEstadosContratos() {

        if (!ejecutando.compareAndSet(false, true)) {
            log.warn(">>> Scheduler EstadoContrato: ya hay una ejecución en curso, se omite esta llamada.");
            return;
        }

        try {
            log.info(">>> Scheduler EstadoContrato: iniciando revisión diaria...");

            List<Contrato> contratos = contratoRepository.findFinanciadosActivosConLetras();
            LocalDate hoy = LocalDate.now();
            int actualizados = 0;

            for (Contrato contrato : contratos) {

                List<LetraCambio> letras = contrato.getLetrasCambio();
                if (letras == null || letras.isEmpty()) continue;

                EstadoContrato estadoActual = contrato.getEstadoContrato();

                // ── PASO 1: número de la última letra PAGADA ─────────────────────────
                // Si no hay ninguna pagada → numUltimaPagada = 0 (se revisan todas desde la 1)
                // Si hay pagadas → solo se evalúan las letras con número MAYOR a este
                int numUltimaPagada = letras.stream()
                        .filter(l -> l.getEstadoLetra() == EstadoLetra.PAGADO)
                        .mapToInt(l -> extraerNumeroLetra(l.getNumeroLetra()))
                        .max()
                        .orElse(0);

                // ── PASO 2: marcar como VENCIDO las letras PENDIENTES vencidas
                //           que están DESPUÉS de la última pagada ──────────────────────
                boolean letrasCambiadas = false;
                for (LetraCambio letra : letras) {
                    if (letra.getEstadoLetra() != EstadoLetra.PENDIENTE) continue;
                    if (letra.getFechaVencimiento() == null) continue;
                    if (!letra.getFechaVencimiento().isBefore(hoy)) continue;
                    // Solo letras posteriores a la última pagada
                    if (extraerNumeroLetra(letra.getNumeroLetra()) <= numUltimaPagada) continue;

                    letra.setEstadoLetra(EstadoLetra.VENCIDO);
                    letrasCambiadas = true;
                }

                // ── PASO 3: contar letras VENCIDO que están DESPUÉS de la última pagada
                //           esas son las únicas que representan mora real ──────────────
                long letrasVencidasReales = letras.stream()
                        .filter(l -> l.getEstadoLetra() == EstadoLetra.VENCIDO)
                        .filter(l -> extraerNumeroLetra(l.getNumeroLetra()) > numUltimaPagada)
                        .count();

                // ── PASO 4: nuevo estado del contrato ────────────────────────────────
                // Sin letras vencidas reales después de la última pagada → ACTIVO
                // Con letras vencidas reales después de la última pagada  → MORA
                EstadoContrato nuevoEstado = (letrasVencidasReales == 0)
                        ? EstadoContrato.ACTIVO
                        : EstadoContrato.MORA;

                if (nuevoEstado != estadoActual || letrasCambiadas) {
                    contrato.setEstadoContrato(nuevoEstado);
                    contratoRepository.saveAndFlush(contrato);
                    if (nuevoEstado != estadoActual) {
                        actualizados++;
                        log.info("Contrato #{} {} → {} (ultima pagada N°{}, vencidas reales: {})",
                                contrato.getIdContrato(), estadoActual, nuevoEstado,
                                numUltimaPagada, letrasVencidasReales);
                    }
                }
            }

            log.info(">>> Scheduler EstadoContrato: {} contrato(s) actualizado(s).", actualizados);

        } finally {
            ejecutando.set(false);
        }
    }

    /** Llamado desde ContratoController: POST /api/contratos/scheduler/ejecutar */
    public void ejecutarManualmente() {
        log.info(">>> Scheduler ejecutado MANUALMENTE.");
        actualizarEstadosContratos();
    }

    // ── Extrae el número entero de un numeroLetra con formato "64/120" ────────
    private int extraerNumeroLetra(String numeroLetra) {
        if (numeroLetra == null || numeroLetra.isBlank()) return 0;
        String parte = numeroLetra.contains("/")
                ? numeroLetra.split("/")[0].trim()
                : numeroLetra.trim();
        try { return Integer.parseInt(parte); }
        catch (NumberFormatException e) { return 0; }
    }
}