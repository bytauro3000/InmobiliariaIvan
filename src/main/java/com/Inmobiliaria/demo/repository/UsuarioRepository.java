package com.Inmobiliaria.demo.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.Inmobiliaria.demo.entity.Usuario;


@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    // Buscar usuario por nombre de usuario (único)
	Optional<Usuario> findByCorreo(String correo);
}