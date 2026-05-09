package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.SerieComprobante;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SerieComprobanteRepository extends JpaRepository<SerieComprobante, Long> { // ← Long, no Integer

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SerieComprobante s " +
           "WHERE s.tipoComprobante = :tipo AND s.serie = :serie")
    Optional<SerieComprobante> findByTipoComprobanteAndSerieForUpdate(
            @Param("tipo") TipoComprobante tipo,
            @Param("serie") String serie);

    @Query("SELECT s FROM SerieComprobante s " +
           "WHERE s.tipoComprobante = :tipo AND s.serie = :serie")
    Optional<SerieComprobante> findByTipoComprobanteAndSerie(
            @Param("tipo") TipoComprobante tipo,
            @Param("serie") String serie);
}