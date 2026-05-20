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

    /**
     * Lista todos los PagoInicial cuyo comprobante tiene tipoOrigen = PAGO_INSCRIPCION.
     * Hace JOIN FETCH de contrato → lotes → lote → programa para poder mostrar
     * Mz-Lt y filtrar por programa en la pantalla "Pagos de Inscripciones".
     * Se usa LEFT JOIN FETCH en la cadena de lotes para no excluir contratos
     * que (en casos excepcionales) no tengan lote asignado.
     */
    @Query("SELECT DISTINCT p FROM PagoInicial p " +
           "JOIN FETCH p.comprobante c " +
           "JOIN FETCH p.contrato co " +
           "LEFT JOIN FETCH co.lotes cl " +
           "LEFT JOIN FETCH cl.lote l " +
           "LEFT JOIN FETCH l.programa prog " +
           "WHERE c.tipoOrigen = :origen " +
           "ORDER BY p.fechaPago DESC")
    List<PagoInicial> findAllByComprobanteOrigen(@Param("origen") TipoOrigenComprobante origen);

    // ─── Consultas para Dashboard de Ingresos Diarios ─────────────────────────

    /**
     * Suma total de importes pagados en pago_inicial para una fecha específica.
     * Incluye tanto CONTADO como FINANCIADO (iniciales de contratos).
     * Devuelve 0 si no hay registros (COALESCE).
     */
    @Query(value =
        "SELECT COALESCE(SUM(importe_pagado), 0) " +
        "FROM pago_inicial " +
        "WHERE fecha_pago = :fecha",
        nativeQuery = true)
    BigDecimal sumImportePagadoByFecha(@Param("fecha") LocalDate fecha);

    /**
     * Cuenta la cantidad de pagos de iniciales registrados en una fecha específica.
     */
    @Query(value =
        "SELECT COUNT(*) " +
        "FROM pago_inicial " +
        "WHERE fecha_pago = :fecha",
        nativeQuery = true)
    long countByFechaPago(@Param("fecha") LocalDate fecha);
}