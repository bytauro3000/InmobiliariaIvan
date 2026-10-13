package com.Inmobiliaria.demo.scheduler;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.Inmobiliaria.demo.repository.AuditoriaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Retención de la bitácora: el primer día de cada mes a las 03:00 se
 * eliminan los registros anteriores a la retención configurada
 * (Ley N.° 29733 de protección de datos: se recomienda conservar 7 años).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditoriaPurgaScheduler {

    private final AuditoriaRepository auditoriaRepository;

    @Value("${app.auditoria.retencion-meses:84}")
    private int retencionMeses;

    @Scheduled(cron = "0 0 3 1 * ?", zone = "America/Lima")
    @Transactional
    public void purgar() {
        try {
            LocalDateTime fechaLimite = LocalDateTime.now().minusMonths(retencionMeses);
            long eliminados = auditoriaRepository.deleteByFechaHoraBefore(fechaLimite);
            if (eliminados > 0) {
                log.info("Auditoría: purgados {} registros anteriores a {}", eliminados, fechaLimite);
            }
        } catch (Exception e) {
            log.error("Auditoría: falló la purga de retención: {}", e.getMessage());
        }
    }
}
