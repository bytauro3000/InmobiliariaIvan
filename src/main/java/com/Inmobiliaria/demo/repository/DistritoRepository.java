package com.Inmobiliaria.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Inmobiliaria.demo.entity.Distrito;

@Repository
public interface DistritoRepository  extends JpaRepository<Distrito , Integer>{

}
