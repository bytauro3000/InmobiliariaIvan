package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.ComisionVendedor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComisionVendedorRepository extends JpaRepository<ComisionVendedor, Integer> {

    Optional<ComisionVendedor> findByContratoIdContrato(Integer idContrato);

    @EntityGraph(attributePaths = {"contrato", "vendedor"})
    List<ComisionVendedor> findAllByOrderByIdComisionDesc();

    @EntityGraph(attributePaths = {"contrato", "vendedor"})
    List<ComisionVendedor> findByContratoEstadoContratoIn(java.util.Collection<com.Inmobiliaria.demo.enums.EstadoContrato> estados);

    boolean existsByContratoIdContrato(Integer idContrato);
}