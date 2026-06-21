package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.RefreshToken;
import com.Inmobiliaria.demo.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("delete from RefreshToken r where r.usuario = :usuario")
    void deleteByUsuario(@Param("usuario") Usuario usuario);
}
