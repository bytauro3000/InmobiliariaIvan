package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.PagoInicial;
import com.Inmobiliaria.demo.enums.TipoOrigenComprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoInicialRepository extends JpaRepository<PagoInicial, Integer> {

    Optional<PagoInicial> findByContratoIdContrato(Integer idContrato);

    @Query("SELECT DISTINCT p FROM PagoInicial p " +
           "JOIN FETCH p.comprobante c " +
           "JOIN FETCH p.contrato co " +
           "LEFT JOIN FETCH co.lotes cl " +
           "LEFT JOIN FETCH cl.lote l " +
           "LEFT JOIN FETCH l.programa prog " +
           "WHERE c.tipoOrigen = :origen " +
           "ORDER BY p.fechaPago DESC")
    List<PagoInicial> findAllByComprobanteOrigen(@Param("origen") TipoOrigenComprobante origen);

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @Query(value =
        "SELECT COALESCE(SUM(importe_pagado), 0) " +
        "FROM pago_inicial " +
        "WHERE fecha_pago = :fecha",
        nativeQuery = true)
    BigDecimal sumImportePagadoByFecha(@Param("fecha") LocalDate fecha);

    @Query(value =
        "SELECT COUNT(*) " +
        "FROM pago_inicial " +
        "WHERE fecha_pago = :fecha",
        nativeQuery = true)
    long countByFechaPago(@Param("fecha") LocalDate fecha);

    // ── Reporte ingresos ──────────────────────────────────────────────────────

    @Query("SELECT DISTINCT p FROM PagoInicial p " +
           "JOIN FETCH p.contrato co " +
           "LEFT JOIN FETCH co.clientes cc " +
           "LEFT JOIN FETCH cc.cliente cli " +
           "LEFT JOIN FETCH p.comprobante c " +
           "WHERE p.fechaPago BETWEEN :desde AND :hasta " +
           "ORDER BY p.fechaPago ASC")
    List<PagoInicial> findByFechaPagoBetween(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query(value =
        "SELECT COALESCE(SUM(importe_pagado), 0) " +
        "FROM pago_inicial " +
        "WHERE fecha_pago BETWEEN :desde AND :hasta",
        nativeQuery = true)
    BigDecimal sumImportePagadoByRango(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query(value =
        "SELECT COUNT(*) " +
        "FROM pago_inicial " +
        "WHERE fecha_pago BETWEEN :desde AND :hasta",
        nativeQuery = true)
    long countByFechaPagoBetween(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    // ── ADMIN: Listado general con filtros opcionales ─────────────────────────

    @Query("SELECT DISTINCT p FROM PagoInicial p " +
           "JOIN FETCH p.contrato co " +
           "LEFT JOIN FETCH co.clientes cc " +
           "LEFT JOIN FETCH cc.cliente cli " +
           "LEFT JOIN FETCH p.comprobante c " +
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
           "AND   (:desde IS NULL OR p.fechaPago >= :desde) " +
           "AND   (:hasta IS NULL OR p.fechaPago <= :hasta) " +
           "ORDER BY p.fechaPago DESC")
    List<PagoInicial> findTodos(
            @Param("numeroComprobante") String numeroComprobante,
            @Param("manzana")          String manzana,
            @Param("numeroLote")       String numeroLote,
            @Param("idPrograma")       Integer idPrograma,
            @Param("desde")            LocalDate desde,
            @Param("hasta")            LocalDate hasta);
}