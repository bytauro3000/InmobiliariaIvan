package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.client.InscripcionClient;
import com.Inmobiliaria.demo.dto.ResumenIngresoItemDTO;
import com.Inmobiliaria.demo.dto.ResumenIngresosRangoDTO;
import com.Inmobiliaria.demo.entity.ContratoCliente;
import com.Inmobiliaria.demo.entity.PagoInicial;
import com.Inmobiliaria.demo.entity.PagoLetras;
import com.Inmobiliaria.demo.entity.PagoMora;
import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.repository.PagoInicialRepository;
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
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteIngresosServiceImpl implements ReporteIngresosService {

    private final PagoLetraRepository   pagoLetraRepository;
    private final PagoMoraRepository    pagoMoraRepository;
    private final PagoInicialRepository pagoInicialRepository;
    private final InscripcionClient     inscripcionClient;

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
                    .build());
        }

        // ③ ── Pagos Iniciales ─────────────────────────────────────────────────
        List<PagoInicial> pagosIniciales = pagoInicialRepository.findByFechaPagoBetween(desde, hasta);
        for (PagoInicial p : pagosIniciales) {
            Integer idContrato = p.getContrato() != null
                    ? p.getContrato().getIdContrato() : null;

            detalle.add(ResumenIngresoItemDTO.builder()
                    .tipoIngreso("INICIAL")
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
                    .build());
        }

        // ④ ── Inscripciones de Servicios Básicos (microservicio externo) ──────
        List<ResumenIngresoItemDTO> itemsInscripciones = obtenerItemsInscripcionesPorRango(desde, hasta);
        detalle.addAll(itemsInscripciones);

        // ─── Ordenar toda la lista por fecha ASC, luego por tipo ─────────────
        detalle.sort(Comparator.comparing(ResumenIngresoItemDTO::getFechaPago)
                .thenComparing(ResumenIngresoItemDTO::getTipoIngreso));

        // ─── Calcular subtotales ──────────────────────────────────────────────
        BigDecimal totalLetras = pagoLetraRepository.sumImportePagadoByRango(desde, hasta);
        long cantidadLetras    = pagoLetraRepository.countByFechaPagoBetween(desde, hasta);

        BigDecimal totalMoras  = pagoMoraRepository.sumImportePagadoByRango(desde, hasta);
        long cantidadMoras     = pagoMoraRepository.countByFechaPagoBetween(desde, hasta);

        BigDecimal totalIniciales = pagoInicialRepository.sumImportePagadoByRango(desde, hasta);
        long cantidadIniciales    = pagoInicialRepository.countByFechaPagoBetween(desde, hasta);

        BigDecimal totalInscripciones = itemsInscripciones.stream()
                .map(ResumenIngresoItemDTO::getImportePagado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long cantidadInscripciones = itemsInscripciones.size();

        BigDecimal totalGeneral = totalLetras
                .add(totalMoras)
                .add(totalIniciales)
                .add(totalInscripciones);

        long cantidadTotal = cantidadLetras + cantidadMoras + cantidadIniciales + cantidadInscripciones;

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


    @SuppressWarnings("unchecked")
    private List<ResumenIngresoItemDTO> obtenerItemsInscripcionesPorRango(
            LocalDate desde, LocalDate hasta) {

        List<ResumenIngresoItemDTO> items = new ArrayList<>();

        try {
            // Llamada Feign: GET /api/inscripciones/ingresos-rango?desde=...&hasta=...
            List<Map<String, Object>> respuesta =
                    inscripcionClient.obtenerIngresosPorRango(desde.toString(), hasta.toString());

            if (respuesta == null) return items;

            for (Map<String, Object> abono : respuesta) {
                BigDecimal monto = abono.get("montoPagado") != null
                        ? new BigDecimal(abono.get("montoPagado").toString())
                        : BigDecimal.ZERO;

                LocalDate fechaPago = abono.get("fechaPago") != null
                        ? LocalDate.parse(abono.get("fechaPago").toString())
                        : null;

                MedioPago medioPago = null;
                if (abono.get("medioPago") != null) {
                    try {
                        medioPago = MedioPago.valueOf(abono.get("medioPago").toString());
                    } catch (IllegalArgumentException ignored) {
                        // Si el valor no coincide con el enum, se deja null
                    }
                }

                Integer idPago = abono.get("idAbono") != null
                        ? Integer.valueOf(abono.get("idAbono").toString()) : null;

                Integer idContrato = abono.get("idContrato") != null
                        ? Integer.valueOf(abono.get("idContrato").toString()) : null;

                String tipoServicio = abono.get("tipoServicio") != null
                        ? abono.get("tipoServicio").toString() : null;

                items.add(ResumenIngresoItemDTO.builder()
                        .tipoIngreso("INSCRIPCION_SERVICIO")
                        .idPago(idPago)
                        .numeroComprobante(null)   // El microservicio no emite comprobante centralizado
                        .fechaPago(fechaPago)
                        .importePagado(monto)
                        .medioPago(medioPago)
                        .numeroOperacion(abono.get("numeroOperacion") != null
                                ? abono.get("numeroOperacion").toString() : null)
                        .referencia(tipoServicio)
                        .idContrato(idContrato)
                        .nombreCliente(abono.get("nombreCliente") != null
                                ? abono.get("nombreCliente").toString() : null)
                        .observaciones(abono.get("observaciones") != null
                                ? abono.get("observaciones").toString() : null)
                        .build());
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener inscripciones de servicios para el rango [{} - {}]: {}",
                    desde, hasta, e.getMessage());
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

        // Intentar obtener el TITULAR_PRINCIPAL primero
        return clientes.stream()
                .filter(cc -> cc.getTipoPropietario() != null
                        && "TITULAR_PRINCIPAL".equals(cc.getTipoPropietario().name()))
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
}