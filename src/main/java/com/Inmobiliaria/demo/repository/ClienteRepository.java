package com.Inmobiliaria.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.Inmobiliaria.demo.entity.Cliente;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

	 
	//Filtro específico para Nombres + Apellidos
    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.distrito WHERE " +
           "LOWER(CONCAT(c.nombre, ' ', c.apellidos)) LIKE LOWER(CONCAT('%', :filtro, '%'))")
    List<Cliente> buscarPorNombresYApellidos(@Param("filtro") String filtro);
    
    //Filtro específico para Documento (usando LIKE para que sea búsqueda parcial)
    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.distrito WHERE c.numDoc LIKE CONCAT('%', :filtro, '%')")
    List<Cliente> buscarPorDocumento(@Param("filtro") String filtro);
    
    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.distrito WHERE c.numDoc = :numDocumento")
    Cliente findByNumDoc(@Param("numDocumento") String numDocumento);

    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.distrito WHERE c.idCliente = :id")
    Cliente findByIdWithDistrito(@Param("id") Integer id);
    
    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.distrito ORDER BY c.idCliente DESC")
    List<Cliente> findAllByOrderByIdClienteDesc();
}