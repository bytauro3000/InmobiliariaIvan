package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.MoraLetra;
import com.Inmobiliaria.demo.enums.EstadoMora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MoraRepository extends JpaRepository<MoraLetra, Integer> {

    // Todas las moras de una letra específica
    List<MoraLetra> findByLetraIdLetra(Integer idLetra);

    // Todas las moras de un contrato (para mostrar resumen total pendiente)
    @Query("SELECT m FROM MoraLetra m " +
           "JOIN FETCH m.letra l " +
           "WHERE l.contrato.idContrato = :idContrato " +
           "ORDER BY m.fechaGeneracion ASC")
    List<MoraLetra> findByContratoIdContrato(@Param("idContrato") Integer idContrato);

    // Solo las moras pendientes de un contrato (para calcular deuda acumulada)
    @Query("SELECT m FROM MoraLetra m " +
           "JOIN FETCH m.letra l " +
           "WHERE l.contrato.idContrato = :idContrato " +
           "AND m.estadoMora = :estado " +
           "ORDER BY m.fechaGeneracion ASC")
    List<MoraLetra> findByContratoIdContratoAndEstado(
            @Param("idContrato") Integer idContrato,
            @Param("estado") EstadoMora estado);

    // Verifica si ya existe una mora PENDIENTE o PAGADA para una letra
    @Query("SELECT COUNT(m) > 0 FROM MoraLetra m " +
           "WHERE m.letra.idLetra = :idLetra " +
           "AND m.estadoMora <> com.Inmobiliaria.demo.enums.EstadoMora.ANULADO")
    boolean existeMoraActivaParaLetra(@Param("idLetra") Integer idLetra);

    // Mora de una letra en estado específico
    Optional<MoraLetra> findByLetraIdLetraAndEstadoMora(Integer idLetra, EstadoMora estadoMora);

    // Suma total de moras pendientes de un contrato
    @Query("SELECT COALESCE(SUM(m.montoMoraTotal), 0) FROM MoraLetra m " +
           "JOIN m.letra l " +
           "WHERE l.contrato.idContrato = :idContrato " +
           "AND m.estadoMora = com.Inmobiliaria.demo.enums.EstadoMora.PENDIENTE")
    java.math.BigDecimal sumaMorasPendientesPorContrato(@Param("idContrato") Integer idContrato);

    // ── NUEVO: busca moras asociadas a un pago de letra específico ─────────
    // Usado en eliminarPago() para desvincular antes de borrar el pago
    List<MoraLetra> findByPagoLetraIdPago(Integer idPago);
}