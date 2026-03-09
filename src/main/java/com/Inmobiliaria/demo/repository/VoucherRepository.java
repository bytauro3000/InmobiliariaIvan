package com.Inmobiliaria.demo.repository;

import com.Inmobiliaria.demo.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    void deleteByPagoIdPago(Integer idPago);
}