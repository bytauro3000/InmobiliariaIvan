package com.Inmobiliaria.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.Inmobiliaria.demo.entity.LetraCambio;
import jakarta.transaction.Transactional;

@Repository
public interface LetraCambioRepository extends JpaRepository<LetraCambio, Integer> {

    List<LetraCambio> findByContratoIdContrato(Integer idContrato);

    @Transactional
    void deleteByContratoIdContrato(Integer idContrato);

    @Query(value = "SELECT " +
            "lc.numero_letra, " +
            "lc.fecha_giro, " +
            "lc.fecha_vencimiento, " +
            "lc.importe, " +
            "lc.importe_letras, " +
            "d.nombre AS distritoNombre, " +
            "MAX(CASE WHEN clientes.client_rank = 1 THEN clientes.nombre END) AS cliente1_nombre, " +
            "MAX(CASE WHEN clientes.client_rank = 1 THEN clientes.apellidos END) AS cliente1_apellidos, " +
            "MAX(CASE WHEN clientes.client_rank = 1 THEN clientes.numDocumento END) AS cliente1_numDocumento, " +
            "MAX(CASE WHEN clientes.client_rank = 2 THEN clientes.nombre END) AS cliente2_nombre, " +
            "MAX(CASE WHEN clientes.client_rank = 2 THEN clientes.apellidos END) AS cliente2_apellidos, " +
            "MAX(CASE WHEN clientes.client_rank = 2 THEN clientes.numDocumento END) AS cliente2_numDocumento, " +
            "MAX(CASE WHEN clientes.client_rank = 1 THEN clientes.direccion END) AS cliente1_direccion, " +   
            "MAX(CASE WHEN clientes.client_rank = 1 THEN clienteDistrito.nombre END) AS cliente1_distrito " +
            "FROM LetraCambio lc " +
            "JOIN Distrito d ON lc.id_distrito = d.id_distrito " +
            "JOIN Contrato c ON lc.id_contrato = c.id_contrato " +
            "JOIN (" +
            "    SELECT " +
            "        cc.id_contrato, " +
            "        cl.nombre, " +
            "        cl.apellidos, " +
            "        cl.numDocumento, " +
            "        cl.direccion, " +
            "        cl.id_distrito AS cliente_distrito_id, " +
            "        ROW_NUMBER() OVER (PARTITION BY cc.id_contrato ORDER BY cl.id_cliente) AS client_rank " +
            "    FROM ContratoCliente cc " +
            "    JOIN Cliente cl ON cc.id_cliente = cl.id_cliente " +
            ") AS clientes ON lc.id_contrato = clientes.id_contrato " +
           
            "JOIN Distrito clienteDistrito ON clienteDistrito.id_distrito = clientes.cliente_distrito_id " +
            "WHERE c.id_contrato = :idContrato " +
            "GROUP BY " +
            "lc.numero_letra, " +
            "lc.fecha_giro, " +
            "lc.fecha_vencimiento, " +
            "lc.importe, " +
            "lc.importe_letras, " +
            "d.nombre", nativeQuery = true)
    List<Object[]> obtenerReportePorContrato(@Param("idContrato") Integer idContrato);
}
