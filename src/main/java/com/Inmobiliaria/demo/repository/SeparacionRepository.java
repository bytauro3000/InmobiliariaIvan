package com.Inmobiliaria.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.Inmobiliaria.demo.dto.SeparacionDTO;
import com.Inmobiliaria.demo.dto.SeparacionResumenDTO;
import com.Inmobiliaria.demo.entity.Separacion;

public interface SeparacionRepository extends JpaRepository<Separacion, Integer> {

	@Query("SELECT new com.Inmobiliaria.demo.dto.SeparacionDTO(s.idSeparacion, CONCAT(s.cliente.nombre, ' ', s.cliente.apellidos, ' - DNI: ', s.cliente.numDoc, ' - Mz ', s.lote.manzana, ' Lt ', s.lote.numeroLote)) " +
	           "FROM Separacion s WHERE LOWER(s.cliente.apellidos) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
	           "OR s.cliente.numDoc LIKE CONCAT('%', :filtro, '%')")
	    List<SeparacionDTO> buscarPorDniOApellido(@Param("filtro") String filtro);
	@Query("""
		    SELECT new com.Inmobiliaria.demo.dto.SeparacionResumenDTO(
		        s.idSeparacion,
		        c.numDoc,
		        CONCAT(
		            COALESCE(c.nombre, ''), ' ',
		            COALESCE(c.apellidos, '')
		        ),
		        CONCAT(
		            COALESCE(l.manzana, ''), '-', COALESCE(l.numeroLote, '')
		        ),
		        s.monto,
		        s.fechaSeparacion,
		        s.fechaLimite,
		        s.estado
		    )
		    FROM Separacion s
		    JOIN s.cliente c
		    JOIN s.lote l
		""")
		List<SeparacionResumenDTO> obtenerResumenSeparaciones();

}
