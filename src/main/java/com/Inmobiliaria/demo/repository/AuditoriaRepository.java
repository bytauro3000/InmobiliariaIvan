package com.Inmobiliaria.demo.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.Inmobiliaria.demo.entity.Auditoria;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long>,
        JpaSpecificationExecutor<Auditoria> {

    /**
     * Registros anteriores a la fecha límite de retención.
     * Usado por AuditoriaPurgaScheduler (borrado por lotes).
     */
    long deleteByFechaHoraBefore(LocalDateTime fechaLimite);
}
