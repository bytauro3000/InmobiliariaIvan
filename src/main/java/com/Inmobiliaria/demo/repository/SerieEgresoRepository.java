package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.SerieEgreso;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SerieEgresoRepository extends JpaRepository<SerieEgreso, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SerieEgreso s WHERE s.serie = :serie")
    Optional<SerieEgreso> findBySerieForUpdate(@Param("serie") String serie);

    Optional<SerieEgreso> findBySerie(String serie);
}