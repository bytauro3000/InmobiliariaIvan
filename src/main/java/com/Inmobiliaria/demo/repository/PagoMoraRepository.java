package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.PagoMora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PagoMoraRepository extends JpaRepository<PagoMora, Integer> {

    // ─── Consultas básicas ─────────────────────────────────────────────────────

    /** Todos los pagos registrados para una mora específica */
    List<PagoMora> findByMoraIdMora(Integer idMora);

    /** Pagos de mora de un contrato específico (a través de la letra) */
    @Query("SELECT pm FROM PagoMora pm " +
           "JOIN pm.mora m " +
           "JOIN m.letra l " +
           "WHERE l.contrato.idContrato = :idContrato " +
           "ORDER BY pm.fechaPago DESC")
    List<PagoMora> findByContratoId(@Param("idContrato") Integer idContrato);

    // ─── Consultas vinculadas al comprobante centralizado ──────────────────────

    /**
     * Busca todos los pagos de mora asociados a un comprobante específico.
     * Reemplaza la búsqueda anterior por numero_comprobante (campo eliminado).
     */
    @Query("SELECT pm FROM PagoMora pm " +
           "WHERE pm.comprobante.idComprobante = :idComprobante")
    List<PagoMora> findByComprobanteId(@Param("idComprobante") Long idComprobante);

    /**
     * Busca pagos de mora cuyo comprobante tiene el numero_completo dado (ej: "EB01-4001").
     * Útil para verificar que no se registre el mismo comprobante dos veces.
     */
    @Query("SELECT pm FROM PagoMora pm " +
           "WHERE pm.comprobante.numeroCompleto = :numeroCompleto")
    List<PagoMora> findByComprobanteNumeroCompleto(@Param("numeroCompleto") String numeroCompleto);

    // ─── Consultas para email ──────────────────────────────────────────────────

    @Query("SELECT pm FROM PagoMora pm " +
           "WHERE pm.fechaPago = :fecha")
    List<PagoMora> findByFechaPago(@Param("fecha") LocalDate fecha);

    // ─── Consultas para Dashboard de Ingresos Diarios ─────────────────────────

    /**
     * Suma total de importes pagados en pago_mora para una fecha específica.
     * Devuelve 0 si no hay registros (COALESCE).
     */
    @Query(value =
        "SELECT COALESCE(SUM(importe_pagado), 0) " +
        "FROM pago_mora " +
        "WHERE fecha_pago = :fecha",
        nativeQuery = true)
    BigDecimal sumImportePagadoByFecha(@Param("fecha") LocalDate fecha);

    /**
     * Cuenta la cantidad de pagos de moras registrados en una fecha específica.
     */
    @Query(value =
        "SELECT COUNT(*) " +
        "FROM pago_mora " +
        "WHERE fecha_pago = :fecha",
        nativeQuery = true)
    long countByFechaPago(@Param("fecha") LocalDate fecha);
}