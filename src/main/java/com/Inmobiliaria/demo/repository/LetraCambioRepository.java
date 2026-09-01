package com.Inmobiliaria.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.Inmobiliaria.demo.entity.LetraCambio;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

@Repository
public interface LetraCambioRepository extends JpaRepository<LetraCambio, Integer> {

    List<LetraCambio> findByContratoIdContrato(Integer idContrato);

    @Modifying
    @Transactional
    @Query("DELETE FROM LetraCambio l WHERE l.contrato.idContrato = :idContrato")
    void deleteByContratoIdContrato(@Param("idContrato") Integer idContrato);

    Optional<LetraCambio> findFirstByContratoIdContratoOrderByNumeroLetraAsc(Integer idContrato);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM LetraCambio l WHERE l.idLetra = :idLetra")
    Optional<LetraCambio> findByIdWithLock(@Param("idLetra") Integer idLetra);

    // ✅ Query 1: trae letras + contrato + clientes (solo UNA colección en JOIN FETCH)
    // Hibernate lanza MultipleBagFetchException si se hace JOIN FETCH de dos List a la vez.
    // Por eso separamos: esta query carga letras con sus clientes del contrato.
    @Query("SELECT DISTINCT l FROM LetraCambio l " +
           "LEFT JOIN FETCH l.contrato c " +
           "LEFT JOIN FETCH c.clientes cc " +
           "LEFT JOIN FETCH cc.cliente " +
           "WHERE c.idContrato = :idContrato " +
           "ORDER BY l.idLetra ASC")
    List<LetraCambio> findByContratoIdContratoConClientes(@Param("idContrato") Integer idContrato);
    
    @Query("SELECT l.contrato.idContrato FROM LetraCambio l " +
    	       "WHERE l.contrato.idContrato IN :ids " +
    	       "GROUP BY l.contrato.idContrato")
    	List<Integer> findContratosConLetras(@Param("ids") List<Integer> ids);

    // ✅ Query 2: trae letras + pagos + comprobante de cada pago (segunda colección separada)
    // Se agrega LEFT JOIN FETCH p.comprobante para evitar LazyInitializationException
    // al acceder a p.getComprobante().getNumeroCompleto() en el service (detección esMultiple).
    @Query("SELECT DISTINCT l FROM LetraCambio l " +
           "LEFT JOIN FETCH l.pagos p " +
           "LEFT JOIN FETCH p.comprobante " +
           "WHERE l.contrato.idContrato = :idContrato " +
           "ORDER BY l.idLetra ASC")
    List<LetraCambio> findByContratoIdContratoConPagos(@Param("idContrato") Integer idContrato);

    

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
            "    FROM contrato_cliente cc " +
            "    JOIN cliente cl ON cc.id_cliente = cl.id_cliente " +
            ") AS clientes ON lc.id_contrato = clientes.id_contrato " +
            "JOIN distrito clienteDistrito ON clienteDistrito.id_distrito = clientes.cliente_distrito_id " +
            "WHERE c.id_contrato = :idContrato " +
            "GROUP BY " +
            "lc.numero_letra, " +
            "lc.fecha_giro, " +
            "lc.fecha_vencimiento, " +
            "lc.importe, " +
            "lc.importe_letras, " +
            "distritoNombre", nativeQuery = true)
    List<Object[]> obtenerReportePorContrato(@Param("idContrato") Integer idContrato);

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
        "MAX(CASE WHEN clientes.es_aval = 1 THEN clientes.nombre END) AS aval1_nombre, " +
        "MAX(CASE WHEN clientes.es_aval = 1 THEN clientes.apellidos END) AS aval1_apellidos, " +
        "MAX(CASE WHEN clientes.es_aval = 1 THEN clientes.numDocumento END) AS aval1_numDocumento, " +
        "MAX(CASE WHEN lotes.lote_rank = 1 THEN lotes.manzana END) AS lote1_manzana, " +
        "MAX(CASE WHEN lotes.lote_rank = 1 THEN lotes.numero_lote END) AS lote1_numero_lote, " +
        "MAX(CASE WHEN lotes.lote_rank = 1 THEN lotes.area END) AS lote1_area, " +
        "MAX(CASE WHEN lotes.lote_rank = 2 THEN lotes.manzana END) AS lote2_manzana, " +
        "MAX(CASE WHEN lotes.lote_rank = 2 THEN lotes.numero_lote END) AS lote2_numero_lote, " +
        "MAX(CASE WHEN lotes.lote_rank = 2 THEN lotes.area END) AS lote2_area, " +
        "p.nombre_programa AS programa_nombre " +
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
        "        CASE WHEN cc.tipo_propietario = 'AVAL' THEN 1 ELSE 0 END AS es_aval, " +
        "        ROW_NUMBER() OVER (" +
        "            PARTITION BY cc.id_contrato " +
        "            ORDER BY CASE WHEN cc.tipo_propietario = 'AVAL' THEN 1 ELSE 0 END, cl.id_cliente" +
        "        ) AS client_rank " +
        "    FROM contrato_cliente cc " +
        "    JOIN cliente cl ON cc.id_cliente = cl.id_cliente " +
        ") AS clientes ON c.id_contrato = clientes.id_contrato " +
        "JOIN distrito d_cliente ON clientes.id_distrito = d_cliente.id_distrito " +
        "JOIN (" +
        "    SELECT " +
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
        "p.nombre_programa " +
        "ORDER BY lc.id_letra ASC",
        nativeQuery = true)
    List<Object[]> obtenerCronogramaPagosPorContrato(@Param("idContrato") Integer idContrato);
}