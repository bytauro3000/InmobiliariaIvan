package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.SesionActiva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SesionActivaRepository extends JpaRepository<SesionActiva, Long> {

    List<SesionActiva> findByUsuarioIdAndActivaTrue(Integer usuarioId);

    @Query("SELECT s FROM SesionActiva s JOIN FETCH s.usuario u WHERE s.activa = true ORDER BY s.ultimoRefresh DESC")
    List<SesionActiva> findActivasConUsuario();

    @Query("SELECT COUNT(s) FROM SesionActiva s WHERE s.fechaLogueo BETWEEN :inicio AND :fin")
    long countVisitasHoy(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
