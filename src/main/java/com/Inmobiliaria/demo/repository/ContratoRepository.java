// ContratoRepository.java
package com.Inmobiliaria.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.Inmobiliaria.demo.entity.Contrato;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Integer> {
	
	@Query("SELECT cl.lote.programa.nombrePrograma, c.tipoContrato, COUNT(c) " +
	           "FROM Contrato c JOIN c.lotes cl " +
	           "GROUP BY cl.lote.programa.nombrePrograma, c.tipoContrato")
	    List<Object[]> contarContratosPorProgramaYTipo();
	    
	List<Contrato> findAllByOrderByIdContratoDesc();
	
	
	@Query("SELECT COUNT(cl) > 0 FROM ContratoLote cl " +
	           "JOIN cl.lote l " +
	           "JOIN l.programa p " +
	           "WHERE p.idPrograma = :idPrograma " +
	           "AND l.manzana = :manzana " +
	           "AND l.numeroLote = :numeroLote")
	    boolean existeContratoDuplicado(
	        @Param("idPrograma") Integer idPrograma, 
	        @Param("manzana") String manzana, 
	        @Param("numeroLote") String numeroLote
	    );

}
