package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.Voucher;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    List<Voucher> findByTipoOrigenAndReferenciaId(String tipoOrigen, Integer referenciaId);

    @Query("SELECT v FROM Voucher v WHERE v.tipoOrigen = :tipoOrigen AND v.referenciaId IN :referenciaIds")
    List<Voucher> findByTipoOrigenAndReferenciaIdIn(
            @Param("tipoOrigen") String tipoOrigen,
            @Param("referenciaIds") List<Integer> referenciaIds);
}