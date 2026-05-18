package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.PagoLetras;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    /**
     * Busca pagos de una fecha cuyo comprobante aún no fue enviado por email.
     * Usa comprobante.emailEnviado como fuente de verdad (campo unificado).
     * También incluye pagos sin comprobante para no dejarlos fuera del proceso.
     */
    @Query("SELECT DISTINCT p FROM PagoLetras p " +
           "JOIN FETCH p.letra l " +
           "JOIN FETCH l.contrato c " +
           "WHERE p.fechaPago = :fecha " +
           "AND (p.comprobante IS NULL OR p.comprobante.emailEnviado = false)")
    List<PagoLetras> findByFechaPagoAndEmailEnviadoFalse(@Param("fecha") LocalDate fecha);

    // ── Utilidad para validar orden de pago ───────────────────────────────────

    @Query(value =
        "SELECT MAX(CAST(SUBSTRING_INDEX(lc.numero_letra, '/', 1) AS UNSIGNED)) " +
        "FROM pago_letra pl " +
        "JOIN letra_cambio lc ON pl.id_letra = lc.id_letra " +
        "WHERE lc.id_contrato = :idContrato",
        nativeQuery = true)
    Optional<Integer> findMaxNumeroLetraPagadoByContrato(@Param("idContrato") Integer idContrato);
}