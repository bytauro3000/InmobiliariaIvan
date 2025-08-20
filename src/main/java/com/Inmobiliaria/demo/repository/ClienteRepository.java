package com.Inmobiliaria.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.Inmobiliaria.demo.entity.Cliente;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    @Query("SELECT c FROM Cliente c WHERE c.dni = :dni")
    Cliente buscarPorDni(@Param("dni") String dni);

    @Query("SELECT c FROM Cliente c WHERE LOWER(c.apellidos) = LOWER(:apellidos)")
    List<Cliente> buscarPorApellidos(@Param("apellidos") String apellidos);

    
    Cliente findByDni(String dni);
    Cliente findTopByOrderByIdClienteDesc();
    
    List<Cliente> findAll();
}