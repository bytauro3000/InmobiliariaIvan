package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.Voucher;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    List<Voucher> findByTipoOrigenAndReferenciaId(String tipoOrigen, Integer referenciaId);
}