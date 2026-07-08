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

    @Query("SELECT COUNT(p) FROM PagoLetras p " +
           "JOIN p.comprobante c " +
           "WHERE c.numeroCompleto = :numeroCompleto")
    long countByComprobanteNumeroCompleto(@Param("numeroCompleto") String numeroCompleto);

    @Query("SELECT p FROM PagoLetras p WHERE p.comprobante.idComprobante = :idComprobante")
    List<PagoLetras> findByComprobanteIdComprobante(@Param("idComprobante") Long idComprobante);

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

    // ── Consultas para lógica de negocio ──────────────────────────────────────

    @Query(value =
        "SELECT MAX(CAST(SUBSTRING_INDEX(lc.numero_letra, '/', 1) AS UNSIGNED)) " +
        "FROM pago_letra pl " +
        "JOIN letra_cambio lc ON pl.id_letra = lc.id_letra " +
        "WHERE lc.id_contrato = :idContrato " +
        "AND (pl.anulado = false OR pl.anulado IS NULL)",
        nativeQuery = true)
    Optional<Integer> findMaxNumeroLetraPagadoByContrato(@Param("idContrato") Integer idContrato);

    @Query(value =
        "SELECT COALESCE(SUM(importe_pagado), 0) " +
        "FROM pago_letra " +
        "WHERE id_letra = :idLetra",
        nativeQuery = true)
    BigDecimal sumImportePagadoByLetra(@Param("idLetra") Integer idLetra);

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @Query(value =
        "SELECT COALESCE(SUM(importe_pagado), 0) " +
        "FROM pago_letra " +
        "WHERE fecha_pago = :fecha " +
        "AND (anulado = false OR anulado IS NULL)",
        nativeQuery = true)
    BigDecimal sumImportePagadoByFecha(@Param("fecha") LocalDate fecha);

    @Query(value =
        "SELECT COUNT(*) " +
        "FROM pago_letra " +
        "WHERE fecha_pago = :fecha " +
        "AND (anulado = false OR anulado IS NULL)",
        nativeQuery = true)
    long countByFechaPago(@Param("fecha") LocalDate fecha);

    // ── Reporte ingresos ──────────────────────────────────────────────────────

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

    @Query(value =
        "SELECT MONTH(fecha_pago) AS mes, YEAR(fecha_pago) AS anio, " +
        "COALESCE(SUM(importe_pagado), 0) AS total " +
        "FROM pago_letra " +
        "WHERE fecha_pago BETWEEN :desde AND :hasta " +
        "AND (anulado = false OR anulado IS NULL) " +
        "GROUP BY YEAR(fecha_pago), MONTH(fecha_pago) " +
        "ORDER BY anio, mes",
        nativeQuery = true)
    List<Object[]> sumImportePagadoGroupedByMonth(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query(value =
        "SELECT COALESCE(SUM(importe_pagado), 0) " +
        "FROM pago_letra " +
        "WHERE fecha_pago BETWEEN :desde AND :hasta " +
        "AND (anulado = false OR anulado IS NULL)",
        nativeQuery = true)
    BigDecimal sumImportePagadoByRango(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query(value =
        "SELECT COUNT(*) " +
        "FROM pago_letra " +
        "WHERE fecha_pago BETWEEN :desde AND :hasta " +
        "AND (anulado = false OR anulado IS NULL)",
        nativeQuery = true)
    long countByFechaPagoBetween(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query("SELECT COALESCE(SUM(p.importePagado), 0) FROM PagoLetras p " +
           "WHERE p.letra.idLetra = :idLetra AND (p.anulado = false OR p.anulado IS NULL)")
    BigDecimal sumImportePagadoActivoByLetra(@Param("idLetra") Integer idLetra);

    @Query("SELECT COUNT(p) FROM PagoLetras p " +
           "WHERE p.letra.idLetra = :idLetra AND (p.anulado = false OR p.anulado IS NULL)")
    long countActivosByLetraIdLetra(@Param("idLetra") Integer idLetra);

    // ── ADMIN: Listado general — QUERY 1: trae pagos + lotes + programa ────────
    // Se separó en dos queries para evitar MultipleBagFetchException.
    // Hibernate no permite hacer JOIN FETCH de dos List<> (bags) en la misma query.
    // Solución: una query trae lotes, otra trae clientes; se combinan en el servicio.
    // Los JOIN FETCH de @ManyToOne (distrito, separacion, vendedor, usuario) evitan
    // queries EAGER adicionales por cada fila.
    @Query("SELECT DISTINCT p FROM PagoLetras p " +
           "JOIN FETCH p.letra l " +
           "LEFT JOIN FETCH l.distrito " +
           "JOIN FETCH l.contrato co " +
           "LEFT JOIN FETCH co.separacion " +
           "LEFT JOIN FETCH co.vendedor " +
           "LEFT JOIN FETCH co.usuario " +
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
            "ORDER BY p.fechaPago DESC, c.tipoComprobante, c.numeroCompleto")
    List<PagoLetras> findTodosConLotes(
            @Param("numeroComprobante") String numeroComprobante,
            @Param("manzana")          String manzana,
            @Param("numeroLote")       String numeroLote,
            @Param("idPrograma")       Integer idPrograma,
            @Param("desde")            LocalDate desde,
            @Param("hasta")            LocalDate hasta);
}