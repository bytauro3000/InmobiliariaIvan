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

    /** Todas las comisiones de un contrato (para eliminarlas al borrar el contrato). */
    List<ComisionVendedor> findAllByContratoIdContrato(Integer idContrato);

    @EntityGraph(attributePaths = {"contrato", "vendedor"})
    List<ComisionVendedor> findAllByOrderByIdComisionDesc();

    /** Comisiones ordenadas por fecha de contrato, de la más reciente a la más antigua. */
    @EntityGraph(attributePaths = {"contrato", "vendedor"})
    List<ComisionVendedor> findAllByOrderByContratoFechaContratoDescIdComisionDesc();

    @EntityGraph(attributePaths = {"contrato", "vendedor"})
    List<ComisionVendedor> findByContratoEstadoContratoIn(java.util.Collection<com.Inmobiliaria.demo.enums.EstadoContrato> estados);

    boolean existsByContratoIdContrato(Integer idContrato);

    @EntityGraph(attributePaths = {"contrato", "vendedor"})
    List<ComisionVendedor> findByVendedorIdVendedor(Integer idVendedor);

    @EntityGraph(attributePaths = {"contrato", "vendedor"})
    List<ComisionVendedor> findByVendedorIdVendedorAndSaldoPendienteGreaterThan(Integer idVendedor, java.math.BigDecimal saldo);
}