package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.dto.ReporteClientesMoraDTO;
import com.Inmobiliaria.demo.dto.ReporteClientesMoraDTO.FilaClienteMora;
import com.Inmobiliaria.demo.entity.*;
import com.Inmobiliaria.demo.enums.EstadoContrato;
import com.Inmobiliaria.demo.enums.EstadoLetra;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import com.Inmobiliaria.demo.service.ReporteMoraService;
import com.Inmobiliaria.demo.util.ReporteClientesMoraPdf;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteMoraServiceImpl implements ReporteMoraService {

    private final ContratoRepository contratoRepository;

    // ─────────────────────────────────────────────────────────────────────────
    //  LÓGICA DE NEGOCIO
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ReporteClientesMoraDTO> obtenerClientesEnMora() {
        Map<Integer, Contrato> mapaContratos = cargarContratosConClientesYLotes();
        Map<Integer, List<LetraCambio>> letrasPorContrato = cargarLetrasPorContrato();

        List<Contrato> contratosEnMora = mapaContratos.values().stream()
                .filter(c -> EstadoContrato.MORA == c.getEstadoContrato())
                .collect(Collectors.toList());

        // Criterio del scheduler: solo letras marcadas VENCIDO.
        Predicate<LetraCambio> esAtrasada = l ->
                l.getEstadoLetra() == EstadoLetra.VENCIDO;

        return construirReporte(contratosEnMora, letrasPorContrato, esAtrasada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReporteClientesMoraDTO> obtenerClientesLetrasVencidas() {
        LocalDate hoy = LocalDate.now();

        Map<Integer, Contrato> mapaContratos = cargarContratosConClientesYLotes();
        Map<Integer, List<LetraCambio>> letrasPorContrato = cargarLetrasPorContrato();

// Solo clientes que tienen una letra sin pagar cuyo vencimiento es HOY.
                    // Aunque acumule letras vencidas de meses anteriores (ej. 25 y 26),
                    // recién aparece cuando la siguiente letra por pagar vence el día de hoy
                    // (ej. la 27 vence 28/08 → aparece el 28/08 con el acumulado 25-27).
                    List<Contrato> conVencimientoHoy = mapaContratos.values().stream()
                            .filter(c -> {
                                List<LetraCambio> letras = letrasPorContrato.getOrDefault(
                                        c.getIdContrato(), Collections.emptyList());
                                if (letras.isEmpty()) return false;

                                int numUltimaPagada = letras.stream()
                                        .filter(l -> l.getEstadoLetra() == EstadoLetra.PAGADO)
                                        .mapToInt(l -> extraerNumeroLetra(l.getNumeroLetra()))
                                        .max()
                                        .orElse(0);

                                return letras.stream()
                                        .filter(l -> l.getEstadoLetra() != EstadoLetra.PAGADO
                                                && l.getEstadoLetra() != EstadoLetra.ANULADO)
                                        .filter(l -> l.getFechaVencimiento() != null)
                                        .filter(l -> l.getFechaVencimiento().isEqual(hoy))
                                        .anyMatch(l -> extraerNumeroLetra(l.getNumeroLetra()) > numUltimaPagada);
                            })
                            .collect(Collectors.toList());

        // Acumulado: todas las letras no pagadas vencidas a la fecha (ej. 25-27)
        Predicate<LetraCambio> esAtrasada = l ->
                l.getEstadoLetra() != EstadoLetra.PAGADO
                        && l.getEstadoLetra() != EstadoLetra.ANULADO
                        && l.getFechaVencimiento() != null
                        && !l.getFechaVencimiento().isAfter(hoy);

        return construirReporte(conVencimientoHoy, letrasPorContrato, esAtrasada);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LÓGICA COMPARTIDA
    // ─────────────────────────────────────────────────────────────────────────

    /** Carga contratos con clientes y lotes en dos queries (sin producto cartesiano). */
    private Map<Integer, Contrato> cargarContratosConClientesYLotes() {
        Map<Integer, Contrato> mapaContratos = new LinkedHashMap<>();
        for (Contrato c : contratoRepository.findAllConClientes()) mapaContratos.put(c.getIdContrato(), c);
        for (Contrato c : contratoRepository.findAllConLotes()) {
            Contrato existente = mapaContratos.get(c.getIdContrato());
            if (existente != null) {
                existente.setLotes(c.getLotes());
            } else {
                mapaContratos.put(c.getIdContrato(), c);
            }
        }
        return mapaContratos;
    }

    /** Carga letras de contratos financiados activos/mora en una sola query. */
    private Map<Integer, List<LetraCambio>> cargarLetrasPorContrato() {
        return contratoRepository.findFinanciadosActivosConLetras().stream()
                .collect(Collectors.toMap(
                        Contrato::getIdContrato,
                        c -> c.getLetrasCambio() != null ? c.getLetrasCambio() : Collections.emptyList(),
                        (a, b) -> a
                ));
    }

    /** Construye el reporte agrupado por programa a partir de contratos y un criterio de letra atrasada. */
    private List<ReporteClientesMoraDTO> construirReporte(
            List<Contrato> contratos,
            Map<Integer, List<LetraCambio>> letrasPorContrato,
            Predicate<LetraCambio> esAtrasada) {

        List<FilaClienteMora> filas = new ArrayList<>();

        for (Contrato contrato : contratos) {
            List<LetraCambio> letras = letrasPorContrato.getOrDefault(
                    contrato.getIdContrato(), Collections.emptyList());

            if (letras.isEmpty()) continue;

            // ── Mismo criterio que ContratoEstadoScheduler ────────────────────
            int numUltimaPagada = letras.stream()
                    .filter(l -> l.getEstadoLetra() == EstadoLetra.PAGADO)
                    .mapToInt(l -> extraerNumeroLetra(l.getNumeroLetra()))
                    .max()
                    .orElse(0);

            // Solo las atrasadas (según criterio) con número mayor al de la última pagada
            List<LetraCambio> letrasAtrasadas = letras.stream()
                    .filter(esAtrasada)
                    .filter(l -> extraerNumeroLetra(l.getNumeroLetra()) > numUltimaPagada)
                    .sorted(Comparator.comparingInt(l -> extraerNumeroLetra(l.getNumeroLetra())))
                    .collect(Collectors.toList());

            if (letrasAtrasadas.isEmpty()) continue;

            filas.add(construirFila(contrato, letrasAtrasadas));
        }

        // ── Agrupar por programa y ordenar MZ → LT ───────────────────────────
        Map<String, List<FilaClienteMora>> porPrograma = filas.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getNombrePrograma() != null ? f.getNombrePrograma() : "SIN PROGRAMA",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return porPrograma.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    List<FilaClienteMora> clientesOrdenados = e.getValue().stream()
                            .sorted(Comparator
                                    .comparing((FilaClienteMora f) ->
                                            f.getManzanas().isEmpty() ? "" : f.getManzanas().get(0),
                                            Comparator.nullsLast(String::compareTo))
                                    .thenComparingInt(f ->
                                            f.getNumeroLotes().isEmpty() ? 0
                                                    : parsearNumeroLote(f.getNumeroLotes().get(0))))
                            .collect(Collectors.toList());
                    return new ReporteClientesMoraDTO(e.getKey(), clientesOrdenados);
                })
                .collect(Collectors.toList());
    }

    /** Construye la fila de un contrato con sus letras atrasadas. */
    private FilaClienteMora construirFila(Contrato contrato, List<LetraCambio> letrasAtrasadas) {

        // ── Nombre de clientes ("JUAN PÉREZ / MARÍA PÉREZ") ──────────────
        String nombreClientes = contrato.getClientes() == null ? "" :
                contrato.getClientes().stream()
                        .map(ContratoCliente::getCliente)
                        .filter(Objects::nonNull)
                        .map(cl -> (cl.getNombre() + " " + cl.getApellidos()).trim().toUpperCase())
                        .collect(Collectors.joining(" / "));

        // ── Celular del primer titular ────────────────────────────────────
        String celular = (contrato.getClientes() == null || contrato.getClientes().isEmpty()) ? "" :
                Optional.ofNullable(contrato.getClientes().iterator().next().getCliente())
                        .map(Cliente::getCelular)
                        .orElse("");

        // ── Lotes: TODOS ordenados por manzana luego numeroLote ───────────
        List<ContratoLote> lotesContrato = contrato.getLotes() != null
                ? new ArrayList<>(contrato.getLotes())
                : new ArrayList<>();

        List<ContratoLote> lotesOrdenados = lotesContrato.stream()
                .filter(cl -> cl.getLote() != null)
                .sorted(Comparator
                        .comparing((ContratoLote cl) -> cl.getLote().getManzana(),
                                Comparator.nullsLast(String::compareTo))
                        .thenComparingInt(cl -> parsearNumeroLote(cl.getLote().getNumeroLote())))
                .collect(Collectors.toList());

        List<String> manzanas    = lotesOrdenados.stream()
                .map(cl -> cl.getLote().getManzana())
                .collect(Collectors.toList());
        List<String> numeroLotes = lotesOrdenados.stream()
                .map(cl -> cl.getLote().getNumeroLote())
                .collect(Collectors.toList());
        List<BigDecimal> areas   = lotesOrdenados.stream()
                .map(cl -> cl.getLote().getArea())
                .collect(Collectors.toList());

        // ── Nombre del programa (del primer lote ordenado) ────────────────
        String nombrePrograma = lotesOrdenados.isEmpty() ? "SIN PROGRAMA" :
                Optional.ofNullable(lotesOrdenados.get(0).getLote().getPrograma())
                        .map(Programa::getNombrePrograma)
                        .orElse("SIN PROGRAMA");

        // ── Rango legible de letras vencidas (ej: "1-4", "19-20", "1, 3") ─
        List<Integer> numerosAtrasados = letrasAtrasadas.stream()
                .map(l -> extraerNumeroLetra(l.getNumeroLetra()))
                .collect(Collectors.toList());
        String rangoLetras = construirRango(numerosAtrasados);

        // ── Importe total adeudado ────────────────────────────────────────
        BigDecimal importeTotal = letrasAtrasadas.stream()
                .map(LetraCambio::getImporte)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String moneda = contrato.getMoneda() != null ? contrato.getMoneda().name() : "USD";

        // ── Fecha de vencimiento de la primera letra atrasada ─────────────
        LocalDate fechaVencimientoInicio = letrasAtrasadas.get(0).getFechaVencimiento();

        return new FilaClienteMora(
                nombreClientes,
                manzanas,
                numeroLotes,
                areas,
                letrasAtrasadas.size(),
                rangoLetras,
                importeTotal,
                moneda,
                celular,
                contrato.getIdContrato(),
                nombrePrograma,
                fechaVencimientoInicio
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PDF — delega al util, igual que ContratoServiceImpl → ContratoFloridaPdf
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] generarPdfClientesEnMora() {
        List<ReporteClientesMoraDTO> datos = obtenerClientesEnMora();
        return ReporteClientesMoraPdf.generar(datos);   // ← solo delega al util
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UTILIDADES PRIVADAS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extrae el número entero de un numeroLetra con formato "19/120" → 19.
     * Idéntico al método del ContratoEstadoScheduler.
     */
    private int extraerNumeroLetra(String numeroLetra) {
        if (numeroLetra == null || numeroLetra.isBlank()) return 0;
        String parte = numeroLetra.contains("/")
                ? numeroLetra.split("/")[0].trim()
                : numeroLetra.trim();
        try { return Integer.parseInt(parte); }
        catch (NumberFormatException e) { return 0; }
    }

    /**
     * Convierte un numeroLote como "02", "25", "A3" → entero para ordenar.
     * Extrae solo los dígitos del string.
     */
    private int parsearNumeroLote(String numeroLote) {
        if (numeroLote == null || numeroLote.isBlank()) return 0;
        String soloDigitos = numeroLote.replaceAll("\\D", "");
        if (soloDigitos.isEmpty()) return 0;
        try { return Integer.parseInt(soloDigitos); }
        catch (NumberFormatException e) { return 0; }
    }

    /**
     * Construye el rango legible de números de letras atrasadas.
     *
     * Ejemplos:
     *   [1,2,3,4]   → "1-4"
     *   [1,2,4]     → "1, 2, 4"
     *   [20]        → "20"
     *   [19,20]     → "19-20"
     *   [1,2,3,6,7] → "1-3, 6-7"
     */
    private String construirRango(List<Integer> numeros) {
        if (numeros == null || numeros.isEmpty()) return "";

        List<Integer> sorted = numeros.stream().sorted().collect(Collectors.toList());
        StringBuilder sb     = new StringBuilder();

        int inicio = sorted.get(0);
        int prev   = sorted.get(0);

        for (int i = 1; i < sorted.size(); i++) {
            int actual = sorted.get(i);
            if (actual == prev + 1) {
                prev = actual;
            } else {
                if (sb.length() > 0) sb.append(", ");
                sb.append(prev == inicio ? String.valueOf(inicio) : inicio + "-" + prev);
                inicio = actual;
                prev   = actual;
            }
        }

        if (sb.length() > 0) sb.append(", ");
        sb.append(prev == inicio ? String.valueOf(inicio) : inicio + "-" + prev);

        return sb.toString();
    }
}