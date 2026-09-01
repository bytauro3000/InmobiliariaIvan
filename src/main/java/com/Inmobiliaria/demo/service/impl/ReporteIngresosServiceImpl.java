package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.dto.ResumenIngresoItemDTO;
import com.Inmobiliaria.demo.dto.ResumenIngresosRangoDTO;
import com.Inmobiliaria.demo.entity.Comprobante;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.ContratoCliente;
import com.Inmobiliaria.demo.entity.PagoInicial;
import com.Inmobiliaria.demo.entity.PagoInscripcionComprobante;
import com.Inmobiliaria.demo.entity.PagoLetras;
import com.Inmobiliaria.demo.entity.PagoMora;
import com.Inmobiliaria.demo.repository.PagoInicialRepository;
import com.Inmobiliaria.demo.repository.PagoInscripcionComprobanteRepository;
import com.Inmobiliaria.demo.repository.PagoLetraRepository;
import com.Inmobiliaria.demo.repository.PagoMoraRepository;
import com.Inmobiliaria.demo.service.ReporteIngresosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteIngresosServiceImpl implements ReporteIngresosService {

    private final PagoLetraRepository                pagoLetraRepository;
    private final PagoMoraRepository                pagoMoraRepository;
    private final PagoInicialRepository              pagoInicialRepository;
    private final PagoInscripcionComprobanteRepository pagoInscripcionComprobanteRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODO PRINCIPAL
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ResumenIngresosRangoDTO obtenerIngresosPorRango(LocalDate desde, LocalDate hasta) {

        List<ResumenIngresoItemDTO> detalle = new ArrayList<>();

        // ① ── Pagos de Letras ─────────────────────────────────────────────────
        List<PagoLetras> pagosLetras = pagoLetraRepository.findByFechaPagoBetween(desde, hasta);
        for (PagoLetras p : pagosLetras) {
            detalle.add(ResumenIngresoItemDTO.builder()
                    .tipoIngreso("LETRA")
                    .tipoComprobante(p.getComprobante() != null
                            ? p.getComprobante().getTipoComprobante().name() : null)
                    .idPago(p.getIdPago())
                    .numeroComprobante(p.getComprobante() != null
                            ? p.getComprobante().getNumeroCompleto() : null)
                    .fechaPago(p.getFechaPago())
                    .importePagado(p.getImportePagado())
                    .medioPago(p.getMedioPago())
                    .numeroOperacion(p.getNumeroOperacion())
                    .referencia(p.getLetra() != null
                            ? p.getLetra().getNumeroLetra() : null)
                    .idContrato(p.getLetra() != null && p.getLetra().getContrato() != null
                            ? p.getLetra().getContrato().getIdContrato() : null)
                    .nombreCliente(resolverNombreCliente(
                            p.getLetra() != null && p.getLetra().getContrato() != null
                                    ? p.getLetra().getContrato().getClientes() : null))
                    .observaciones(p.getObservaciones())
                    .anulado(Boolean.TRUE.equals(p.getAnulado()))
                    .fechaOperacion(p.getFechaOperacion())
                    .build());
        }

        // ② ── Pagos de Moras ──────────────────────────────────────────────────
        List<PagoMora> pagosMoras = pagoMoraRepository.findByFechaPagoBetween(desde, hasta);
        for (PagoMora p : pagosMoras) {
            Integer idContrato = null;
            java.util.Collection<ContratoCliente> clientesContrato = null;

            if (p.getMora() != null && p.getMora().getLetra() != null
                    && p.getMora().getLetra().getContrato() != null) {
                idContrato      = p.getMora().getLetra().getContrato().getIdContrato();
                clientesContrato = p.getMora().getLetra().getContrato().getClientes();
            }

            detalle.add(ResumenIngresoItemDTO.builder()
                    .tipoIngreso("MORA")
                    .tipoComprobante(p.getComprobante() != null
                            ? p.getComprobante().getTipoComprobante().name() : null)
                    .idPago(p.getIdPagoMora())
                    .numeroComprobante(p.getComprobante() != null
                            ? p.getComprobante().getNumeroCompleto() : null)
                    .fechaPago(p.getFechaPago())
                    .importePagado(p.getImportePagado())
                    .medioPago(p.getMedioPago())
                    .numeroOperacion(p.getNumeroOperacion())
                    .referencia(p.getMora() != null
                            ? "MORA-" + p.getMora().getIdMora() : null)
                    .idContrato(idContrato)
                    .nombreCliente(resolverNombreCliente(clientesContrato))
                    .observaciones(p.getObservaciones())
                    .anulado(Boolean.TRUE.equals(p.getAnulado()))
                    .build());
        }

        // ③ ── Pagos Iniciales ─────────────────────────────────────────────────
        List<PagoInicial> pagosIniciales = pagoInicialRepository.findByFechaPagoBetween(desde, hasta);
        for (PagoInicial p : pagosIniciales) {
            Integer idContrato = p.getContrato() != null
                    ? p.getContrato().getIdContrato() : null;

            detalle.add(ResumenIngresoItemDTO.builder()
                    .tipoIngreso("INICIAL")
                    .tipoComprobante(p.getComprobante() != null
                            ? p.getComprobante().getTipoComprobante().name() : null)
                    .idPago(p.getIdPagoInicial())
                    .numeroComprobante(p.getComprobante() != null
                            ? p.getComprobante().getNumeroCompleto() : null)
                    .fechaPago(p.getFechaPago())
                    .importePagado(p.getImportePagado())
                    .medioPago(p.getMedioPago())
                    .numeroOperacion(p.getNumeroOperacion())
                    .referencia(idContrato != null ? "C-" + idContrato : null)
                    .idContrato(idContrato)
                    .nombreCliente(resolverNombreCliente(
                            p.getContrato() != null ? p.getContrato().getClientes() : null))
                    .observaciones(p.getObservaciones())
                    .anulado(Boolean.TRUE.equals(p.getAnulado()))
                    .build());
        }

        // ④ ── Inscripciones de Servicios Básicos (tabla local) ──────
        List<ResumenIngresoItemDTO> itemsInscripciones = obtenerItemsInscripcionesPorRango(desde, hasta);
        detalle.addAll(itemsInscripciones);

        // ─── Ordenar toda la lista: 1) fecha ASC, 2) RECIBO antes que BOLETA,
        //                                   3) N° comprobante ASC dentro de cada tipo
        //      Para los items sin comprobante (ej. INSCRIPCION_SERVICIO) van al final.
        detalle.sort(
            Comparator.comparing(ResumenIngresoItemDTO::getFechaPago,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(
                    ResumenIngresoItemDTO::getTipoComprobante,
                    Comparator.nullsLast(compararTipoComprobante())
                )
                .thenComparing(
                    ResumenIngresoItemDTO::getNumeroComprobante,
                    Comparator.nullsLast(Comparator.naturalOrder())
                )
        );

        // ─── Calcular subtotales ──────────────────────────────────────────────
        BigDecimal totalLetras = pagoLetraRepository.sumImportePagadoByRango(desde, hasta);
        long cantidadLetras    = pagoLetraRepository.countByFechaPagoBetween(desde, hasta);

        BigDecimal totalMoras  = pagoMoraRepository.sumImportePagadoByRango(desde, hasta);
        long cantidadMoras     = pagoMoraRepository.countByFechaPagoBetween(desde, hasta);

        BigDecimal totalIniciales = pagoInicialRepository.sumImportePagadoByRango(desde, hasta);
        long cantidadIniciales    = pagoInicialRepository.countByFechaPagoBetween(desde, hasta);

        BigDecimal totalInscripciones = itemsInscripciones.stream()
                .filter(i -> !Boolean.TRUE.equals(i.getAnulado()))
                .map(ResumenIngresoItemDTO::getImportePagado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long cantidadInscripciones = itemsInscripciones.stream()
                .filter(i -> !Boolean.TRUE.equals(i.getAnulado()))
                .count();

        BigDecimal totalGeneral = totalLetras
                .add(totalMoras)
                .add(totalIniciales)
                .add(totalInscripciones);

        long cantidadTotal = detalle.size();

        return ResumenIngresosRangoDTO.builder()
                .fechaDesde(desde)
                .fechaHasta(hasta)
                .totalLetras(totalLetras)
                .cantidadLetras(cantidadLetras)
                .totalMoras(totalMoras)
                .cantidadMoras(cantidadMoras)
                .totalIniciales(totalIniciales)
                .cantidadIniciales(cantidadIniciales)
                .totalInscripcionesServicios(totalInscripciones)
                .cantidadInscripcionesServicios(cantidadInscripciones)
                .totalGeneral(totalGeneral)
                .cantidadTotal(cantidadTotal)
                .detalle(detalle)
                .build();
    }


    private List<ResumenIngresoItemDTO> obtenerItemsInscripcionesPorRango(
            LocalDate desde, LocalDate hasta) {

        List<ResumenIngresoItemDTO> items = new ArrayList<>();

        List<PagoInscripcionComprobante> pagos = pagoInscripcionComprobanteRepository
                .findByFechaPagoBetween(desde, hasta);

        for (PagoInscripcionComprobante p : pagos) {
            Comprobante comp = p.getComprobante();
            Contrato contrato = p.getContrato();

            items.add(ResumenIngresoItemDTO.builder()
                    .tipoIngreso("INSCRIPCION_SERVICIO")
                    .tipoComprobante(comp != null ? comp.getTipoComprobante().name() : null)
                    .idPago(p.getIdPagoInscripcionComprobante())
                    .numeroComprobante(comp != null ? comp.getNumeroCompleto() : null)
                    .fechaPago(p.getFechaPago())
                    .importePagado(p.getImportePagado())
                    .medioPago(p.getMedioPago())
                    .numeroOperacion(p.getNumeroOperacion())
                    .referencia(p.getTipoServicio())
                    .idContrato(contrato != null ? contrato.getIdContrato() : null)
                    .nombreCliente(resolverNombreCliente(
                            contrato != null ? contrato.getClientes() : null))
                    .observaciones(p.getObservaciones())
                    .anulado(Boolean.TRUE.equals(p.getAnulado()))
                    .build());
        }

        return items;
    }

    /**
     * Resuelve el nombre completo del primer titular (TITULAR_PRINCIPAL)
     * de la lista de ContratoCliente.
     *
     * Si no hay titular principal, toma el primer cliente de la lista.
     * Devuelve null si la lista está vacía o es nula.
     */
    private String resolverNombreCliente(java.util.Collection<ContratoCliente> clientes) {
        if (clientes == null || clientes.isEmpty()) return null;

        // Intentar obtener el TITULAR primero
        return clientes.stream()
                .filter(cc -> cc.getTipoPropietario() != null
                        && "TITULAR".equals(cc.getTipoPropietario().name()))
                .findFirst()
                .map(cc -> cc.getCliente().getNombre() + " " + cc.getCliente().getApellidos())
                .orElseGet(() -> {
                    // Si no hay titular principal, tomar el primero disponible
                    ContratoCliente primero = clientes.iterator().next();
                    if (primero.getCliente() != null) {
                        return primero.getCliente().getNombre() + " "
                                + primero.getCliente().getApellidos();
                    }
                    return null;
                });
    }

    /**
     * Comparator que ordena los tipos de comprobante de la siguiente forma:
     *  1) RECIBO   (primero)
     *  2) BOLETA
     *  3) FACTURA  (al final, por si llegara a aparecer)
     *  Cualquier otro valor se considera "después de FACTURA".
     */
    private static Comparator<String> compararTipoComprobante() {
        Map<String, Integer> orden = new HashMap<>();
        orden.put("RECIBO", 1);
        orden.put("BOLETA", 2);
        orden.put("FACTURA", 3);
        return (a, b) -> {
            Integer oa = orden.getOrDefault(a, 99);
            Integer ob = orden.getOrDefault(b, 99);
            return oa.compareTo(ob);
        };
    }
}