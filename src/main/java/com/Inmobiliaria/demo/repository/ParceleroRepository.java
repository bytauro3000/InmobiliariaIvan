package com.Inmobiliaria.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Inmobiliaria.demo.entity.Parcelero;

import java.util.Optional;

@Repository
public interface ParceleroRepository extends JpaRepository<Parcelero, Integer> {

    Optional<Parcelero> findByDni(String dni);

    boolean existsByDni(String dni);

    boolean existsByEmail(String email);

    boolean existsByDniAndIdParceleroNot(String dni, Integer idParcelero);

    boolean existsByEmailAndIdParceleroNot(String email, Integer idParcelero);
}
