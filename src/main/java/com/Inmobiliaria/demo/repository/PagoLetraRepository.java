package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.PagoLetras;
import com.Inmobiliaria.demo.enums.TipoComprobante;

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

    List<PagoLetras> findByLetraContratoIdContrato(Integer idContrato);

    long countByLetraIdLetra(Integer idLetra);

    Optional<PagoLetras> findFirstByTipoComprobanteOrderByFechaOperacionDesc(TipoComprobante tipoComprobante);

    boolean existsByTipoComprobanteAndNumeroComprobante(TipoComprobante tipoComprobante, String numeroComprobante);

    boolean existsByTipoComprobanteAndNumeroComprobanteAndIdPagoNot(TipoComprobante tipoComprobante, String numeroComprobante, Integer idPago);

    List<PagoLetras> findByNumeroComprobante(String numeroComprobante);

    long countByNumeroComprobante(String numeroComprobante);

    @Query("SELECT DISTINCT p FROM PagoLetras p " +
           "JOIN FETCH p.letra l " +
           "JOIN FETCH l.contrato c " +
           "WHERE p.fechaPago = :fecha")
    List<PagoLetras> findByFechaPago(@Param("fecha") LocalDate fecha);

    @Query("SELECT DISTINCT p FROM PagoLetras p " +
           "JOIN FETCH p.letra l " +
           "JOIN FETCH l.contrato c " +
           "WHERE p.fechaPago = :fecha AND p.emailEnviado = false")
    List<PagoLetras> findByFechaPagoAndEmailEnviadoFalse(@Param("fecha") LocalDate fecha);

    @Query(value =
        "SELECT MAX(CAST(SUBSTRING_INDEX(lc.numero_letra, '/', 1) AS UNSIGNED)) " +
        "FROM pago_letra pl " +
        "JOIN letra_cambio lc ON pl.id_letra = lc.id_letra " +
        "WHERE lc.id_contrato = :idContrato",
        nativeQuery = true)
    Optional<Integer> findMaxNumeroLetraPagadoByContrato(@Param("idContrato") Integer idContrato);
}