
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
}
