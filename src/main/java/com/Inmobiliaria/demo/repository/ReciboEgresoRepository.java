package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.ReciboEgreso;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReciboEgresoRepository extends JpaRepository<ReciboEgreso, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReciboEgreso r WHERE r.serie = :serie ORDER BY r.numero DESC")
    List<ReciboEgreso> findTopBySerieForUpdate(@Param("serie") String serie);

    Optional<ReciboEgreso> findByNumeroCompleto(String numeroCompleto);

    @Query("SELECT MAX(r.numero) FROM ReciboEgreso r WHERE r.serie = :serie")
    Integer findMaxNumeroBySerie(@Param("serie") String serie);

    List<ReciboEgreso> findByFechaEmisionBetweenOrderByNumeroAsc(LocalDate desde, LocalDate hasta);

    /**
     * Egresos por rango usando fecha de PAGO para comisiones y fecha de EMISIÓN para otros.
     * Retorna Object[]: [numeroCompleto, serie, numero, fechaDocumento, concepto,
     *   beneficiario, idContrato, monto, moneda, medioPago, numeroOperacion,
     *   fechaOperacion, usuarioRegistro]
     */
    @Query(value = "SELECT r.numero_completo, r.serie, r.numero, " +
            "COALESCE(pcv.fecha_pago, r.fecha_emision) AS fecha_doc, " +
            "r.concepto, r.beneficiario, r.id_contrato, r.monto, r.moneda, " +
            "r.medio_pago, r.numero_operacion, r.fecha_operacion, r.usuario_registro " +
            "FROM recibo_egreso r " +
            "LEFT JOIN pago_comision_vendedor pcv ON r.numero_completo = pcv.numero_egreso " +
            "WHERE COALESCE(pcv.fecha_pago, r.fecha_emision) BETWEEN :desde AND :hasta " +
            "ORDER BY fecha_doc ASC, r.numero ASC",
            nativeQuery = true)
    List<Object[]> findEgresosPorFechaPagoOrEmision(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}