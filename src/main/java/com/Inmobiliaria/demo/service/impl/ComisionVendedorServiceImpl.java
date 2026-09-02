package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.dto.*;
import com.Inmobiliaria.demo.entity.*;
import com.Inmobiliaria.demo.enums.EstadoComision;
import com.Inmobiliaria.demo.enums.EstadoContrato;
import com.Inmobiliaria.demo.enums.EstadoLetra;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.*;
import com.Inmobiliaria.demo.service.ComisionVendedorService;
import com.Inmobiliaria.demo.service.ReciboEgresoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComisionVendedorServiceImpl implements ComisionVendedorService {

    /** Cantidad de letras que el cliente debe pagar antes de habilitar pagos mensuales. */
    private static final int LETRAS_PREVIAS = 8;

    private final ComisionVendedorRepository comisionRepository;
    private final PagoComisionVendedorRepository pagoComisionRepository;
    private final LetraCambioRepository letraRepository;
    private final ContratoLoteRepository contratoLoteRepository;
    private final ReciboEgresoService reciboEgresoService;

    // ─── Redondeos ─────────────────────────────────────────────────────────────
    // Comisión total, 30% de la inicial y adelanto del programa: SIEMPRE hacia abajo (floor).
    private static BigDecimal floor(BigDecimal valor) {
        return valor.setScale(0, RoundingMode.FLOOR);
    }

    // Pago mensual (10% de la letra): redondeo estándar (22.50 → 23, 22.49 → 22).
    private static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(0, RoundingMode.HALF_UP);
    }

    private static BigDecimal porcentajeDe(BigDecimal base, BigDecimal porcentaje) {
        if (base == null) return BigDecimal.ZERO;
        if (porcentaje == null) return BigDecimal.ZERO;
        return base.multiply(porcentaje).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    // ─── Crear comisión al guardar contrato ────────────────────────────────────

    @Override
    @Transactional
    public ComisionVendedor crearComisionSiAplica(Contrato contrato) {
        if (contrato == null) return null;
        // Solo contratos financiados con vendedor y % de comisión > 0
        if (contrato.getVendedor() == null) return null;
        if (contrato.getTipoContrato() != com.Inmobiliaria.demo.enums.TipoContrato.FINANCIADO) return null;
        BigDecimal porcentaje = contrato.getVendedor().getComision();
        if (porcentaje == null || porcentaje.compareTo(BigDecimal.ZERO) <= 0) return null;
        if (comisionRepository.existsByContratoIdContrato(contrato.getIdContrato())) {
            return comisionRepository.findByContratoIdContrato(contrato.getIdContrato())
                    .orElse(null);
        }

        BigDecimal montoTotal = contrato.getMontoTotal() != null ? contrato.getMontoTotal() : BigDecimal.ZERO;
        BigDecimal montoComisionTotal = floor(porcentajeDe(montoTotal, porcentaje));

        ComisionVendedor comision = new ComisionVendedor();
        comision.setContrato(contrato);
        comision.setVendedor(contrato.getVendedor());
        comision.setPorcentajeComision(porcentaje);
        comision.setMontoTotalContrato(montoTotal);
        comision.setMontoComisionTotal(montoComisionTotal);
        comision.setMoneda(contrato.getMoneda() != null ? contrato.getMoneda() : Moneda.USD);
        comision.setMontoAdelanto(null);
        comision.setSaldoPendiente(montoComisionTotal);
        comision.setEstado(EstadoComision.PENDIENTE);

        ComisionVendedor guardada = comisionRepository.save(comision);
        log.info("Comisión creada para contrato {} ({}% → {})",
                contrato.getIdContrato(), porcentaje, montoComisionTotal);
        return guardada;
    }

    // ─── Listado (secretaría) ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ComisionVendedorDTO> listarComisiones() {
        List<ComisionVendedor> comisiones = comisionRepository.findAllByOrderByIdComisionDesc();
        List<ComisionVendedorDTO> result = new ArrayList<>();
        for (ComisionVendedor c : comisiones) {
            result.add(toDTO(c));
        }
        return result;
    }

    private ComisionVendedorDTO toDTO(ComisionVendedor c) {
        ComisionVendedorDTO dto = new ComisionVendedorDTO();
        dto.setIdComision(c.getIdComision());
        Contrato contrato = c.getContrato();
        dto.setIdContrato(contrato != null ? contrato.getIdContrato() : null);
        dto.setNombreVendedor(c.getVendedor() != null
                ? (c.getVendedor().getNombre() + " " + c.getVendedor().getApellidos()).trim()
                : "-");
        dto.setNombreCliente(primerClienteNombre(contrato));
        dto.setPrograma(nombrePrograma(contrato));
        List<String> mz = new ArrayList<>();
        List<String> lotes = new ArrayList<>();
        if (contrato != null) {
            List<Lote> lotesContrato = contratoLoteRepository.findLotesByContrato(contrato.getIdContrato());
            for (Lote l : lotesContrato) {
                if (l.getManzana() != null && !l.getManzana().isBlank()) mz.add(l.getManzana());
                if (l.getNumeroLote() != null && !l.getNumeroLote().isBlank()) lotes.add(l.getNumeroLote());
            }
        }
        dto.setManzanas(String.join(", ", mz));
        dto.setNumeroLotes(String.join(", ", lotes));
        dto.setPorcentajeComision(c.getPorcentajeComision());
        dto.setMontoTotalContrato(c.getMontoTotalContrato());
        dto.setMontoComisionTotal(c.getMontoComisionTotal());
        dto.setMoneda(c.getMoneda() != null ? c.getMoneda().name() : "USD");
        dto.setMontoAdelanto(c.getMontoAdelanto());
        dto.setSaldoPendiente(c.getSaldoPendiente());
        dto.setEstado(c.getEstado() != null ? c.getEstado().name() : "PENDIENTE");
        dto.setFechaCreacion(c.getFechaCreacion() != null ? c.getFechaCreacion().toLocalDate() : null);

        long pagadas = letrasPagadas(c.getContrato() != null ? c.getContrato().getIdContrato() : null);
        dto.setCantidadLetrasPagadas(pagadas);
        // El adelanto se habilita cuando la primera letra ya fue pagada y aún no se registró.
        dto.setAdelantoHabilitado(c.getMontoAdelanto() == null && pagadas >= 1);
        dto.setMontoAdelantoSugerido(calcularAdelantoSugerido(c));
        return dto;
    }

    private long letrasPagadas(Integer idContrato) {
        if (idContrato == null) return 0;
        return letraRepository.countByContratoIdContratoAndEstadoLetra(idContrato, EstadoLetra.PAGADO);
    }

    private String primerClienteNombre(Contrato contrato) {
        if (contrato == null || contrato.getClientes() == null || contrato.getClientes().isEmpty()) return "-";
        return contrato.getClientes().stream()
                .sorted((a, b) -> Integer.compare(
                        a.getOrden() != null ? a.getOrden() : 0,
                        b.getOrden() != null ? b.getOrden() : 0))
                .map(cc -> cc.getCliente())
                .filter(cl -> cl != null)
                .findFirst()
                .map(cl -> (cl.getNombre() + " " + cl.getApellidos()).trim())
                .orElse("-");
    }

    private String nombrePrograma(Contrato contrato) {
        if (contrato == null) return "-";
        List<Programa> programas = contratoLoteRepository.findProgramasByContrato(contrato.getIdContrato());
        return programas.stream().map(Programa::getNombrePrograma).distinct()
                .collect(Collectors.joining(", "));
    }

    /** Adelanto sugerido: 30% de la inicial, o adelanto del programa si no hubo inicial. */
    private BigDecimal calcularAdelantoSugerido(ComisionVendedor c) {
        Contrato contrato = c.getContrato();
        if (contrato == null) return BigDecimal.ZERO;
        BigDecimal inicial = contrato.getInicial() != null ? contrato.getInicial() : BigDecimal.ZERO;
        if (inicial.compareTo(BigDecimal.ZERO) > 0) {
            return floor(porcentajeDe(inicial, BigDecimal.valueOf(30)));
        }
        // Sin inicial → adelanto del programa (default $100)
        List<Programa> programas = contratoLoteRepository.findProgramasByContrato(contrato.getIdContrato());
        for (Programa p : programas) {
            if (p.getAdelantoVendedor() != null) {
                return floor(p.getAdelantoVendedor());
            }
        }
        return BigDecimal.valueOf(100);
    }

    // ─── Pagos mensuales habilitados ──────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PagoComisionMensualDTO> pagosMensualesHabilitados(Integer idComision) {
        ComisionVendedor comision = obtenerComision(idComision);
        Contrato contrato = comision.getContrato();
        if (contrato == null) throw new NegocioException("La comisión no tiene contrato asociado.");

        // Si el contrato está en estado terminal, ya no se habilitan pagos.
        if (contratoEstadoTerminal(contrato.getEstadoContrato())) {
            return new ArrayList<>();
        }
        // Los pagos mensuales solo aplican después de registrar el adelanto.
        if (comision.getMontoAdelanto() == null) {
            return new ArrayList<>();
        }

        // Las letras se pagan en secuencia estricta. Se habilitan las letras pagadas
        // cuya posición (orden de generación) supera las 8 previas.
        List<LetraCambio> letras = letraRepository
                .findByContratoIdContratoAndEstadoLetraOrderByIdLetraAsc(contrato.getIdContrato(), EstadoLetra.PAGADO);

        List<PagoComisionMensualDTO> result = new ArrayList<>();
        for (int i = LETRAS_PREVIAS; i < letras.size(); i++) {
            LetraCambio letra = letras.get(i);
            // Si ya se registró el pago mensual de esta letra, no se habilita.
            if (pagoComisionRepository.existsByLetraIdLetraAndTipo(letra.getIdLetra(), "MENSUAL")) continue;

            BigDecimal montoComision = redondear(porcentajeDe(letra.getImporte(), BigDecimal.valueOf(10)));
            BigDecimal saldo = comision.getSaldoPendiente() != null ? comision.getSaldoPendiente() : BigDecimal.ZERO;
            boolean ultimo = montoComision.compareTo(saldo) >= 0;

            PagoComisionMensualDTO dto = new PagoComisionMensualDTO();
            dto.setIdLetra(letra.getIdLetra());
            dto.setNumeroLetra(letra.getNumeroLetra());
            dto.setFechaVencimiento(letra.getFechaVencimiento());
            dto.setImporteLetra(letra.getImporte());
            dto.setMontoComision(montoComision);
            dto.setUltimoPago(ultimo);
            dto.setSeleccionado(false);
            result.add(dto);
        }
        return result;
    }

    // ─── Registrar adelanto ───────────────────────────────────────────────────

    @Override
    @Transactional
    public PagoComisionResultadoDTO registrarAdelanto(RegistrarAdelantoRequest request) {
        if (request == null || request.getIdComision() == null) {
            throw new NegocioException("Debe indicar la comisión.");
        }
        ComisionVendedor comision = obtenerComision(request.getIdComision());

        if (pagoComisionRepository.existsByComisionIdComisionAndTipo(comision.getIdComision(), "ADELANTO")) {
            throw new NegocioException("El adelanto de esta comisión ya fue registrado.");
        }
        Contrato contrato = comision.getContrato();
        if (contrato == null) throw new NegocioException("La comisión no tiene contrato asociado.");
        if (contratoEstadoTerminal(contrato.getEstadoContrato())) {
            throw new NegocioException("No se puede registrar el adelanto: el contrato está " + contrato.getEstadoContrato());
        }
        // Solo se habilita cuando la primera letra fue pagada.
        if (letrasPagadas(contrato.getIdContrato()) < 1) {
            throw new NegocioException("El adelanto se habilita cuando el cliente pague la primera letra.");
        }

        // Monto: si no viene, usa el sugerido (30% de la inicial o adelanto del programa).
        BigDecimal monto = request.getMonto() != null ? floor(request.getMonto()) : calcularAdelantoSugerido(comision);
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("El monto del adelanto debe ser mayor a 0.");
        }
        if (monto.compareTo(comision.getSaldoPendiente() != null ? comision.getSaldoPendiente() : BigDecimal.ZERO) > 0) {
            monto = comision.getSaldoPendiente();
        }

        ReciboEgreso egreso = reciboEgresoService.generarEgreso(
                "Pago de comisión al vendedor - Adelanto",
                comision.getVendedor() != null
                        ? (comision.getVendedor().getNombre() + " " + comision.getVendedor().getApellidos()).trim()
                        : "-",
                contrato.getIdContrato(),
                monto,
                comision.getMoneda().name());

        PagoComisionVendedor pago = new PagoComisionVendedor();
        pago.setComision(comision);
        pago.setTipo("ADELANTO");
        pago.setMonto(monto);
        pago.setFechaPago(LocalDate.now());
        pago.setNumeroEgreso(egreso.getNumeroCompleto());
        pago.setObservacion(request.getObservacion());
        pagoComisionRepository.save(pago);

        comision.setMontoAdelanto(monto);
        BigDecimal nuevoSaldo = comision.getSaldoPendiente().subtract(monto);
        comision.setSaldoPendiente(nuevoSaldo);
        comision.setEstado(nuevoSaldo.compareTo(BigDecimal.ZERO) <= 0
                ? EstadoComision.COMPLETADA : EstadoComision.EN_PAGO);
        comisionRepository.save(comision);

        log.info("Adelanto de comisión {} registrado: {} ({})", comision.getIdComision(), monto, egreso.getNumeroCompleto());
        return resultado(comision, List.of(egreso.getNumeroCompleto()));
    }

    // ─── Registrar pagos mensuales (multiselección) ───────────────────────────

    @Override
    @Transactional
    public PagoComisionResultadoDTO registrarPagosMensuales(RegistrarPagosMensualesRequest request) {
        if (request == null || request.getIdComision() == null) {
            throw new NegocioException("Debe indicar la comisión.");
        }
        if (request.getIdLetras() == null || request.getIdLetras().isEmpty()) {
            throw new NegocioException("Seleccione al menos un pago mensual.");
        }
        ComisionVendedor comision = obtenerComision(request.getIdComision());
        Contrato contrato = comision.getContrato();
        if (contrato == null) throw new NegocioException("La comisión no tiene contrato asociado.");
        if (contratoEstadoTerminal(contrato.getEstadoContrato())) {
            throw new NegocioException("No se pueden registrar pagos: el contrato está " + contrato.getEstadoContrato());
        }
        // El adelanto debe haberse registrado antes de los pagos mensuales.
        if (comision.getMontoAdelanto() == null) {
            throw new NegocioException("Primero debe registrarse el adelanto de la comisión.");
        }

        List<LetraCambio> letrasPagadas = letraRepository
                .findByContratoIdContratoAndEstadoLetraOrderByIdLetraAsc(contrato.getIdContrato(), EstadoLetra.PAGADO);
        // Conjunto de ids de letras habilitadas (posición > 8, sin pago previo).
        List<Integer> habilitadas = new ArrayList<>();
        for (int i = LETRAS_PREVIAS; i < letrasPagadas.size(); i++) {
            LetraCambio l = letrasPagadas.get(i);
            if (!pagoComisionRepository.existsByLetraIdLetraAndTipo(l.getIdLetra(), "MENSUAL")) {
                habilitadas.add(l.getIdLetra());
            }
        }

        BigDecimal saldo = comision.getSaldoPendiente() != null ? comision.getSaldoPendiente() : BigDecimal.ZERO;
        BigDecimal totalPagado = BigDecimal.ZERO;
        List<PagoComisionVendedor> pagos = new ArrayList<>();
        List<String> conceptos = new ArrayList<>();

        for (Integer idLetra : request.getIdLetras()) {
            if (!habilitadas.contains(idLetra)) {
                throw new NegocioException("La letra " + idLetra + " no tiene pago de comisión habilitado.");
            }
            LetraCambio letra = letrasPagadas.stream()
                    .filter(l -> l.getIdLetra().equals(idLetra)).findFirst().orElseThrow();
            BigDecimal monto = redondear(porcentajeDe(letra.getImporte(), BigDecimal.valueOf(10)));
            // El último pago se limita al saldo restante.
            if (monto.compareTo(saldo) > 0) monto = saldo;
            if (monto.compareTo(BigDecimal.ZERO) <= 0) continue;

            PagoComisionVendedor pago = new PagoComisionVendedor();
            pago.setComision(comision);
            pago.setLetra(letra);
            pago.setTipo("MENSUAL");
            pago.setMonto(monto);
            pago.setFechaPago(LocalDate.now());
            pago.setObservacion(request.getObservacion());
            pagos.add(pago);

            totalPagado = totalPagado.add(monto);
            saldo = saldo.subtract(monto);
            conceptos.add("Comisión vendedor - Letra " + letra.getNumeroLetra() + " (" + monto + ")");
        }

        if (pagos.isEmpty()) {
            throw new NegocioException("No hay montos que registrar (saldo pendiente en 0).");
        }

        ReciboEgreso egreso = reciboEgresoService.generarEgreso(
                String.join(" / ", conceptos),
                comision.getVendedor() != null
                        ? (comision.getVendedor().getNombre() + " " + comision.getVendedor().getApellidos()).trim()
                        : "-",
                contrato.getIdContrato(),
                totalPagado,
                comision.getMoneda().name());

        for (PagoComisionVendedor p : pagos) {
            p.setNumeroEgreso(egreso.getNumeroCompleto());
            pagoComisionRepository.save(p);
        }

        comision.setSaldoPendiente(saldo);
        comision.setEstado(saldo.compareTo(BigDecimal.ZERO) <= 0
                ? EstadoComision.COMPLETADA : EstadoComision.EN_PAGO);
        comisionRepository.save(comision);

        log.info("Pagos mensuales de comisión {} registrados: {} letras, total {} ({})",
                comision.getIdComision(), pagos.size(), totalPagado, egreso.getNumeroCompleto());
        return resultado(comision, List.of(egreso.getNumeroCompleto()));
    }

    // ─── Anular comisión cuando el contrato se renuncia/resuelve ──────────────

    @Override
    @Transactional
    public void anularComisionSiExiste(Integer idContrato) {
        if (idContrato == null) return;
        comisionRepository.findByContratoIdContrato(idContrato).ifPresent(c -> {
            if (c.getEstado() != EstadoComision.COMPLETADA) {
                c.setEstado(EstadoComision.ANULADA);
                comisionRepository.save(c);
                log.info("Comisión {} ANULADA por contrato {}", c.getIdComision(), idContrato);
            }
        });
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ComisionVendedor obtenerComision(Integer idComision) {
        return comisionRepository.findById(idComision)
                .orElseThrow(() -> new NegocioException("Comisión no encontrada: " + idComision));
    }

    private boolean contratoEstadoTerminal(EstadoContrato estado) {
        if (estado == null) return false;
        return estado == EstadoContrato.RENUNCIA
                || estado == EstadoContrato.RESUELTO
                || estado == EstadoContrato.TRANSFERIDO
                || estado == EstadoContrato.CANCELADO;
    }

    private PagoComisionResultadoDTO resultado(ComisionVendedor comision, List<String> numerosEgreso) {
        PagoComisionResultadoDTO dto = new PagoComisionResultadoDTO();
        dto.setNumerosEgreso(numerosEgreso);
        dto.setIdComision(comision.getIdComision());
        dto.setSaldoPendiente(comision.getSaldoPendiente());
        dto.setEstado(comision.getEstado().name());
        dto.setFechaPago(LocalDate.now());
        return dto;
    }
}