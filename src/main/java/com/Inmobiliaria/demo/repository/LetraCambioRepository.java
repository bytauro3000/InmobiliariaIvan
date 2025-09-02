package com.Inmobiliaria.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Inmobiliaria.demo.entity.LetraCambio;

import jakarta.transaction.Transactional;

@Repository 
public interface LetraCambioRepository extends JpaRepository<LetraCambio, Integer> {
	
	 List<LetraCambio> findByContratoIdContrato(Integer idContrato);
	 
	 @Transactional
     void deleteByContratoIdContrato(Integer idContrato);
}
