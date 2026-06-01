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

    @Query("SELECT pm FROM PagoMora pm " +
           "WHERE pm.comprobante.idComprobante = :idComprobante")
    List<PagoMora> findByComprobanteId(@Param("idComprobante") Long idComprobante);

   
    @Query("SELECT pm FROM PagoMora pm " +
           "WHERE pm.comprobante.numeroCompleto = :numeroCompleto")
    List<PagoMora> findByComprobanteNumeroCompleto(@Param("numeroCompleto") String numeroCompleto);

    // ─── Consultas para email ──────────────────────────────────────────────────

    @Query("SELECT pm FROM PagoMora pm " +
           "WHERE pm.fechaPago = :fecha")
    List<PagoMora> findByFechaPago(@Param("fecha") LocalDate fecha);

    // ─── Consultas para Dashboard de Ingresos Diarios ─────────────────────────

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

    // ── NUEVAS: Consultas para Reporte de Ingresos por Rango de Fechas ────────

    @Query("SELECT pm FROM PagoMora pm " +
           "JOIN FETCH pm.mora m " +
           "JOIN FETCH m.letra l " +
           "JOIN FETCH l.contrato co " +
           "LEFT JOIN FETCH co.clientes cc " +
           "LEFT JOIN FETCH cc.cliente cli " +
           "LEFT JOIN FETCH pm.comprobante c " +
           "WHERE pm.fechaPago BETWEEN :desde AND :hasta " +
           "ORDER BY pm.fechaPago ASC")
    List<PagoMora> findByFechaPagoBetween(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    /**
     * Suma total de pagos de moras dentro de un rango de fechas.
     */
    @Query(value =
        "SELECT COALESCE(SUM(importe_pagado), 0) " +
        "FROM pago_mora " +
        "WHERE fecha_pago BETWEEN :desde AND :hasta",
        nativeQuery = true)
    BigDecimal sumImportePagadoByRango(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    /**
     * Cuenta pagos de moras dentro de un rango de fechas.
     */
    @Query(value =
        "SELECT COUNT(*) " +
        "FROM pago_mora " +
        "WHERE fecha_pago BETWEEN :desde AND :hasta",
        nativeQuery = true)
    long countByFechaPagoBetween(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}