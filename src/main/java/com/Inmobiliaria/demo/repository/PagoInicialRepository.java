package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.PagoInicial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PagoInicialRepository extends JpaRepository<PagoInicial, Integer> {

    Optional<PagoInicial> findByContratoIdContrato(Integer idContrato);
}