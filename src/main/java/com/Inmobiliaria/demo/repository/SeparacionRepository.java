package com.Inmobiliaria.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.Inmobiliaria.demo.dto.SeparacionDTO;
import com.Inmobiliaria.demo.dto.SeparacionResumenDTO;
import com.Inmobiliaria.demo.entity.Separacion;

@Repository
public interface SeparacionRepository extends JpaRepository<Separacion, Integer> {
    
    /**
     * Se han añadido "vendedor.distrito" y "lotes.lote.programa" para evitar
     * errores de carga diferida (LazyInitializationException) al serializar a JSON.
     */
    @EntityGraph(attributePaths = {
        "clientes", 
        "clientes.cliente", 
        "lotes", 
        "lotes.lote", 
        "lotes.lote.programa", // Carga el programa asociado al lote
        "vendedor", 
        "vendedor.distrito"    // Carga el distrito del vendedor (evita el error ByteBuddy)
    })
    @Override
    Optional<Separacion> findById(Integer id);

    @Query("SELECT new com.Inmobiliaria.demo.dto.SeparacionDTO(s.idSeparacion, " +
           "CONCAT(c.nombre, ' ', c.apellidos, ' - DNI: ', c.numDoc, ' - Mz ', l.manzana, ' Lt ', l.numeroLote)) " +
           "FROM Separacion s " +
           "JOIN s.clientes sc " + 
           "JOIN sc.cliente c " +
           "JOIN s.lotes sl " + 
           "JOIN sl.lote l " +
           "WHERE LOWER(c.apellidos) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
           "OR c.numDoc LIKE CONCAT('%', :filtro, '%')")
    List<SeparacionDTO> buscarPorDniOApellido(@Param("filtro") String filtro);

    @Query("""
        SELECT new com.Inmobiliaria.demo.dto.SeparacionResumenDTO(
            s.idSeparacion,
            c.numDoc,
            CONCAT(COALESCE(c.nombre, ''), ' ', COALESCE(c.apellidos, '')),
            CONCAT(COALESCE(l.manzana, ''), '-', COALESCE(l.numeroLote, '')),
            s.monto,
            s.fechaSeparacion,
            s.fechaLimite,
            s.estado
        )
        FROM Separacion s
        JOIN s.clientes sc
        JOIN sc.cliente c
        JOIN s.lotes sl
        JOIN sl.lote l
    """)
    List<SeparacionResumenDTO> obtenerResumenSeparaciones();
}