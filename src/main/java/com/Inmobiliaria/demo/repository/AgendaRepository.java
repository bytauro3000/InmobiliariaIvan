package com.Inmobiliaria.demo.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.Inmobiliaria.demo.entity.Agenda;
import com.Inmobiliaria.demo.enums.EstadoAgenda;

@Repository
public interface AgendaRepository extends JpaRepository<Agenda, Integer> {

    List<Agenda> findByFechaOrderByHoraAsc(LocalDate fecha);

    List<Agenda> findByFechaBetweenOrderByFechaAscHoraAsc(LocalDate inicio, LocalDate fin);

    @Query("SELECT a FROM Agenda a WHERE a.fecha >= :fecha AND a.estado = :estado ORDER BY a.fecha ASC, a.hora ASC")
    List<Agenda> findPendientesDesdeFecha(@Param("fecha") LocalDate fecha, @Param("estado") EstadoAgenda estado);

    @Query("SELECT a FROM Agenda a WHERE a.fecha = :fecha AND a.recordatorioEnviado = false AND a.estado = 'PENDIENTE'")
    List<Agenda> findPendientesRecordatorio(@Param("fecha") LocalDate fecha);

    List<Agenda> findByEstadoOrderByFechaDescHoraDesc(EstadoAgenda estado);
}