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

        // 1. Traer contratos con clientes y lotes (dos queries para evitar
        //    MultipleBagFetchException, mismo patrón de ContratoServiceImpl)
        List<Contrato> conClientes = contratoRepository.findAllConClientes();
        List<Contrato> conLotes    = contratoRepository.findAllConLotes();

        // Mapa idContrato → lotes
        Map<Integer, List<ContratoLote>> lotesPorContrato = conLotes.stream()
                .filter(c -> c.getLotes() != null)
                .collect(Collectors.toMap(
                        Contrato::getIdContrato,
                        Contrato::getLotes,
                        (a, b) -> a
                ));

        // 2. Traer letras (misma query que usa el scheduler)
        List<Contrato> contratosConLetras = contratoRepository.findFinanciadosActivosConLetras();
        Map<Integer, List<LetraCambio>> letrasPorContrato = contratosConLetras.stream()
                .collect(Collectors.toMap(
                        Contrato::getIdContrato,
                        c -> c.getLetrasCambio() != null ? c.getLetrasCambio() : Collections.emptyList(),
                        (a, b) -> a
                ));

        // 3. Filtrar solo contratos EN MORA
        List<Contrato> contratosEnMora = conClientes.stream()
                .filter(c -> EstadoContrato.MORA == c.getEstadoContrato())
                .collect(Collectors.toList());

        // 4. Construir una fila por contrato
        List<FilaClienteMora> filas = new ArrayList<>();

        for (Contrato contrato : contratosEnMora) {
            List<LetraCambio> letras = letrasPorContrato.getOrDefault(
                    contrato.getIdContrato(), Collections.emptyList());

            if (letras.isEmpty()) continue;

            // ── Mismo criterio que ContratoEstadoScheduler ────────────────────
            // Número de la última letra PAGADA (0 si ninguna está pagada)
            int numUltimaPagada = letras.stream()
                    .filter(l -> l.getEstadoLetra() == EstadoLetra.PAGADO)
                    .mapToInt(l -> extraerNumeroLetra(l.getNumeroLetra()))
                    .max()
                    .orElse(0);

            // Solo las VENCIDAS con número mayor al de la última pagada
            List<LetraCambio> letrasAtrasadas = letras.stream()
                    .filter(l -> l.getEstadoLetra() == EstadoLetra.VENCIDO)
                    .filter(l -> extraerNumeroLetra(l.getNumeroLetra()) > numUltimaPagada)
                    .sorted(Comparator.comparingInt(l -> extraerNumeroLetra(l.getNumeroLetra())))
                    .collect(Collectors.toList());

            if (letrasAtrasadas.isEmpty()) continue;

            // ── Nombre de clientes ("JUAN PÉREZ / MARÍA PÉREZ") ──────────────
            String nombreClientes = contrato.getClientes() == null ? "" :
                    contrato.getClientes().stream()
                            .map(ContratoCliente::getCliente)
                            .filter(Objects::nonNull)
                            .map(cl -> (cl.getNombre() + " " + cl.getApellidos()).trim().toUpperCase())
                            .collect(Collectors.joining(" / "));

            // ── Celular del primer titular ────────────────────────────────────
            String celular = (contrato.getClientes() == null || contrato.getClientes().isEmpty()) ? "" :
                    Optional.ofNullable(contrato.getClientes().get(0).getCliente())
                            .map(Cliente::getCelular)
                            .orElse("");

            // ── Lotes: TODOS ordenados por manzana luego numeroLote ───────────
            List<ContratoLote> lotesContrato = lotesPorContrato.getOrDefault(
                    contrato.getIdContrato(), Collections.emptyList());

            // Ordenar los lotes del contrato: por manzana (alfa) luego por número de lote
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

            // ── Construir fila ─────────────────────────────────────────────────
            FilaClienteMora fila = new FilaClienteMora(
                    nombreClientes,
                    manzanas,
                    numeroLotes,
                    letrasAtrasadas.size(),
                    rangoLetras,
                    importeTotal,
                    moneda,
                    celular,
                    contrato.getIdContrato(),
                    nombrePrograma,
                    fechaVencimientoInicio
            );

            filas.add(fila);
        }

        // 5. Agrupar por programa y ordenar filas dentro de cada grupo
        //    por manzana del primer lote, luego por número del primer lote
        Map<String, List<FilaClienteMora>> porPrograma = filas.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getNombrePrograma() != null ? f.getNombrePrograma() : "SIN PROGRAMA",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return porPrograma.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    // Ordenar clientes dentro del programa: MZ → LT (primer lote)
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