
package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.ContratoCliente;
import com.Inmobiliaria.demo.entity.ContratoClienteId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ContratoClienteRepository extends JpaRepository<ContratoCliente, ContratoClienteId> {

    /** Batch: clientes (con su entidad Cliente) de varios contratos, ordenados por orden. */
    @EntityGraph(attributePaths = {"cliente"})
    List<ContratoCliente> findByContratoIdContratoInOrderByOrdenAsc(Collection<Integer> idContratos);
}
