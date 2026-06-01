package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.PagoInscripcionComprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
           "ORDER BY p.fechaPago DESC")
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
}