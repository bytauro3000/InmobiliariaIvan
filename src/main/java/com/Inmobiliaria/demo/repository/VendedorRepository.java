package com.Inmobiliaria.demo.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Inmobiliaria.demo.entity.Vendedor;

@Repository
public interface VendedorRepository extends JpaRepository<Vendedor, Integer> {

    Optional<Vendedor> findByIdUsuario(Integer idUsuario);
}

