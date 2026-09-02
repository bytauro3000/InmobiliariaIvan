package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.ReciboEgreso;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReciboEgresoRepository extends JpaRepository<ReciboEgreso, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReciboEgreso r WHERE r.serie = :serie ORDER BY r.numero DESC")
    List<ReciboEgreso> findTopBySerieForUpdate(@Param("serie") String serie);

    Optional<ReciboEgreso> findByNumeroCompleto(String numeroCompleto);
}