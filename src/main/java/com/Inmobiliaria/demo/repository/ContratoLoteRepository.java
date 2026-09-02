
package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.ContratoLote;
import com.Inmobiliaria.demo.entity.ContratoLoteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContratoLoteRepository extends JpaRepository<ContratoLote, ContratoLoteId> {

    @Query("SELECT cl.lote.programa FROM ContratoLote cl WHERE cl.contrato.idContrato = :idContrato")
    List<com.Inmobiliaria.demo.entity.Programa> findProgramasByContrato(@Param("idContrato") Integer idContrato);

    @Query("SELECT cl.lote FROM ContratoLote cl WHERE cl.contrato.idContrato = :idContrato")
    List<com.Inmobiliaria.demo.entity.Lote> findLotesByContrato(@Param("idContrato") Integer idContrato);

    /** Batch: manzana y número de lote por contrato (para listado de comisiones). */
    @Query("SELECT cl.contrato.idContrato, cl.lote.manzana, cl.lote.numeroLote " +
           "FROM ContratoLote cl WHERE cl.contrato.idContrato IN :ids")
    List<Object[]> findLotesByContratos(@Param("ids") java.util.Collection<Integer> ids);

    /** Batch: programa (objeto completo) por contrato (distinto). */
    @Query("SELECT DISTINCT cl.contrato.idContrato, cl.lote.programa " +
           "FROM ContratoLote cl WHERE cl.contrato.idContrato IN :ids")
    List<Object[]> findProgramasByContratos(@Param("ids") java.util.Collection<Integer> ids);
}
