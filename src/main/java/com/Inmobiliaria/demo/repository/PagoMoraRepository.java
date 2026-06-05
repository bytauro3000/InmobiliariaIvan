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

    List<PagoMora> findByMoraIdMora(Integer idMora);

    @Query("SELECT pm FROM PagoMora pm " +
           "JOIN pm.mora m " +
           "JOIN m.letra l " +
           "WHERE l.contrato.idContrato = :idContrato " +
           "ORDER BY pm.fechaPago DESC")
    List<PagoMora> findByContratoId(@Param("idContrato") Integer idContrato);

    @Query("SELECT pm FROM PagoMora pm " +
           "WHERE pm.comprobante.idComprobante = :idComprobante")
    List<PagoMora> findByComprobanteId(@Param("idComprobante") Long idComprobante);

    @Query("SELECT pm FROM PagoMora pm " +
           "WHERE pm.comprobante.numeroCompleto = :numeroCompleto")
    List<PagoMora> findByComprobanteNumeroCompleto(@Param("numeroCompleto") String numeroCompleto);

    @Query("SELECT pm FROM PagoMora pm " +
           "WHERE pm.fechaPago = :fecha")
    List<PagoMora> findByFechaPago(@Param("fecha") LocalDate fecha);

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @Query(value =
        "SELECT COALESCE(SUM(importe_pagado), 0) " +
        "FROM pago_mora " +
        "WHERE fecha_pago = :fecha",
        nativeQuery = true)
    BigDecimal sumImportePagadoByFecha(@Param("fecha") LocalDate fecha);

    @Query(value =
        "SELECT COUNT(*) " +
        "FROM pago_mora " +
        "WHERE fecha_pago = :fecha",
        nativeQuery = true)
    long countByFechaPago(@Param("fecha") LocalDate fecha);

    // ── Reporte ingresos ──────────────────────────────────────────────────────

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

    @Query(value =
        "SELECT COALESCE(SUM(importe_pagado), 0) " +
        "FROM pago_mora " +
        "WHERE fecha_pago BETWEEN :desde AND :hasta",
        nativeQuery = true)
    BigDecimal sumImportePagadoByRango(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query(value =
        "SELECT COUNT(*) " +
        "FROM pago_mora " +
        "WHERE fecha_pago BETWEEN :desde AND :hasta",
        nativeQuery = true)
    long countByFechaPagoBetween(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    // ── ADMIN: Listado general con filtros opcionales ─────────────────────────

    @Query("SELECT DISTINCT pm FROM PagoMora pm " +
           "JOIN FETCH pm.mora m " +
           "JOIN FETCH m.letra l " +
           "JOIN FETCH l.contrato co " +
           "LEFT JOIN FETCH co.clientes cc " +
           "LEFT JOIN FETCH cc.cliente cli " +
           "LEFT JOIN FETCH pm.comprobante c " +
           "LEFT JOIN FETCH co.lotes cl " +
           "LEFT JOIN FETCH cl.lote lot " +
           "LEFT JOIN FETCH lot.programa prog " +
           "WHERE (:numeroComprobante IS NULL OR " +
           "       (c IS NOT NULL AND LOWER(c.numeroCompleto) LIKE LOWER(CONCAT('%', :numeroComprobante, '%')))) " +
           "AND   (:manzana IS NULL OR " +
           "       (lot IS NOT NULL AND LOWER(lot.manzana) = LOWER(:manzana))) " +
           "AND   (:numeroLote IS NULL OR " +
           "       (lot IS NOT NULL AND LOWER(lot.numeroLote) = LOWER(:numeroLote))) " +
           "AND   (:idPrograma IS NULL OR " +
           "       (prog IS NOT NULL AND prog.idPrograma = :idPrograma)) " +
           "AND   (:desde IS NULL OR pm.fechaPago >= :desde) " +
           "AND   (:hasta IS NULL OR pm.fechaPago <= :hasta) " +
           "ORDER BY pm.fechaPago DESC")
    List<PagoMora> findTodos(
            @Param("numeroComprobante") String numeroComprobante,
            @Param("manzana")          String manzana,
            @Param("numeroLote")       String numeroLote,
            @Param("idPrograma")       Integer idPrograma,
            @Param("desde")            LocalDate desde,
            @Param("hasta")            LocalDate hasta);
}