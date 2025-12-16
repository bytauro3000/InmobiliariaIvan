
package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.ContratoCliente;
import com.Inmobiliaria.demo.entity.ContratoClienteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContratoClienteRepository extends JpaRepository<ContratoCliente, ContratoClienteId> {
}
