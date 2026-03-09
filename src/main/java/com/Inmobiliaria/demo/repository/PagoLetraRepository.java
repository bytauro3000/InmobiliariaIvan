package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.PagoLetras;
import com.Inmobiliaria.demo.enums.TipoComprobante;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoLetraRepository extends JpaRepository<PagoLetras, Integer> {

    // Buscar pagos por el ID de la letra
    List<PagoLetras> findByLetraIdLetra(Integer idLetra);

    // Buscar pagos por el ID del contrato (a través de la letra)
    List<PagoLetras> findByLetraContratoIdContrato(Integer idContrato);
    
    long countByLetraIdLetra(Integer idLetra);
    
    Optional<PagoLetras> findFirstByTipoComprobanteOrderByFechaOperacionDesc(TipoComprobante tipoComprobante);
    
    // Validación de unicidad
    boolean existsByTipoComprobanteAndNumeroComprobante(TipoComprobante tipoComprobante, String numeroComprobante);

    // Para actualización: excluir el propio pago
    boolean existsByTipoComprobanteAndNumeroComprobanteAndIdPagoNot(TipoComprobante tipoComprobante, String numeroComprobante, Integer idPago);
}