package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.PagoInscripcionComprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoInscripcionComprobanteRepository
        extends JpaRepository<PagoInscripcionComprobante, Integer> {

    @Query("SELECT DISTINCT p FROM PagoInscripcionComprobante p " +
           "JOIN FETCH p.comprobante c " +
           "JOIN FETCH p.contrato co " +
           "LEFT JOIN FETCH co.lotes cl " +
           "LEFT JOIN FETCH cl.lote l " +
           "LEFT JOIN FETCH l.programa prog " +
            "ORDER BY p.fechaPago DESC, c.tipoComprobante, c.numeroCompleto")
    List<PagoInscripcionComprobante> findAllConDetalle();

    @Query("SELECT p FROM PagoInscripcionComprobante p " +
           "JOIN FETCH p.comprobante c " +
           "JOIN FETCH p.contrato co " +
           "LEFT JOIN FETCH co.clientes cc " +
           "LEFT JOIN FETCH cc.cliente cli " +
           "LEFT JOIN FETCH co.usuario u " +
           "WHERE p.idPagoInscripcionComprobante = :id")
    Optional<PagoInscripcionComprobante> findByIdConClientesYComprobante(Integer id);

    @Query("SELECT p FROM PagoInscripcionComprobante p " +
           "JOIN FETCH p.contrato co " +
           "LEFT JOIN FETCH co.lotes cl " +
           "LEFT JOIN FETCH cl.lote l " +
           "LEFT JOIN FETCH l.programa prog " +
           "WHERE p.idPagoInscripcionComprobante = :id")
    Optional<PagoInscripcionComprobante> findByIdConLotes(Integer id);

    @Query("SELECT p FROM PagoInscripcionComprobante p " +
           "JOIN FETCH p.comprobante c " +
           "JOIN FETCH p.contrato co " +
           "LEFT JOIN FETCH co.clientes cc " +
           "LEFT JOIN FETCH cc.cliente cli " +
           "WHERE p.fechaPago >= :desde AND p.fechaPago <= :hasta " +
           "ORDER BY p.fechaPago ASC")
    List<PagoInscripcionComprobante> findByFechaPagoBetween(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    // ── ADMIN: Listado general con filtros opcionales ─────────────────────────

    @Query("SELECT DISTINCT p FROM PagoInscripcionComprobante p " +
           "JOIN FETCH p.comprobante c " +
           "JOIN FETCH p.contrato co " +
           "LEFT JOIN FETCH co.separacion " +
           "LEFT JOIN FETCH co.vendedor " +
           "LEFT JOIN FETCH co.usuario " +
           "LEFT JOIN FETCH co.clientes cc " +
           "LEFT JOIN FETCH cc.cliente cli " +
           "LEFT JOIN FETCH co.lotes cl " +
           "LEFT JOIN FETCH cl.lote lot " +
           "LEFT JOIN FETCH lot.programa prog " +
           "WHERE (:numeroComprobante IS NULL OR " +
           "       LOWER(c.numeroCompleto) LIKE LOWER(CONCAT('%', :numeroComprobante, '%'))) " +
           "AND   (:manzana IS NULL OR " +
           "       (lot IS NOT NULL AND LOWER(lot.manzana) = LOWER(:manzana))) " +
           "AND   (:numeroLote IS NULL OR " +
           "       (lot IS NOT NULL AND LOWER(lot.numeroLote) = LOWER(:numeroLote))) " +
           "AND   (:idPrograma IS NULL OR " +
           "       (prog IS NOT NULL AND prog.idPrograma = :idPrograma)) " +
           "AND   (:desde IS NULL OR p.fechaPago >= :desde) " +
           "AND   (:hasta IS NULL OR p.fechaPago <= :hasta) " +
            "ORDER BY p.fechaPago DESC, c.tipoComprobante, c.numeroCompleto")
    List<PagoInscripcionComprobante> findTodos(
            @Param("numeroComprobante") String numeroComprobante,
            @Param("manzana")          String manzana,
            @Param("numeroLote")       String numeroLote,
            @Param("idPrograma")       Integer idPrograma,
            @Param("desde")            LocalDate desde,
            @Param("hasta")            LocalDate hasta);
}