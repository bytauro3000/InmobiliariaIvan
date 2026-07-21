package com.Inmobiliaria.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.Inmobiliaria.demo.entity.Distrito;

@Repository
public interface DistritoRepository  extends JpaRepository<Distrito , Integer>{

    @Query("SELECT DISTINCT d.departamento FROM Distrito d ORDER BY d.departamento")
    List<String> findDepartamentos();

    @Query("SELECT DISTINCT d.provincia FROM Distrito d WHERE d.departamento = :departamento ORDER BY d.provincia")
    List<String> findProvinciasByDepartamento(String departamento);

    List<Distrito> findByDepartamentoAndProvinciaOrderByNombreAsc(String departamento, String provincia);
}
