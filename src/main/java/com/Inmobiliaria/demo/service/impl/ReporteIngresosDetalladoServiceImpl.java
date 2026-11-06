package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.dto.IngresoDetalladoItemDTO;
import com.Inmobiliaria.demo.dto.ReporteIngresosDetalladoDTO;
import com.Inmobiliaria.demo.dto.TotalesMonedaDTO;
import com.Inmobiliaria.demo.entity.Cliente;
import com.Inmobiliaria.demo.entity.Comprobante;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.ContratoCliente;
import com.Inmobiliaria.demo.entity.PagoInicial;
import com.Inmobiliaria.demo.entity.PagoInscripcionComprobante;
import com.Inmobiliaria.demo.entity.PagoLetras;
import com.Inmobiliaria.demo.entity.PagoMora;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.enums.TipoOrigenComprobante;
import com.Inmobiliaria.demo.enums.TipoPropietario;
import com.Inmobiliaria.demo.repository.ComprobanteRepository;
import com.Inmobiliaria.demo.repository.PagoInicialRepository;
import com.Inmobiliaria.demo.repository.PagoInscripcionComprobanteRepository;
import com.Inmobiliaria.demo.repository.PagoLetraRepository;
import com.Inmobiliaria.demo.repository.PagoMoraRepository;
import com.Inmobiliaria.demo.service.ReporteIngresosDetalladoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteIngresosDetalladoServiceImpl implements ReporteIngresosDetalladoService {

    private final ComprobanteRepository comprobanteRepository;
    private final PagoLetraRepository pagoLetraRepository;
    private final PagoMoraRepository pagoMoraRepository;
    private final PagoInicialRepository pagoInicialRepository;
    private final PagoInscripcionComprobanteRepository pagoInscripcionComprobanteRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODO PRINCIPAL
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ReporteIngresosDetalladoDTO obtenerReporteDetallado(
            LocalDate desde, LocalDate hasta,
            List<TipoComprobante> tipos, String tipoFiltro) {

        List<Comprobante> comprobantes = comprobanteRepository.buscarDetallado(desde, hasta, tipos);

        if (comprobantes.isEmpty()) {
            return ReporteIngresosDetalladoDTO.builder()
                    .fechaDesde(desde)
                    .fechaHasta(hasta)
                    .tipoFiltro(tipoFiltro)
                    .cantidadTotal(0)
                    .cantidadAnuladas(0)
                    .totales(List.of())
                    .detalle(List.of())
                    .build();
        }

        // ① Receptor + moneda + anulado del pago (≤4 consultas batch, sin N+1)
        Map<TipoOrigenComprobante, Map<Integer, InfoPago>> pagos = resolverPagos(comprobantes);

        // ② Notas de crédito que anulan cada comprobante (2 consultas)
        Map<Long, Comprobante> notasCredito = resolverNotasCredito(comprobantes);

        // ③ Filas + totales por moneda
        List<IngresoDetalladoItemDTO> detalle = new ArrayList<>();
        Map<String, Acumulador> acumuladores = new LinkedHashMap<>();
        long cantidadAnuladas = 0;

        for (Comprobante c : comprobantes) {
            InfoPago info = pagos.getOrDefault(c.getTipoOrigen(), Map.of())
                    .get(c.getReferenciaId());
            Comprobante nc = notasCredito.get(c.getIdComprobante());

            String estado;
            if (nc != null) {
                estado = "ANULADO_NC";
            } else if (info != null && info.anulado()) {
                estado = "ANULADO_PAGO";
            } else {
                estado = "VIGENTE";
            }
            boolean anulado = !"VIGENTE".equals(estado);
            if (anulado) cantidadAnuladas++;

            String moneda = info != null ? info.moneda() : "N/D";
            String dni = info != null ? info.dni() : null;
            String receptor = info != null ? info.receptor() : "-";

            detalle.add(IngresoDetalladoItemDTO.builder()
                    .idComprobante(c.getIdComprobante())
                    .numeroCompleto(c.getNumeroCompleto())
                    .tipoComprobante(c.getTipoComprobante().name())
                    .fechaEmision(c.getFechaEmision())
                    .monto(c.getMonto())
                    .moneda(moneda)
                    .dni(dni)
                    .receptor(receptor)
                    .estado(estado)
                    .ncNumeroCompleto(nc != null ? nc.getNumeroCompleto() : null)
                    .ncFechaEmision(nc != null ? nc.getFechaEmision() : null)
                    .build());

            Acumulador acum = acumuladores.computeIfAbsent(moneda, k -> new Acumulador());
            acum.bruto = acum.bruto.add(c.getMonto());
            if (anulado) {
                acum.anulada = acum.anulada.add(c.getMonto());
            }
        }

        List<TotalesMonedaDTO> totales = acumuladores.entrySet().stream()
                .map(e -> TotalesMonedaDTO.builder()
                        .moneda(e.getKey())
                        .bruto(e.getValue().bruto)
                        .anulada(e.getValue().anulada)
                        .neto(e.getValue().bruto.subtract(e.getValue().anulada))
                        .build())
                .sorted((a, b) -> Integer.compare(ordenMoneda(a.getMoneda()), ordenMoneda(b.getMoneda())))
                .toList();

        return ReporteIngresosDetalladoDTO.builder()
                .fechaDesde(desde)
                .fechaHasta(hasta)
                .tipoFiltro(tipoFiltro)
                .cantidadTotal(detalle.size())
                .cantidadAnuladas(cantidadAnuladas)
                .totales(totales)
                .detalle(detalle)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RECEPTOR + MONEDA (batch por tipo de origen)
    // ─────────────────────────────────────────────────────────────────────────

    private Map<TipoOrigenComprobante, Map<Integer, InfoPago>> resolverPagos(
            List<Comprobante> comprobantes) {

        Map<TipoOrigenComprobante, List<Integer>> idsPorOrigen = new EnumMap<>(TipoOrigenComprobante.class);
        for (Comprobante c : comprobantes) {
            if (c.getTipoOrigen() != null && c.getReferenciaId() != null) {
                idsPorOrigen.computeIfAbsent(c.getTipoOrigen(), k -> new ArrayList<>())
                        .add(c.getReferenciaId());
            }
        }

        Map<TipoOrigenComprobante, Map<Integer, InfoPago>> resultado = new EnumMap<>(TipoOrigenComprobante.class);

        List<Integer> idsLetra = idsPorOrigen.get(TipoOrigenComprobante.PAGO_LETRA);
        if (idsLetra != null) {
            Map<Integer, InfoPago> mapa = new HashMap<>();
            for (PagoLetras p : pagoLetraRepository.buscarPorIdsConClientes(idsLetra)) {
                Contrato contrato = p.getLetra() != null ? p.getLetra().getContrato() : null;
                mapa.put(p.getIdPago(), infoDesdeContrato(contrato, p.getAnulado()));
            }
            resultado.put(TipoOrigenComprobante.PAGO_LETRA, mapa);
        }

        List<Integer> idsMora = idsPorOrigen.get(TipoOrigenComprobante.PAGO_MORA);
        if (idsMora != null) {
            Map<Integer, InfoPago> mapa = new HashMap<>();
            for (PagoMora p : pagoMoraRepository.buscarPorIdsConClientes(idsMora)) {
                Contrato contrato = null;
                if (p.getMora() != null && p.getMora().getLetra() != null) {
                    contrato = p.getMora().getLetra().getContrato();
                }
                mapa.put(p.getIdPagoMora(), infoDesdeContrato(contrato, p.getAnulado()));
            }
            resultado.put(TipoOrigenComprobante.PAGO_MORA, mapa);
        }

        List<Integer> idsInicial = idsPorOrigen.get(TipoOrigenComprobante.PAGO_INICIAL);
        if (idsInicial != null) {
            Map<Integer, InfoPago> mapa = new HashMap<>();
            for (PagoInicial p : pagoInicialRepository.buscarPorIdsConClientes(idsInicial)) {
                mapa.put(p.getIdPagoInicial(), infoDesdeContrato(p.getContrato(), p.getAnulado()));
            }
            resultado.put(TipoOrigenComprobante.PAGO_INICIAL, mapa);
        }

        List<Integer> idsInscripcion = idsPorOrigen.get(TipoOrigenComprobante.PAGO_INSCRIPCION);
        if (idsInscripcion != null) {
            Map<Integer, InfoPago> mapa = new HashMap<>();
            for (PagoInscripcionComprobante p :
                    pagoInscripcionComprobanteRepository.buscarPorIdsConClientes(idsInscripcion)) {
                mapa.put(p.getIdPagoInscripcionComprobante(),
                        infoDesdeContrato(p.getContrato(), p.getAnulado()));
            }
            resultado.put(TipoOrigenComprobante.PAGO_INSCRIPCION, mapa);
        }

        return resultado;
    }

    /**
     * Receptor = TITULAR del contrato (o el primer cliente), formato
     * "DNI - Apellidos, Nombres". Moneda = contrato.moneda (USD|PEN).
     */
    private InfoPago infoDesdeContrato(Contrato contrato, Boolean pagoAnulado) {
        boolean anulado = Boolean.TRUE.equals(pagoAnulado);

        if (contrato == null) {
            return new InfoPago(null, "-", "N/D", anulado);
        }

        String moneda = contrato.getMoneda() != null ? contrato.getMoneda().name() : "N/D";

        String dni = null;
        String apellidos = null;
        String nombres = null;

        if (contrato.getClientes() != null && !contrato.getClientes().isEmpty()) {
            ContratoCliente titular = contrato.getClientes().stream()
                    .filter(cc -> cc.getTipoPropietario() != null
                            && cc.getTipoPropietario() == TipoPropietario.TITULAR)
                    .findFirst()
                    .orElseGet(() -> contrato.getClientes().iterator().next());

            Cliente cliente = titular != null ? titular.getCliente() : null;
            if (cliente != null) {
                dni = cliente.getNumDoc();
                apellidos = cliente.getApellidos();
                nombres = cliente.getNombre();
            }
        }

        StringBuilder nombre = new StringBuilder();
        if (apellidos != null && !apellidos.isBlank()) nombre.append(apellidos.trim());
        if (nombres != null && !nombres.isBlank()) {
            if (nombre.length() > 0) nombre.append(", ");
            nombre.append(nombres.trim());
        }

        String receptor;
        if (nombre.length() == 0) {
            receptor = "-";
        } else if (dni != null && !dni.isBlank()) {
            receptor = dni.trim() + " - " + nombre;
        } else {
            receptor = nombre.toString();
        }

        String dniLimpio = (dni != null && !dni.isBlank()) ? dni.trim() : null;
        return new InfoPago(dniLimpio, receptor, moneda, anulado);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NOTAS DE CRÉDITO (2 consultas para todo el lote)
    // ─────────────────────────────────────────────────────────────────────────

    private Map<Long, Comprobante> resolverNotasCredito(List<Comprobante> comprobantes) {
        Map<Long, Comprobante> resultado = new HashMap<>();

        List<Long> ids = comprobantes.stream()
                .map(Comprobante::getIdComprobante)
                .toList();

        // Fuente primaria: NC → original (FK comprobante_referencia_id)
        for (Comprobante nc : comprobanteRepository.findByComprobanteReferenciaIdComprobanteIn(ids)) {
            if (nc.getTipoComprobante() == TipoComprobante.NOTA_CREDITO
                    && nc.getComprobanteReferencia() != null) {
                resultado.put(nc.getComprobanteReferencia().getIdComprobante(), nc);
            }
        }

        // Fallback: original.id_nota_credito_anulacion (datos antiguos)
        List<Long> idsAnulacion = comprobantes.stream()
                .map(Comprobante::getIdNotaCreditoAnulacion)
                .filter(id -> id != null)
                .toList();

        if (!idsAnulacion.isEmpty()) {
            Map<Long, Comprobante> porId = new HashMap<>();
            for (Comprobante nc : comprobanteRepository.findAllById(idsAnulacion)) {
                porId.put(nc.getIdComprobante(), nc);
            }
            for (Comprobante c : comprobantes) {
                if (c.getIdNotaCreditoAnulacion() == null
                        || resultado.containsKey(c.getIdComprobante())) {
                    continue;
                }
                Comprobante nc = porId.get(c.getIdNotaCreditoAnulacion());
                if (nc != null) {
                    resultado.put(c.getIdComprobante(), nc);
                }
            }
        }

        return resultado;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AUXILIARES
    // ─────────────────────────────────────────────────────────────────────────

    private record InfoPago(String dni, String receptor, String moneda, boolean anulado) {}

    private static final class Acumulador {
        private BigDecimal bruto = BigDecimal.ZERO;
        private BigDecimal anulada = BigDecimal.ZERO;
    }

    /** Orden de presentación de monedas: USD → PEN → N/D. */
    private static int ordenMoneda(String moneda) {
        return switch (moneda) {
            case "USD" -> 0;
            case "PEN" -> 1;
            default -> 2;
        };
    }
}
