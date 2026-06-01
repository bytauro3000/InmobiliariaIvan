package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.PagoLetras;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoLetraRepository extends JpaRepository<PagoLetras, Integer> {

    List<PagoLetras> findByLetraIdLetra(Integer idLetra);

    /**
     * Lista los pagos de un contrato ordenados por número de letra (parte numérica) de menor a mayor.
     */
    @Query(value =
        "SELECT p.* FROM pago_letra p " +
        "JOIN letra_cambio lc ON p.id_letra = lc.id_letra " +
        "WHERE lc.id_contrato = :idContrato " +
        "ORDER BY CAST(SUBSTRING_INDEX(lc.numero_letra, '/', 1) AS UNSIGNED) ASC",
        nativeQuery = true)
    List<PagoLetras> findByLetraContratoIdContrato(@Param("idContrato") Integer idContrato);

    long countByLetraIdLetra(Integer idLetra);

    @Query("SELECT p FROM PagoLetras p " +
           "JOIN FETCH p.comprobante c " +
           "WHERE c.numeroCompleto = :numeroCompleto")
    List<PagoLetras> findByComprobanteNumeroCompleto(@Param("numeroCompleto") String numeroCompleto);

    /**
     * Cuenta los pagos asociados a un mismo comprobante.
     */
    @Query("SELECT COUNT(p) FROM PagoLetras p " +
           "JOIN p.comprobante c " +
           "WHERE c.numeroCompleto = :numeroCompleto")
    long countByComprobanteNumeroCompleto(@Param("numeroCompleto") String numeroCompleto);

    // ── Consultas para scheduler y email ──────────────────────────────────────

    @Query("SELECT DISTINCT p FROM PagoLetras p " +
           "JOIN FETCH p.letra l " +
           "JOIN FETCH l.contrato c " +
           "WHERE p.fechaPago = :fecha")
    List<PagoLetras> findByFechaPago(@Param("fecha") LocalDate fecha);

    @Query("SELECT DISTINCT p FROM PagoLetras p " +
           "JOIN FETCH p.letra l " +
           "JOIN FETCH l.contrato c " +
           "LEFT JOIN FETCH c.clientes cc " +
           "LEFT JOIN FETCH cc.cliente " +
           "WHERE p.fechaPago = :fecha " +
           "AND (p.comprobante IS NULL OR p.comprobante.emailEnviado = false)")
    List<PagoLetras> findByFechaPagoAndEmailEnviadoFalse(@Param("fecha") LocalDate fecha);

    // ── Consultas para lógica de negocio (PagoLetraServiceImpl) ───────────────

    @Query(value =
        "SELECT MAX(CAST(SUBSTRING_INDEX(lc.numero_letra, '/', 1) AS UNSIGNED)) " +
        "FROM pago_letra pl " +
        "JOIN letra_cambio lc ON pl.id_letra = lc.id_letra " +
        "WHERE lc.id_contrato = :idContrato",
        nativeQuery = true)
    Optional<Integer> findMaxNumeroLetraPagadoByContrato(@Param("idContrato") Integer idContrato);

    // ── NUEVO: suma de importes ya pagados para una letra (para calcular saldo) ──

    @Query(value =
        "SELECT COALESCE(SUM(importe_pagado), 0) " +
        "FROM pago_letra " +
        "WHERE id_letra = :idLetra",
        nativeQuery = true)
    BigDecimal sumImportePagadoByLetra(@Param("idLetra") Integer idLetra);

    // ── Consultas para Dashboard de Ingresos Diarios ──────────────────────────

    @Query(value =
        "SELECT COALESCE(SUM(importe_pagado), 0) " +
        "FROM pago_letra " +
        "WHERE fecha_pago = :fecha",
        nativeQuery = true)
    BigDecimal sumImportePagadoByFecha(@Param("fecha") LocalDate fecha);

    @Query(value =
        "SELECT COUNT(*) " +
        "FROM pago_letra " +
        "WHERE fecha_pago = :fecha",
        nativeQuery = true)
    long countByFechaPago(@Param("fecha") LocalDate fecha);

    // ── NUEVAS: Consultas para Reporte de Ingresos por Rango de Fechas ────────

    @Query("SELECT DISTINCT p FROM PagoLetras p " +
           "JOIN FETCH p.letra l " +
           "JOIN FETCH l.contrato co " +
           "LEFT JOIN FETCH co.clientes cc " +
           "LEFT JOIN FETCH cc.cliente cli " +
           "LEFT JOIN FETCH p.comprobante c " +
           "WHERE p.fechaPago BETWEEN :desde AND :hasta " +
           "ORDER BY p.fechaPago ASC")
    List<PagoLetras> findByFechaPagoBetween(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    /**
     * Suma total de pagos de letras dentro de un rango de fechas.
     */
    @Query(value =
        "SELECT COALESCE(SUM(importe_pagado), 0) " +
        "FROM pago_letra " +
        "WHERE fecha_pago BETWEEN :desde AND :hasta",
        nativeQuery = true)
    BigDecimal sumImportePagadoByRango(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    /**
     * Cuenta pagos de letras dentro de un rango de fechas.
     */
    @Query(value =
        "SELECT COUNT(*) " +
        "FROM pago_letra " +
        "WHERE fecha_pago BETWEEN :desde AND :hasta",
        nativeQuery = true)
    long countByFechaPagoBetween(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}