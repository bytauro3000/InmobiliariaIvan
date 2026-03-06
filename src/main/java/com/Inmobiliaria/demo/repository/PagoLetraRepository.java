package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.PagoLetras;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PagoLetraRepository extends JpaRepository<PagoLetras, Integer> {

    // Buscar pagos por el ID de la letra
    List<PagoLetras> findByLetraIdLetra(Integer idLetra);

    // Buscar pagos por el ID del contrato (a través de la letra)
    List<PagoLetras> findByLetraContratoIdContrato(Integer idContrato);
}