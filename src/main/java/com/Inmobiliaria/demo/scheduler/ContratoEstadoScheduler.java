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
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContratoEstadoScheduler {

    private final ContratoRepository contratoRepository;

    // Semáforo en memoria: garantiza que solo una ejecución corra a la vez.
    // Protege ante dos escenarios:
    //   1. Reinicio del servicio en Render justo cuando el scheduler está corriendo.
    //   2. Ejecución manual (POST /scheduler/ejecutar) mientras el cron ya está activo.
    private final AtomicBoolean ejecutando = new AtomicBoolean(false);

    /**
     * SE EJECUTA AUTOMÁTICAMENTE AL ARRANCAR EL SERVIDOR.
     * Esto soluciona el problema con Render plan gratuito:
     * el backend duerme hasta las ~10 AM (Lima), por lo que el cron
     * de las 6 AM UTC nunca se ejecutaba. Ahora cada vez que Render
     * despierta el servidor, se actualizan los estados inmediatamente.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ejecutarAlArrancar() {
        log.info(">>> Scheduler EstadoContrato: ejecución inicial al arrancar el servidor...");
        actualizarEstadosContratos();
    }

    /**
     * Corre todos los días a las 3:00 PM UTC = 10:00 AM Lima (UTC-5).
     * Corre también a las 6:09 PM UTC = 1:09 PM Lima (UTC-5).
     * Se ajustaron los horarios para coincidir con las horas en que
     * el backend de Render (plan gratuito) está activo.
     * También puede ejecutarse manualmente vía POST /api/contratos/scheduler/ejecutar.
     */
    @Scheduled(cron = "0 0 15 * * *")   // 10:00 AM Lima
    @Scheduled(cron = "0 15 18 * * *")   //  1:09 PM Lima
    @Transactional
    public void actualizarEstadosContratos() {

        // compareAndSet(false, true): solo entra si estaba en false, y lo pone en true.
        // Si ya hay una ejecución en curso → false → se descarta esta llamada.
        if (!ejecutando.compareAndSet(false, true)) {
            log.warn(">>> Scheduler EstadoContrato: ya hay una ejecución en curso, se omite esta llamada.");
            return;
        }

        try {
            log.info(">>> Scheduler EstadoContrato: iniciando revisión diaria...");

            // JOIN FETCH carga letrasCambio en la misma query
            // — sin lazy loading, sin LazyInitializationException
            List<Contrato> contratos = contratoRepository.findFinanciadosActivosConLetras();
            LocalDate hoy = LocalDate.now();
            int actualizados = 0;

            for (Contrato contrato : contratos) {

                List<LetraCambio> letras = contrato.getLetrasCambio();
                if (letras == null || letras.isEmpty()) continue;

                EstadoContrato estadoActual = contrato.getEstadoContrato();

                // Punto de corte: última letra PAGADA.
                // Solo se revisan las letras posteriores a la última pagada.
                // Si no hay pagadas → se revisan todas.
                Optional<LocalDate> fechaUltimaPagada = letras.stream()
                        .filter(l -> l.getEstadoLetra() == EstadoLetra.PAGADO)
                        .map(LetraCambio::getFechaVencimiento)
                        .filter(f -> f != null)
                        .max(Comparator.naturalOrder());

                // Marcar como VENCIDO las letras PENDIENTES que ya pasaron su fecha
                boolean letrasCambiadas = false;
                for (LetraCambio letra : letras) {
                    if (letra.getEstadoLetra() != EstadoLetra.PENDIENTE) continue;
                    if (letra.getFechaVencimiento() == null) continue;
                    if (!letra.getFechaVencimiento().isBefore(hoy)) continue;

                    if (fechaUltimaPagada.isPresent() &&
                            !letra.getFechaVencimiento().isAfter(fechaUltimaPagada.get())) {
                        continue;
                    }

                    letra.setEstadoLetra(EstadoLetra.VENCIDO);
                    letrasCambiadas = true;
                }

                long letrasVencidas = letras.stream()
                        .filter(l -> l.getEstadoLetra() == EstadoLetra.VENCIDO)
                        .count();

                EstadoContrato nuevoEstado = (letrasVencidas == 0)
                        ? EstadoContrato.ACTIVO
                        : EstadoContrato.MORA;

                if (nuevoEstado != estadoActual || letrasCambiadas) {
                    contrato.setEstadoContrato(nuevoEstado);
                    contratoRepository.saveAndFlush(contrato);
                    if (nuevoEstado != estadoActual) {
                        actualizados++;
                        log.info("Contrato #{} {} → {} ({} letras vencidas)",
                                contrato.getIdContrato(), estadoActual, nuevoEstado, letrasVencidas);
                    }
                }
            }

            log.info(">>> Scheduler EstadoContrato: {} contrato(s) actualizado(s).", actualizados);

        } finally {
            // El bloque finally garantiza que el semáforo se libera
            // incluso si ocurre una excepción inesperada durante la ejecución.
            ejecutando.set(false);
        }
    }

    /** Llamado desde ContratoController: POST /api/contratos/scheduler/ejecutar */
    public void ejecutarManualmente() {
        log.info(">>> Scheduler ejecutado MANUALMENTE.");
        actualizarEstadosContratos();
    }
}