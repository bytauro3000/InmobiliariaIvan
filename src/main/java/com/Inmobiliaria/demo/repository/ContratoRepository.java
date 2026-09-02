package com.Inmobiliaria.demo.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.enums.EstadoContrato;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Integer> {

    /**
     * Contratos elegibles para migración de comisiones: FINANCIADO o CONTADO,
     * con vendedor cuyo % de comisión &gt; 0, y que aún no tengan comisión creada.
     */
    @Query("SELECT c FROM Contrato c " +
           "LEFT JOIN FETCH c.vendedor " +
           "WHERE c.vendedor IS NOT NULL " +
           "  AND c.vendedor.comision > 0 " +
           "  AND (c.tipoContrato = com.Inmobiliaria.demo.enums.TipoContrato.FINANCIADO " +
           "    OR c.tipoContrato = com.Inmobiliaria.demo.enums.TipoContrato.CONTADO) " +
           "  AND NOT EXISTS (SELECT 1 FROM ComisionVendedor cv WHERE cv.contrato.idContrato = c.idContrato) " +
           "ORDER BY c.idContrato ASC")
    java.util.List<Contrato> findContratosElegiblesParaComision();

    @Query("SELECT cl.lote.programa.nombrePrograma, c.tipoContrato, COUNT(c) " +
               "FROM Contrato c JOIN c.lotes cl " +
               "GROUP BY cl.lote.programa.nombrePrograma, c.tipoContrato")
    List<Object[]> contarContratosPorProgramaYTipo();

    // ✅ QUERY 1/2 — Contratos con CLIENTES + vendedor + comprobante inicial (SIN lotes)
    // Separadas de lotes para evitar el producto cartesiano clientes × lotes.
    @Query("SELECT DISTINCT c FROM Contrato c " +
           "LEFT JOIN FETCH c.clientes cc " +
           "LEFT JOIN FETCH cc.cliente " +
           "LEFT JOIN FETCH cc.cliente.distrito " +
           "LEFT JOIN FETCH c.vendedor " +
           "LEFT JOIN FETCH c.comprobanteInicial " +
           "LEFT JOIN FETCH c.pagoInicial " +
           "ORDER BY c.idContrato DESC")
    List<Contrato> findAllConClientes();

    // ✅ QUERY 2/2 — Contratos con LOTES + programa (SIN clientes)
    @Query("SELECT DISTINCT c FROM Contrato c " +
           "LEFT JOIN FETCH c.lotes cl " +
           "LEFT JOIN FETCH cl.lote l " +
           "LEFT JOIN FETCH l.programa " +
           "LEFT JOIN FETCH c.vendedor " +
           "LEFT JOIN FETCH c.comprobanteInicial " +
           "LEFT JOIN FETCH c.pagoInicial " +
           "ORDER BY c.idContrato DESC")
    List<Contrato> findAllConLotes();

    @Query("SELECT DISTINCT c FROM Contrato c " +
           "LEFT JOIN FETCH c.clientes cc " +
           "LEFT JOIN FETCH cc.cliente " +
           "LEFT JOIN FETCH cc.cliente.distrito " +
           "LEFT JOIN FETCH c.lotes cl " +
           "LEFT JOIN FETCH cl.lote l " +
           "LEFT JOIN FETCH l.programa " +
           "LEFT JOIN FETCH c.vendedor " +
           "LEFT JOIN FETCH c.comprobanteInicial " +
           "LEFT JOIN FETCH c.pagoInicial " +
           "WHERE c.idContrato = :id")
    Contrato findByIdConTodo(@Param("id") Integer id);

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

    @Query("SELECT c.idContrato FROM Contrato c " +
           "WHERE c.tipoContrato = com.Inmobiliaria.demo.enums.TipoContrato.FINANCIADO " +
           "AND c.estadoContrato IN (" +
           "  com.Inmobiliaria.demo.enums.EstadoContrato.ACTIVO, " +
           "  com.Inmobiliaria.demo.enums.EstadoContrato.MORA)")
    Page<Integer> findFinanciadosActivosId(Pageable pageable);

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
           "LEFT JOIN FETCH c.clientes cc " +
           "LEFT JOIN FETCH cc.cliente " +
           "LEFT JOIN FETCH cc.cliente.distrito " +
           "LEFT JOIN FETCH c.lotes cl " +
           "LEFT JOIN FETCH cl.lote l " +
           "LEFT JOIN FETCH l.programa " +
           "LEFT JOIN FETCH c.vendedor " +
           "LEFT JOIN FETCH c.comprobanteInicial " +
           "LEFT JOIN FETCH c.pagoInicial " +
           "JOIN c.clientes cc2 " +
           "JOIN cc2.cliente cl2 " +
           "WHERE LOWER(CONCAT(cl2.nombre, ' ', cl2.apellidos)) LIKE LOWER(CONCAT('%', :termino, '%')) " +
           "ORDER BY c.idContrato DESC")
    List<Contrato> findByClienteNombreContaining(@Param("termino") String termino);

    // ── CORRECCIÓN: Pantalla de inscripciones ────────────────────────────────
    // Se separa en DOS queries para evitar MultipleBagFetchException y el error
    // de Hibernate que prohíbe ORDER BY con DISTINCT + múltiples JOIN FETCH de colecciones.

    // QUERY A — Carga clientes (sin ORDER BY en el FETCH de colección)
    @Query("SELECT DISTINCT c FROM Contrato c " +
           "LEFT JOIN FETCH c.clientes cc " +
           "LEFT JOIN FETCH cc.cliente")
    List<Contrato> findResumenInscripcionesConClientes();

    // QUERY B — Carga lotes sobre los mismos contratos (sin ORDER BY)
    @Query("SELECT DISTINCT c FROM Contrato c " +
           "LEFT JOIN FETCH c.lotes cl " +
           "LEFT JOIN FETCH cl.lote")
    List<Contrato> findResumenInscripcionesConLotes();

    // QUERY batch: trae varios contratos con sus clientes y los clientes de cada uno
    // en una sola consulta (evita N+1 al resolver nombre de cliente en inscripciones)
    @Query("SELECT DISTINCT c FROM Contrato c " +
           "LEFT JOIN FETCH c.clientes cc " +
           "LEFT JOIN FETCH cc.cliente " +
           "WHERE c.idContrato IN :ids")
    List<Contrato> findAllByIdConClientes(@Param("ids") Collection<Integer> ids);

    @Query("SELECT DISTINCT c FROM Contrato c " +
           "LEFT JOIN FETCH c.clientes cc " +
           "LEFT JOIN FETCH cc.cliente " +
           "ORDER BY c.idContrato DESC")
    List<Contrato> findAllResumenConClientes();

    @Query("SELECT DISTINCT c FROM Contrato c " +
           "LEFT JOIN FETCH c.lotes cl " +
           "LEFT JOIN FETCH cl.lote l " +
           "LEFT JOIN FETCH l.programa " +
           "ORDER BY c.idContrato DESC")
    List<Contrato> findAllResumenConLotes();

    @Query("SELECT c FROM Contrato c ORDER BY c.idContrato DESC")
    Page<Contrato> findAllResumenPaginado(Pageable pageable);

    @Query("SELECT cl.contrato.montoTotal FROM ContratoLote cl " +
           "WHERE cl.lote.idLote = :idLote " +
            "AND cl.contrato.estadoContrato NOT IN (" +
            "  com.Inmobiliaria.demo.enums.EstadoContrato.RENUNCIA, " +
            "  com.Inmobiliaria.demo.enums.EstadoContrato.TRANSFERIDO" +
            ") " +
            "ORDER BY cl.contrato.idContrato DESC")
    java.util.Optional<java.math.BigDecimal> findPrecioVentaByLoteId(@Param("idLote") Integer idLote);

    @Query("SELECT cl.contrato FROM ContratoLote cl " +
           "JOIN FETCH cl.contrato.clientes cc " +
           "JOIN FETCH cc.cliente " +
           "JOIN FETCH cl.contrato.lotes cl2 " +
           "JOIN FETCH cl2.lote l2 " +
           "JOIN FETCH l2.programa " +
           "WHERE cl.lote.idLote = :idLote " +
            "AND cl.contrato.estadoContrato NOT IN (" +
            "  com.Inmobiliaria.demo.enums.EstadoContrato.RENUNCIA, " +
            "  com.Inmobiliaria.demo.enums.EstadoContrato.TRANSFERIDO" +
            ") " +
            "ORDER BY cl.contrato.idContrato DESC")
    java.util.Optional<Contrato> findContratoByLoteId(@Param("idLote") Integer idLote);

    /**
     * Contratos en estado "vendido" con sus CLIENTES (sin lotes).
     * Separado de findLotesVendidosConLotes para evitar el producto cartesiano
     * clientes × lotes (causa de OutOfMemory en Render).
     */
    @Query("SELECT DISTINCT c FROM Contrato c " +
           "LEFT JOIN FETCH c.clientes cc " +
           "LEFT JOIN FETCH cc.cliente " +
           "LEFT JOIN FETCH c.vendedor " +
           "WHERE c.estadoContrato IN (" +
           "  com.Inmobiliaria.demo.enums.EstadoContrato.ACTIVO, " +
           "  com.Inmobiliaria.demo.enums.EstadoContrato.MORA, " +
           "  com.Inmobiliaria.demo.enums.EstadoContrato.CANCELADO, " +
           "  com.Inmobiliaria.demo.enums.EstadoContrato.TRANSFERIDO" +
           ") " +
           "AND (:idVendedor IS NULL OR c.vendedor.idVendedor = :idVendedor)")
    List<Contrato> findLotesVendidosConClientes(@Param("idVendedor") Integer idVendedor);

    /**
     * Contratos en estado "vendido" con sus LOTES y programa (sin clientes).
     */
    @Query("SELECT DISTINCT c FROM Contrato c " +
           "LEFT JOIN FETCH c.lotes cl " +
           "LEFT JOIN FETCH cl.lote l " +
           "LEFT JOIN FETCH l.programa " +
           "LEFT JOIN FETCH c.vendedor " +
           "WHERE c.estadoContrato IN (" +
           "  com.Inmobiliaria.demo.enums.EstadoContrato.ACTIVO, " +
           "  com.Inmobiliaria.demo.enums.EstadoContrato.MORA, " +
           "  com.Inmobiliaria.demo.enums.EstadoContrato.CANCELADO, " +
           "  com.Inmobiliaria.demo.enums.EstadoContrato.TRANSFERIDO" +
           ") " +
           "AND (:idVendedor IS NULL OR c.vendedor.idVendedor = :idVendedor)")
    List<Contrato> findLotesVendidosConLotes(@Param("idVendedor") Integer idVendedor);
}