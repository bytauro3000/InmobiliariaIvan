package com.Inmobiliaria.demo.scheduler;

import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.LetraCambio;
import com.Inmobiliaria.demo.enums.EstadoContrato;
import com.Inmobiliaria.demo.enums.EstadoLetra;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContratoEstadoScheduler {

    private final ContratoRepository contratoRepository;

    /**
     * Corre todos los días a las 6:00 AM.
     * También puede ejecutarse manualmente vía POST /api/contratos/scheduler/ejecutar
     */
    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void actualizarEstadosContratos() {
        log.info(">>> Scheduler EstadoContrato: iniciando revisión diaria...");

        // JOIN FETCH carga letrasCambio en la misma query — sin lazy loading, sin LazyInitializationException
        List<Contrato> contratos = contratoRepository.findFinanciadosActivosConLetras();
        LocalDate hoy = LocalDate.now();
        int actualizados = 0;

        for (Contrato contrato : contratos) {

            List<LetraCambio> letras = contrato.getLetrasCambio();
            if (letras == null || letras.isEmpty()) continue;

            EstadoContrato estadoActual = contrato.getEstadoContrato();

            // ─── Punto de corte: última letra PAGADA ─────────────────────────
            // Si hay letras pagadas → solo revisar las posteriores a la última pagada
            // Si no hay pagadas → revisar todas las letras del contrato
            java.util.Optional<LocalDate> fechaUltimaPagada = letras.stream()
                .filter(l -> l.getEstadoLetra() == EstadoLetra.PAGADO)
                .map(LetraCambio::getFechaVencimiento)
                .filter(f -> f != null)
                .max(java.util.Comparator.naturalOrder());

            // Marcar PENDIENTES vencidas como VENCIDO según punto de corte
            boolean letrasCambiadas = false;
            for (LetraCambio letra : letras) {
                if (letra.getEstadoLetra() != EstadoLetra.PENDIENTE) continue;
                if (letra.getFechaVencimiento() == null) continue;
                if (!letra.getFechaVencimiento().isBefore(hoy)) continue;

                // Con pagadas: ignorar letras anteriores o iguales a la última pagada
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
    }

    /** Llamado desde ContratoController: POST /api/contratos/scheduler/ejecutar */
    public void ejecutarManualmente() {
        log.info(">>> Scheduler ejecutado MANUALMENTE.");
        actualizarEstadosContratos();
    }
}