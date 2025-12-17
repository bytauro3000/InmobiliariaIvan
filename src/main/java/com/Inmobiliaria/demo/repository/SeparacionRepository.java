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

    @EntityGraph(attributePaths = {
        "clientes", 
        "clientes.cliente", 
        "lotes", 
        "lotes.lote", 
        "lotes.lote.programa",
        "vendedor", 
        "vendedor.distrito"
    })
    @Override
    Optional<Separacion> findById(Integer id);

    // Usamos este EntityGraph para traer toda la lista sin errores de carga diferida
    @EntityGraph(attributePaths = {
        "clientes.cliente", 
        "lotes.lote", 
        "vendedor"
    })
    @Override
    List<Separacion> findAll();

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
}