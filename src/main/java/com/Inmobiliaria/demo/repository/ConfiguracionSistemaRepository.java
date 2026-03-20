package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.ConfiguracionSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracionSistemaRepository extends JpaRepository<ConfiguracionSistema, String> {
}