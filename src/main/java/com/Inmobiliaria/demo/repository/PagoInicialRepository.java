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

    /**
     * Suma total de pagos iniciales dentro de un rango de fechas.
     */
    @Query(value =
        "SELECT COALESCE(SUM(importe_pagado), 0) " +
        "FROM pago_inicial " +
        "WHERE fecha_pago BETWEEN :desde AND :hasta",
        nativeQuery = true)
    BigDecimal sumImportePagadoByRango(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    /**
     * Cuenta pagos iniciales dentro de un rango de fechas.
     */
    @Query(value =
        "SELECT COUNT(*) " +
        "FROM pago_inicial " +
        "WHERE fecha_pago BETWEEN :desde AND :hasta",
        nativeQuery = true)
    long countByFechaPagoBetween(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}