package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.dto.CuentasPorCobrarDTO;
import com.Inmobiliaria.demo.dto.CuentasPorCobrarDTO.FilaCuenta;
import com.Inmobiliaria.demo.dto.CuentasPorCobrarDTO.GrupoPrograma;
import com.Inmobiliaria.demo.entity.*;
import com.Inmobiliaria.demo.enums.EstadoContrato;
import com.Inmobiliaria.demo.enums.EstadoLetra;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import com.Inmobiliaria.demo.service.CuentasPorCobrarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Cuentas por cobrar: ingresos esperados por letras pendientes de pago.
 *
 * Regla de negocio (alineada con la cobranza):
 *  - Solo contratos FINANCIADO en estado ACTIVO o MORA (los que se cobran).
 *  - Se cobra el saldo de cada letra NO PAGADA ni ANULADA:
 *      - PENDIENTE / VENCIDO → importe completo de la letra.
 *      - PARCIAL            → saldo_pendiente (el resto por pagar).
 *  - Se excluyen las letras ya pagadas y las anuladas (renuncia/resolución).
 *  - Resultado agrupado por programa y desglosado en USD y PEN según la moneda
 *    de cada contrato.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CuentasPorCobrarServiceImpl implements CuentasPorCobrarService {

    private final ContratoRepository contratoRepository;

    @Override
    @Transactional(readOnly = true)
    public CuentasPorCobrarDTO obtenerCuentasPorCobrar() {
        // ── Carga de datos (mismo patrón que ReporteMora: sin producto cartesiano) ──
        Map<Integer, Contrato> mapaContratos = cargarContratosConClientesYLotes();
        Map<Integer, List<LetraCambio>> letrasPorContrato = cargarLetrasPorContrato();

        List<FilaCuenta> filas = new ArrayList<>();

        for (Contrato contrato : letrasPorContrato.keySet().stream()
                .map(mapaContratos::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())) {

            List<LetraCambio> letras = letrasPorContrato.getOrDefault(
                    contrato.getIdContrato(), Collections.emptyList());

            // Letras que aún se deben cobrar (no pagadas, no anuladas)
            List<LetraCambio> porCobrar = letras.stream()
                    .filter(l -> l.getEstadoLetra() != EstadoLetra.PAGADO
                            && l.getEstadoLetra() != EstadoLetra.ANULADO)
                    .collect(Collectors.toList());

            if (porCobrar.isEmpty()) continue;

            BigDecimal montoPorCobrar = porCobrar.stream()
                    .map(this::montoCobrableDeLetra)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String moneda = contrato.getMoneda() != null
                    ? contrato.getMoneda().name() : Moneda.USD.name();

            LocalDate proximaVencimiento = porCobrar.stream()
                    .map(LetraCambio::getFechaVencimiento)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(null);

            Lote lote = primerLote(contrato);
            String nombrePrograma = (lote != null && lote.getPrograma() != null)
                    ? lote.getPrograma().getNombrePrograma() : "SIN PROGRAMA";

            filas.add(new FilaCuenta(
                    contrato.getIdContrato(),
                    resolverNombreCliente(contrato.getClientes()),
                    lote != null ? lote.getManzana() : "",
                    lote != null ? lote.getNumeroLote() : "",
                    nombrePrograma,
                    moneda,
                    porCobrar.size(),
                    montoPorCobrar,
                    proximaVencimiento
            ));
        }

        // ── Agrupar por programa y totalizar USD / PEN ─────────────────────────
        Map<String, List<FilaCuenta>> porPrograma = filas.stream()
                .collect(Collectors.groupingBy(
                        FilaCuenta::getNombrePrograma,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<GrupoPrograma> programas = porPrograma.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    List<FilaCuenta> contratos = e.getValue().stream()
                            .sorted(Comparator
                                    .comparing((FilaCuenta f) -> f.getManzana() == null ? "" : f.getManzana(),
                                            Comparator.nullsLast(String::compareTo))
                                    .thenComparing(f -> extraerNumeroLote(f.getNumeroLote())))
                            .collect(Collectors.toList());
                    return new GrupoPrograma(
                            e.getKey(),
                            sumarPorMoneda(contratos, Moneda.USD),
                            sumarPorMoneda(contratos, Moneda.PEN),
                            contratos
                    );
                })
                .collect(Collectors.toList());

        BigDecimal totalUsd = programas.stream()
                .map(GrupoPrograma::getTotalUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPen = programas.stream()
                .map(GrupoPrograma::getTotalPen)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CuentasPorCobrarDTO(totalUsd, totalPen, programas);
    }

    /** Monto que aún se cobra de una letra según su estado. */
    private BigDecimal montoCobrableDeLetra(LetraCambio letra) {
        if (letra.getImporte() == null) return BigDecimal.ZERO;
        // PARCIAL: el resto por pagar es saldo_pendiente.
        if (letra.getEstadoLetra() == EstadoLetra.PARCIAL) {
            return letra.getSaldoPendiente() != null
                    ? letra.getSaldoPendiente() : BigDecimal.ZERO;
        }
        return letra.getImporte();
    }

    private BigDecimal sumarPorMoneda(List<FilaCuenta> filas, Moneda moneda) {
        return filas.stream()
                .filter(f -> moneda.name().equals(f.getMoneda()))
                .map(FilaCuenta::getMontoPorCobrar)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String resolverNombreCliente(Collection<ContratoCliente> clientes) {
        if (clientes == null || clientes.isEmpty()) return "SIN CLIENTE";
        // Prioriza al TITULAR (rol guardado); si no, el primero (orden ASC).
        return clientes.stream()
                .filter(cc -> cc.getCliente() != null)
                .sorted(Comparator.comparing(
                        cc -> cc.getTipoPropietario() != null && "TITULAR".equals(cc.getTipoPropietario().name()) ? 0 : 1
                ))
                .map(cc -> (cc.getCliente().getNombre() + " " + cc.getCliente().getApellidos()).trim().toUpperCase())
                .findFirst()
                .orElse("SIN CLIENTE");
    }

    private Lote primerLote(Contrato contrato) {
        if (contrato.getLotes() == null || contrato.getLotes().isEmpty()) return null;
        return contrato.getLotes().stream()
                .map(ContratoLote::getLote)
                .filter(Objects::nonNull)
                .min(Comparator
                        .comparing((Lote l) -> l.getManzana() == null ? "" : l.getManzana(),
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(l -> extraerNumeroLote(l.getNumeroLote())))
                .orElse(null);
    }

    private int extraerNumeroLote(String numeroLote) {
        if (numeroLote == null || numeroLote.isBlank()) return 0;
        String soloDigitos = numeroLote.replaceAll("\\D", "");
        if (soloDigitos.isEmpty()) return 0;
        try { return Integer.parseInt(soloDigitos); }
        catch (NumberFormatException e) { return 0; }
    }

    private Map<Integer, Contrato> cargarContratosConClientesYLotes() {
        Map<Integer, Contrato> mapa = new LinkedHashMap<>();
        for (Contrato c : contratoRepository.findAllConClientes()) {
            mapa.put(c.getIdContrato(), c);
        }
        for (Contrato c : contratoRepository.findAllConLotes()) {
            Contrato existente = mapa.get(c.getIdContrato());
            if (existente != null) {
                existente.setLotes(c.getLotes());
            } else {
                mapa.put(c.getIdContrato(), c);
            }
        }
        return mapa;
    }

    private Map<Integer, List<LetraCambio>> cargarLetrasPorContrato() {
        return contratoRepository.findFinanciadosActivosConLetras().stream()
                .collect(Collectors.toMap(
                        Contrato::getIdContrato,
                        c -> c.getLetrasCambio() != null ? c.getLetrasCambio() : Collections.emptyList(),
                        (a, b) -> a
                ));
    }
}