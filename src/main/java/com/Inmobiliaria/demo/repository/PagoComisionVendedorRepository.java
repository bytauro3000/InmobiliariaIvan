package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.PagoComisionVendedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagoComisionVendedorRepository extends JpaRepository<PagoComisionVendedor, Integer> {

    List<PagoComisionVendedor> findByComisionIdComisionOrderByIdPagoComisionAsc(Integer idComision);

    boolean existsByComisionIdComisionAndTipo(Integer idComision, String tipo);

    boolean existsByLetraIdLetraAndTipo(Integer idLetra, String tipo);
}