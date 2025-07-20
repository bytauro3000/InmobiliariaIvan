package com.Inmobiliaria.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.Inmobiliaria.demo.entity.Cliente;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    @Query(value = "SELECT * FROM Cliente WHERE dni = :dni", nativeQuery = true)
    Cliente buscarClientePorDni(@Param("dni") String dni);

    @Query(value = "SELECT * FROM Cliente WHERE apellidos = :apellidos", nativeQuery = true)
    List<Cliente> buscarClientesPorApellido(@Param("apellidos") String apellidos);

    @Query(value = "SELECT * FROM Cliente ORDER BY id_cliente DESC LIMIT 1", nativeQuery = true)
    Cliente obtenerUltimoCliente();

    @Query(value = "SELECT * FROM Cliente ORDER BY id_cliente ASC", nativeQuery = true)
    List<Cliente> listarClientesOrdenados();
}

