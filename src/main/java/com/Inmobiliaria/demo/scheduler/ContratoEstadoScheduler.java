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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    private static final int PAGE_SIZE = 50;

    private final ContratoRepository  contratoRepository;

    private final AtomicBoolean ejecutando = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    public void ejecutarAlArrancar() {
        log.info(">>> Scheduler EstadoContrato: ejecución inicial al arrancar el servidor...");
        actualizarEstadosContratos();
    }

    @Scheduled(cron = "0 0 15 * * *")
    @Scheduled(cron = "0 15 18 * * *")
    @Transactional
    public void actualizarEstadosContratos() {

        if (!ejecutando.compareAndSet(false, true)) {
            log.warn(">>> Scheduler EstadoContrato: ya hay una ejecución en curso, se omite esta llamada.");
            return;
        }

        try {
            log.info(">>> Scheduler EstadoContrato: iniciando revisión diaria...");

            LocalDate hoy = LocalDate.now();
            int actualizados = 0;
            int page = 0;
            Page<Integer> idsPagina;

            do {
                idsPagina = contratoRepository.findFinanciadosActivosId(PageRequest.of(page, PAGE_SIZE));

                for (Integer idContrato : idsPagina.getContent()) {
                    Contrato contrato = contratoRepository.findByIdConTodo(idContrato);
                    if (contrato == null) continue;

                    if (procesarContrato(contrato, hoy)) {
                        actualizados++;
                    }
                }

                page++;
                contratoRepository.flush();

            } while (idsPagina.hasNext());

            log.info(">>> Scheduler EstadoContrato: {} contrato(s) actualizado(s).", actualizados);

        } finally {
            ejecutando.set(false);
        }
    }

    private boolean procesarContrato(Contrato contrato, LocalDate hoy) {

        List<LetraCambio> letras = contrato.getLetrasCambio();
        if (letras == null || letras.isEmpty()) return false;

        EstadoContrato estadoActual = contrato.getEstadoContrato();

        int numUltimaPagada = letras.stream()
                .filter(l -> l.getEstadoLetra() == EstadoLetra.PAGADO)
                .mapToInt(l -> extraerNumeroLetra(l.getNumeroLetra()))
                .max()
                .orElse(0);

        boolean letrasCambiadas = false;
        for (LetraCambio letra : letras) {
            if (letra.getEstadoLetra() != EstadoLetra.PENDIENTE) continue;
            if (letra.getFechaVencimiento() == null) continue;
            if (!letra.getFechaVencimiento().isBefore(hoy)) continue;
            if (extraerNumeroLetra(letra.getNumeroLetra()) <= numUltimaPagada) continue;

            letra.setEstadoLetra(EstadoLetra.VENCIDO);
            letrasCambiadas = true;
        }

        long letrasVencidasReales = letras.stream()
                .filter(l -> l.getEstadoLetra() == EstadoLetra.VENCIDO)
                .filter(l -> extraerNumeroLetra(l.getNumeroLetra()) > numUltimaPagada)
                .count();

        EstadoContrato nuevoEstado = (letrasVencidasReales == 0)
                ? EstadoContrato.ACTIVO
                : EstadoContrato.MORA;

        if (nuevoEstado != estadoActual || letrasCambiadas) {
            contrato.setEstadoContrato(nuevoEstado);
            contratoRepository.save(contrato);
            if (nuevoEstado != estadoActual) {
                log.info("Contrato #{} {} → {} (ultima pagada N°{}, vencidas reales: {})",
                        contrato.getIdContrato(), estadoActual, nuevoEstado,
                        numUltimaPagada, letrasVencidasReales);
                return true;
            }
        }
        return false;
    }

    public void ejecutarManualmente() {
        log.info(">>> Scheduler ejecutado MANUALMENTE.");
        actualizarEstadosContratos();
    }

    private int extraerNumeroLetra(String numeroLetra) {
        if (numeroLetra == null || numeroLetra.isBlank()) return 0;
        String parte = numeroLetra.contains("/")
                ? numeroLetra.split("/")[0].trim()
                : numeroLetra.trim();
        try { return Integer.parseInt(parte); }
        catch (NumberFormatException e) { return 0; }
    }
}
