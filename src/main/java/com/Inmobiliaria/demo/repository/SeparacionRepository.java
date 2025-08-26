package com.Inmobiliaria.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.Inmobiliaria.demo.dto.SeparacionDTO;
import com.Inmobiliaria.demo.entity.Separacion;

public interface SeparacionRepository extends JpaRepository<Separacion, Integer> {

	@Query("SELECT new com.Inmobiliaria.demo.dto.SeparacionDTO(s.idSeparacion, CONCAT(s.cliente.nombre, ' ', s.cliente.apellidos, ' - DNI: ', s.cliente.numDoc, ' - Mz ', s.lote.manzana, ' Lt ', s.lote.numeroLote)) " +
	           "FROM Separacion s WHERE LOWER(s.cliente.apellidos) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
	           "OR s.cliente.numDoc LIKE CONCAT('%', :filtro, '%')")
	    List<SeparacionDTO> buscarPorDniOApellido(@Param("filtro") String filtro);
}
