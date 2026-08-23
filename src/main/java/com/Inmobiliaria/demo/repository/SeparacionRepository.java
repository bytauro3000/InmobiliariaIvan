package com.Inmobiliaria.demo.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.Inmobiliaria.demo.dto.SeparacionDTO;
import com.Inmobiliaria.demo.entity.Separacion;

@Repository
public interface SeparacionRepository extends JpaRepository<Separacion, Integer> {

    // 🟢 CORRECCIÓN: Se añaden "clientes.cliente.distrito" y se unifica findById
    @EntityGraph(attributePaths = {
        "clientes", 
        "clientes.cliente", 
        "clientes.cliente.distrito", // 👈 Crucial para evitar el error "no session" en Distrito
        "lotes", 
        "lotes.lote", 
        "lotes.lote.programa",
        "vendedor", 
        "vendedor.distrito"
    })
    @Override
    Optional<Separacion> findById(Integer id);

    // EntityGraph optimizado para traer la información base en listados
    @EntityGraph(attributePaths = {
        "clientes.cliente", 
        "lotes.lote", 
        "vendedor"
    })
    @Override
    List<Separacion> findAll();

    // 🟢 MEJORA: Se añade DISTINCT para que no se repitan IDs en el buscador de Angular
    @Query("SELECT DISTINCT new com.Inmobiliaria.demo.dto.SeparacionDTO(s.idSeparacion, " +
           "CONCAT(c.nombre, ' ', c.apellidos, ' - DNI: ', c.numDoc, ' - Mz ', l.manzana, ' Lt ', l.numeroLote)) " +
           "FROM Separacion s " +
           "JOIN s.clientes sc " + 
           "JOIN sc.cliente c " +
           "JOIN s.lotes sl " + 
           "JOIN sl.lote l " +
           "WHERE (LOWER(COALESCE(c.nombre, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
           "OR LOWER(COALESCE(c.apellidos, '')) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
           "OR LOWER(CONCAT(COALESCE(c.nombre, ''), ' ', COALESCE(c.apellidos, ''))) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
           "OR LOWER(CONCAT(COALESCE(c.apellidos, ''), ' ', COALESCE(c.nombre, ''))) LIKE LOWER(CONCAT('%', :filtro, '%')) " +
           "OR c.numDoc LIKE CONCAT('%', :filtro, '%') " +
           "OR CAST(s.idSeparacion AS string) LIKE CONCAT('%', :filtro, '%'))")
    List<SeparacionDTO> buscarPorDniOApellido(@Param("filtro") String filtro);
}