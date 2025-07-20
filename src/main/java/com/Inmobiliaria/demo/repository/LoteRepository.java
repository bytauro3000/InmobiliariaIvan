package com.Inmobiliaria.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Inmobiliaria.demo.entity.Lote;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Integer>{

}
