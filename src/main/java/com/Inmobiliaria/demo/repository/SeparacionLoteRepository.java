package com.Inmobiliaria.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Inmobiliaria.demo.entity.SeparacionLote;
import com.Inmobiliaria.demo.entity.SeparacionLoteId;

@Repository
public interface SeparacionLoteRepository extends JpaRepository<SeparacionLote, SeparacionLoteId>{

}
