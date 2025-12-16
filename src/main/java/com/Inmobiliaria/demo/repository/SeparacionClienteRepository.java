package com.Inmobiliaria.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.Inmobiliaria.demo.entity.SeparacionCliente;
import com.Inmobiliaria.demo.entity.SeparacionClienteId;

@Repository
public interface SeparacionClienteRepository extends JpaRepository<SeparacionCliente, SeparacionClienteId>{

}
