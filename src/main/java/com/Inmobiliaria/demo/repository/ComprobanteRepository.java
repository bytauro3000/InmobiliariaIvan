package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.Comprobante;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.enums.TipoOrigenComprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    // ─── Búsqueda por número completo (ej: "EB01-4001") ───────────────────────

    Optional<Comprobante> findByNumeroCompleto(String numeroCompleto);

    boolean existsByNumeroCompleto(String numeroCompleto);

    // ─── Listado por tipo y origen ─────────────────────────────────────────────

    List<Comprobante> findByTipoComprobanteOrderByNumeroDesc(TipoComprobante tipoComprobante);

    List<Comprobante> findByTipoOrigenOrderByFechaEmisionDesc(TipoOrigenComprobante tipoOrigen);

    // ─── Comprobantes por origen y referencia (para vincular con la entidad) ───

    Optional<Comprobante> findByTipoOrigenAndReferenciaId(
            TipoOrigenComprobante tipoOrigen,
            Integer referenciaId);

    List<Comprobante> findByTipoOrigenAndReferenciaIdIn(
            TipoOrigenComprobante tipoOrigen,
            List<Integer> referenciaIds);

    // ─── Comprobantes pendientes de envío por email ────────────────────────────

    @Query("SELECT c FROM Comprobante c " +
           "WHERE c.emailEnviado = false " +
           "AND c.fechaEmision = :fecha")
    List<Comprobante> findPendientesEmailPorFecha(@Param("fecha") LocalDate fecha);

    @Query("SELECT c FROM Comprobante c " +
           "WHERE c.emailEnviado = false")
    List<Comprobante> findAllPendientesEmail();

    // ─── Rangos de fecha para reportes ────────────────────────────────────────

    @Query("SELECT c FROM Comprobante c " +
           "WHERE c.fechaEmision BETWEEN :desde AND :hasta " +
           "ORDER BY c.fechaEmision DESC, c.numero DESC")
    List<Comprobante> findByFechaEmisionBetween(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query("SELECT c FROM Comprobante c " +
           "WHERE c.tipoComprobante = :tipo " +
           "AND c.fechaEmision BETWEEN :desde AND :hasta " +
           "ORDER BY c.numero DESC")
    List<Comprobante> findByTipoAndFecha(
            @Param("tipo") TipoComprobante tipo,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    // --- Numero maximo emitido por tipo (incluyendo manuales) ----------------
    // Usado para garantizar que el siguiente numero automatico siempre sea
    // mayor al numero mas alto existente en la BD (incluye numeros manuales).

    @Query("SELECT COALESCE(MAX(c.numero), 0) FROM Comprobante c " +
           "WHERE c.tipoComprobante = :tipo AND c.serie = :serie")
    Integer findMaxNumeroByTipoAndSerie(
            @Param("tipo") TipoComprobante tipo,
            @Param("serie") String serie);

    Optional<Comprobante> findByComprobanteReferenciaIdComprobante(Long idComprobante);
}