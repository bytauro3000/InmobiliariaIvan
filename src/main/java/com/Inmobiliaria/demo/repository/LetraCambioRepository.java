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
            // LetraCambio -> letra_cambio
            "FROM letra_cambio lc " + 
            "JOIN distrito d ON lc.id_distrito = d.id_distrito " +
            "JOIN contrato c ON lc.id_contrato = c.id_contrato " +
            "JOIN (" +
            "    SELECT " +
            "        cc.id_contrato, " +
            "        cl.nombre, " +
            "        cl.apellidos, " +
            "        cl.numDocumento, " +
            "        cl.direccion, " +
            "        cl.id_distrito AS cliente_distrito_id, " +
            "        ROW_NUMBER() OVER (PARTITION BY cc.id_contrato ORDER BY cl.id_cliente) AS client_rank " +
            // ContratoCliente -> contrato_cliente
            "    FROM contrato_cliente cc " + 
            "    JOIN cliente cl ON cc.id_cliente = cl.id_cliente " +
            ") AS clientes ON lc.id_contrato = clientes.id_contrato " +
            "JOIN distrito clienteDistrito ON clienteDistrito.id_distrito = clientes.cliente_distrito_id " +
            "WHERE c.id_contrato = :idContrato " +
            "GROUP BY " +
            // Corregido: Se debe agrupar por los campos seleccionados que no son agregados.
            "lc.numero_letra, " +
            "lc.fecha_giro, " +
            "lc.fecha_vencimiento, " +
            "lc.importe, " +
            "lc.importe_letras, " +
            "distritoNombre", nativeQuery = true) // <-- Usar el alias de columna
    List<Object[]> obtenerReportePorContrato(@Param("idContrato") Integer idContrato);

    
    // Nueva consulta para el Cronograma de Pagos con múltiples clientes y lotes
    @Query(value = "SELECT " +
        "lc.id_letra, " +
        "c.cantidad_letras, " +
        "c.monto_total, " +
        "c.inicial, " +
        "c.saldo, " +
        "v.nombre AS vendedor_nombre, " +
        "v.apellidos AS vendedor_apellidos, " +
        "lc.numero_letra, " +
        "lc.fecha_vencimiento, " +
        "lc.importe, " +
        "MAX(CASE WHEN clientes.client_rank = 1 THEN clientes.nombre END) AS cliente1_nombre, " +
        "MAX(CASE WHEN clientes.client_rank = 1 THEN clientes.apellidos END) AS cliente1_apellidos, " +
        "MAX(CASE WHEN clientes.client_rank = 1 THEN clientes.numDocumento END) AS cliente1_numDocumento, " +
        "MAX(CASE WHEN clientes.client_rank = 1 THEN clientes.celular END) AS cliente1_celular, " +
        "MAX(CASE WHEN clientes.client_rank = 1 THEN clientes.telefono END) AS cliente1_telefono, " +
        "MAX(CASE WHEN clientes.client_rank = 1 THEN clientes.direccion END) AS cliente1_direccion, " +
        "MAX(CASE WHEN clientes.client_rank = 1 THEN d_cliente.nombre END) AS cliente1_distrito, " +
        "MAX(CASE WHEN clientes.client_rank = 2 THEN clientes.nombre END) AS cliente2_nombre, " +
        "MAX(CASE WHEN clientes.client_rank = 2 THEN clientes.apellidos END) AS cliente2_apellidos, " +
        "MAX(CASE WHEN clientes.client_rank = 2 THEN clientes.numDocumento END) AS cliente2_numDocumento, " +
        "MAX(CASE WHEN lotes.lote_rank = 1 THEN lotes.manzana END) AS lote1_manzana, " +
        "MAX(CASE WHEN lotes.lote_rank = 1 THEN lotes.numero_lote END) AS lote1_numero_lote, " +
        "MAX(CASE WHEN lotes.lote_rank = 1 THEN lotes.area END) AS lote1_area, " +
        "MAX(CASE WHEN lotes.lote_rank = 2 THEN lotes.manzana END) AS lote2_manzana, " +
        "MAX(CASE WHEN lotes.lote_rank = 2 THEN lotes.numero_lote END) AS lote2_numero_lote, " +
        "MAX(CASE WHEN lotes.lote_rank = 2 THEN lotes.area END) AS lote2_area, " +
        "p.nombre_programa AS programa_nombre " +
        // LetraCambio -> letra_cambio
        "FROM letra_cambio lc " + 
        "JOIN contrato c ON lc.id_contrato = c.id_contrato " +
        "JOIN vendedor v ON c.id_vendedor = v.id_vendedor " +
        "JOIN (" +
        "    SELECT " +
        "        cc.id_contrato, " +
        "        cl.id_cliente, " +
        "        cl.nombre, " +
        "        cl.apellidos, " +
        "        cl.numDocumento, " +
        "        cl.celular, " +
        "        cl.telefono, " +
        "        cl.direccion, " +
        "        cl.id_distrito, " +
        "        ROW_NUMBER() OVER (PARTITION BY cc.id_contrato ORDER BY cl.id_cliente) AS client_rank " +
        // ContratoCliente -> contrato_cliente
        "    FROM contrato_cliente cc " + 
        "    JOIN cliente cl ON cc.id_cliente = cl.id_cliente " +
        ") AS clientes ON c.id_contrato = clientes.id_contrato " +
        "JOIN distrito d_cliente ON clientes.id_distrito = d_cliente.id_distrito " +
        "JOIN (" +
        "    SELECT " +
        // ContratoLote -> contrato_lote
        "        cl.id_contrato, " + 
        "        l.id_lote, " +
        "        l.manzana, " +
        "        l.numero_lote, " +
        "        l.area, " +
        "        l.id_programa, " +
        "        ROW_NUMBER() OVER (PARTITION BY cl.id_contrato ORDER BY l.id_lote) AS lote_rank " +
        "    FROM contrato_lote cl " +
        "    JOIN lote l ON cl.id_lote = l.id_lote " +
        ") AS lotes ON c.id_contrato = lotes.id_contrato " +
        "JOIN programa p ON lotes.id_programa = p.id_programa " +
        "WHERE c.id_contrato = :idContrato " +
        "GROUP BY " +
        "lc.id_letra, " +
        "c.cantidad_letras, " +
        "c.monto_total, " +
        "c.inicial, " +
        "c.saldo, " +
        "v.nombre, " +
        "v.apellidos, " +
        "lc.numero_letra, " +
        "lc.fecha_vencimiento, " +
        "lc.importe, " +
        "p.nombre_programa", nativeQuery = true)
    List<Object[]> obtenerCronogramaPagosPorContrato(@Param("idContrato") Integer idContrato);
}