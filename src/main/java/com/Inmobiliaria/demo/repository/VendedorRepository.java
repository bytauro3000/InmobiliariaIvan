package com.Inmobiliaria.demo.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.Inmobiliaria.demo.entity.Vendedor;

@Repository
public interface VendedorRepository extends JpaRepository<Vendedor, Integer> {

    // La relación en Vendedor se llama "usuario", por eso se navega v.usuario.id.
    // El nombre del método no se puede derivar automáticamente (findByIdUsuario buscaría
    // una propiedad literal "idUsuario"); se declara la JPQL explícita.
    @Query("SELECT v FROM Vendedor v WHERE v.usuario.id = :idUsuario")
    Optional<Vendedor> findByIdUsuario(@Param("idUsuario") Integer idUsuario);
}

