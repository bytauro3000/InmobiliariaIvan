package com.Inmobiliaria.demo.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.Inmobiliaria.demo.entity.Cliente;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

	 
	//Filtro específico para Nombres + Apellidos
    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.distrito WHERE " +
           "LOWER(CONCAT(c.nombre, ' ', c.apellidos)) LIKE LOWER(CONCAT('%', :filtro, '%'))")
    List<Cliente> buscarPorNombresYApellidos(@Param("filtro") String filtro);
    
    // Filtro por Documento: igualdad exacta (el campo está cifrado con IV
    // determinístico, por lo que LIKE ya no es posible; ver AesGcmConverter).
    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.distrito WHERE c.numDoc = :filtro")
    List<Cliente> buscarPorDocumento(@Param("filtro") String filtro);
    
    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.distrito WHERE c.numDoc = :numDocumento")
    Cliente findByNumDoc(@Param("numDocumento") String numDocumento);

    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.distrito WHERE c.idCliente = :id")
    Cliente findByIdWithDistrito(@Param("id") Integer id);
    
    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.distrito ORDER BY c.idCliente DESC")
    List<Cliente> findAllByOrderByIdClienteDesc();

    @Query(value = "SELECT c FROM Cliente c ORDER BY c.idCliente DESC",
           countQuery = "SELECT count(c) FROM Cliente c")
    Page<Cliente> findAllPaginado(Pageable pageable);

    /**
     * Migración FASE 2: escribe los campos YA CIFRADOS directamente en BD
     * (update nativo, bypass del converter). Idempotente por IV determinístico.
     */
    @Modifying
    @Query(value = "UPDATE cliente SET numDocumento = :doc, celular = :cel, telefono = :tel "
                 + "WHERE id_cliente = :id", nativeQuery = true)
    void actualizarCamposCifrados(@Param("id") Integer id,
                                  @Param("doc") String doc,
                                  @Param("cel") String cel,
                                  @Param("tel") String tel);
}