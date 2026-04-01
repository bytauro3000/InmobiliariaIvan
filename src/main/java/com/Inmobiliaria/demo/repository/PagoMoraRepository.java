package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.PagoMora;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagoMoraRepository extends JpaRepository<PagoMora, Integer> {

    // Todos los pagos de una mora específica
    List<PagoMora> findByMoraIdMora(Integer idMora);

    // Verificar si ya existe un comprobante duplicado
    boolean existsByTipoComprobanteAndNumeroComprobante(
            TipoComprobante tipoComprobante,
            String numeroComprobante);

    // Último pago de mora de un tipo de comprobante (para sugerencia de número compartida)
    Optional<PagoMora> findFirstByTipoComprobanteOrderByIdPagoMoraDesc(
            TipoComprobante tipoComprobante);
}