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

    // Solo cargamos letra y contrato — clientes y lotes se cargan lazy cuando se necesiten
    // Evita MultipleBagFetchException de Hibernate al hacer JOIN FETCH de dos List<> simultáneas
    // Buscar todos los pagos con el mismo número de comprobante (para comprobante múltiple)
    List<PagoLetras> findByNumeroComprobante(String numeroComprobante);

    // Contar pagos con el mismo número de comprobante (para detectar pago múltiple)
    long countByNumeroComprobante(String numeroComprobante);

    @Query("SELECT DISTINCT p FROM PagoLetras p " +
           "JOIN FETCH p.letra l " +
           "JOIN FETCH l.contrato c " +
           "WHERE p.fechaPago = :fecha")
    List<PagoLetras> findByFechaPago(@Param("fecha") LocalDate fecha);

    // Para el scheduler: pagos del día anterior que aún no tuvieron email enviado
    @Query("SELECT DISTINCT p FROM PagoLetras p " +
           "JOIN FETCH p.letra l " +
           "JOIN FETCH l.contrato c " +
           "WHERE p.fechaPago = :fecha AND p.emailEnviado = false")
    List<PagoLetras> findByFechaPagoAndEmailEnviadoFalse(@Param("fecha") LocalDate fecha);
}