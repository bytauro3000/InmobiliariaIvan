package com.Inmobiliaria.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Inmobiliaria.demo.entity.Parcelero;

@Repository
public interface ParceleroRepository extends JpaRepository<Parcelero, Integer> {

}
