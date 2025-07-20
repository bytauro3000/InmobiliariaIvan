// ContratoRepository.java
package com.Inmobiliaria.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.Inmobiliaria.demo.entity.Contrato;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Integer> {
}
