package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.PagoComisionVendedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagoComisionVendedorRepository extends JpaRepository<PagoComisionVendedor, Integer> {

    List<PagoComisionVendedor> findByComisionIdComisionOrderByIdPagoComisionAsc(Integer idComision);

    boolean existsByComisionIdComisionAndTipo(Integer idComision, String tipo);

    boolean existsByLetraIdLetraAndTipo(Integer idLetra, String tipo);

    List<PagoComisionVendedor> findByNumeroEgreso(String numeroEgreso);

    /** Conteo de pagos de tipo dado por comisión (para el listado batch). */
    @Query("SELECT p.comision.idComision, COUNT(p) FROM PagoComisionVendedor p " +
           "WHERE p.comision.idComision IN :ids AND p.tipo = :tipo GROUP BY p.comision.idComision")
    List<Object[]> countByComisionesAndTipo(
            @Param("ids") List<Integer> ids, @Param("tipo") String tipo);
}