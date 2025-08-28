package com.Inmobiliaria.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.Inmobiliaria.demo.entity.Cliente;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

	 
    Cliente findByNumDoc(String numDocumento);

    // Consulta JPQL con concatenación para filtrar por nombres + apellidos o apellidos + nombres
    @Query("SELECT c FROM Cliente c WHERE LOWER(CONCAT(c.apellidos, ' ', c.nombre)) LIKE LOWER(CONCAT('%', :filtro, '%'))")
    List<Cliente> buscarPorApellidosYNombres(@Param("filtro") String filtro);

    Cliente findTopByOrderByIdClienteDesc();
    
    List<Cliente> findAll();
}