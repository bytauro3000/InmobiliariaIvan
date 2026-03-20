package com.Inmobiliaria.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.enums.EstadoContrato;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Integer> {

    @Query("SELECT cl.lote.programa.nombrePrograma, c.tipoContrato, COUNT(c) " +
               "FROM Contrato c JOIN c.lotes cl " +
               "GROUP BY cl.lote.programa.nombrePrograma, c.tipoContrato")
    List<Object[]> contarContratosPorProgramaYTipo();

    // ✅ QUERY 1 — Trae contratos con CLIENTES y vendedor
    // Hibernate no permite JOIN FETCH de dos List (clientes + lotes) en una sola query
    // → MultipleBagFetchException. Solución: dos queries separadas combinadas en memoria.
    @Query("SELECT DISTINCT c FROM Contrato c " +
           "LEFT JOIN FETCH c.clientes cc " +
           "LEFT JOIN FETCH cc.cliente " +
           "LEFT JOIN FETCH c.vendedor " +
           "ORDER BY c.idContrato DESC")
    List<Contrato> findAllConClientes();

    // ✅ QUERY 2 — Trae contratos con LOTES
    // Se ejecuta después de findAllConClientes() y se combina en el service por ID
    @Query("SELECT DISTINCT c FROM Contrato c " +
           "LEFT JOIN FETCH c.lotes cl " +
           "LEFT JOIN FETCH cl.lote l " +
           "LEFT JOIN FETCH l.programa " +
           "ORDER BY c.idContrato DESC")
    List<Contrato> findAllConLotes();

    // Se mantiene por compatibilidad con otros usos internos
    List<Contrato> findAllByOrderByIdContratoDesc();

    // Carga contratos FINANCIADOS con sus letras en una sola query
    @Query("SELECT DISTINCT c FROM Contrato c " +
           "LEFT JOIN FETCH c.letrasCambio " +
           "WHERE c.tipoContrato = com.Inmobiliaria.demo.enums.TipoContrato.FINANCIADO " +
           "AND c.estadoContrato IN (" +
           "  com.Inmobiliaria.demo.enums.EstadoContrato.ACTIVO, " +
           "  com.Inmobiliaria.demo.enums.EstadoContrato.MORA) " +
           "ORDER BY c.idContrato DESC")
    List<Contrato> findFinanciadosActivosConLetras();

    // USAR ESTE PARA GUARDAR (Nuevos registros)
    @Query("SELECT COUNT(cl) > 0 FROM ContratoLote cl " +
           "JOIN cl.contrato c " +
           "JOIN cl.lote l " +
           "JOIN l.programa p " +
           "WHERE p.idPrograma = :idPrograma " +
           "AND l.manzana = :manzana " +
           "AND l.numeroLote = :numeroLote " +
           "AND c.estadoContrato NOT IN :estadosExcluidos")
    boolean existeContratoDuplicado(
        @Param("idPrograma") Integer idPrograma,
        @Param("manzana") String manzana,
        @Param("numeroLote") String numeroLote,
        @Param("estadosExcluidos") java.util.List<EstadoContrato> estadosExcluidos
    );

    // USAR ESTE PARA ACTUALIZAR
    @Query("SELECT COUNT(cl) > 0 FROM ContratoLote cl " +
               "JOIN cl.contrato c " +
               "JOIN cl.lote l " +
               "JOIN l.programa p " +
               "WHERE p.idPrograma = :idPrograma " +
               "AND l.manzana = :manzana " +
               "AND l.numeroLote = :numeroLote " +
               "AND cl.contrato.idContrato <> :idContratoActual " +
               "AND c.estadoContrato NOT IN :estadosExcluidos")
    boolean existeContratoDuplicadoParaOtroContrato(
        @Param("idPrograma") Integer idPrograma,
        @Param("manzana") String manzana,
        @Param("numeroLote") String numeroLote,
        @Param("idContratoActual") Integer idContratoActual,
        @Param("estadosExcluidos") java.util.List<EstadoContrato> estadosExcluidos
    );

    List<Contrato> findByLotesLoteProgramaIdPrograma(Integer idPrograma);

    // CONSULTAR CONTRATO POR PROGRAMA + MZ + LT
    @Query("SELECT cl.contrato FROM ContratoLote cl " +
               "WHERE cl.lote.programa.idPrograma = :idPrograma " +
               "AND cl.lote.manzana = :manzana " +
               "AND cl.lote.numeroLote = :numeroLote " +
               "AND cl.contrato.estadoContrato NOT IN (" +
               "  com.Inmobiliaria.demo.enums.EstadoContrato.TRANSFERIDO, " +
               "  com.Inmobiliaria.demo.enums.EstadoContrato.RENUNCIA, " +
               "  com.Inmobiliaria.demo.enums.EstadoContrato.RESUELTO, " +
               "  com.Inmobiliaria.demo.enums.EstadoContrato.CANCELADO" +
               ")")
    Optional<Contrato> findByProgramaManzanaLote(
        @Param("idPrograma") Integer idPrograma,
        @Param("manzana") String manzana,
        @Param("numeroLote") String numeroLote
    );

    // Consulta de nombre + apellidos
    @Query("SELECT DISTINCT c FROM Contrato c " +
               "JOIN c.clientes cc " +
               "JOIN cc.cliente cl " +
               "WHERE LOWER(CONCAT(cl.nombre, ' ', cl.apellidos)) LIKE LOWER(CONCAT('%', :termino, '%')) " +
               "ORDER BY c.idContrato DESC")
    List<Contrato> findByClienteNombreContaining(@Param("termino") String termino);
}