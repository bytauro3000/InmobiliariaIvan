package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.dto.*;
import com.Inmobiliaria.demo.entity.*;
import com.Inmobiliaria.demo.enums.EstadoComision;
import com.Inmobiliaria.demo.enums.EstadoContrato;
import com.Inmobiliaria.demo.enums.EstadoLetra;
import com.Inmobiliaria.demo.enums.MedioPago;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.*;
import com.Inmobiliaria.demo.service.ComisionVendedorService;
import com.Inmobiliaria.demo.service.ReciboEgresoService;
import com.Inmobiliaria.demo.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComisionVendedorServiceImpl implements ComisionVendedorService {

    /** Cantidad de letras previas que NO generan comisión mensual (la letra 8 es la primera). */
    private static final int LETRAS_PREVIAS = 7;

    /**
     * Extrae el número de una letra del formato "N/120" (o "8").
     * Ej: "8/120" → 8. Si no es numérico, devuelve 0.
     */
    private static int extraerNumeroLetra(String numeroLetra) {
        if (numeroLetra == null || numeroLetra.isBlank()) return 0;
        String parte = numeroLetra.split("/")[0].trim();
        try {
            return Integer.parseInt(parte);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private final ComisionVendedorRepository comisionRepository;
    private final PagoComisionVendedorRepository pagoComisionRepository;
    private final LetraCambioRepository letraRepository;
    private final ContratoLoteRepository contratoLoteRepository;
    private final ContratoClienteRepository contratoClienteRepository;
    private final ContratoRepository contratoRepository;
    private final VoucherRepository voucherRepository;
    private final PagoInicialRepository pagoInicialRepository;
    private final UsuarioService usuarioService;
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

    /**
     * ¿Es el vendedor la propia inmobiliaria? (INMOBILIARIA CONSTRUCTORA IVAN).
     * Cuando la empresa vende directamente no hay comisión que pagar, por lo que
     * esos contratos no generan comisión ni figuran en la lista.
     */
    private boolean esVendedorLaPropiaInmobiliaria(Vendedor v) {
        if (v == null) return false;
        String nombreCompleto = ((v.getNombre() == null ? "" : v.getNombre())
                + " " + (v.getApellidos() == null ? "" : v.getApellidos())).toUpperCase();
        return nombreCompleto.contains("INMOBILIARIA") && nombreCompleto.contains("IVAN");
    }

    @Override
    @Transactional
    public ComisionVendedor crearComisionSiAplica(Contrato contrato) {
        if (contrato == null) return null;
        // La comisión se genera si el contrato es FINANCIADO o CONTADO con vendedor
        // y % de comisión > 0. Si el contrato está RENUNCIADO/RESUELTO/TRANSFERIDO,
        // NO se genera comisión.
        if (contrato.getVendedor() == null) return null;
        // La propia inmobiliaria como vendedor NO genera comisión (vende directamente).
        if (esVendedorLaPropiaInmobiliaria(contrato.getVendedor())) return null;
        var tipo = contrato.getTipoContrato();
        if (tipo != com.Inmobiliaria.demo.enums.TipoContrato.FINANCIADO
                && tipo != com.Inmobiliaria.demo.enums.TipoContrato.CONTADO) return null;
        if (contratoEstadoTerminal(contrato.getEstadoContrato())) return null;
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

    // ─── Migración (backfill) de contratos existentes ─────────────────────────

    @Override
    @Transactional
    public Map<String, Object> migrarComisiones() {
        List<Contrato> elegibles = contratoRepository.findContratosElegiblesParaComision();
        int creadas = 0;
        int omitidasRenuncia = 0;
        int omitidasVendedor = 0;
        List<Integer> creadasIds = new ArrayList<>();

        for (Contrato c : elegibles) {
            // Exclusión explícita por estado (renuncia/resuelto/transferido)
            if (contratoEstadoTerminal(c.getEstadoContrato())) {
                omitidasRenuncia++;
                continue;
            }
            if (c.getVendedor() == null
                    || c.getVendedor().getComision() == null
                    || c.getVendedor().getComision().compareTo(BigDecimal.ZERO) <= 0) {
                omitidasVendedor++;
                continue;
            }
            if (comisionRepository.existsByContratoIdContrato(c.getIdContrato())) continue;

            ComisionVendedor nueva = crearComisionSiAplica(c);
            if (nueva != null) {
                creadas++;
                creadasIds.add(nueva.getIdComision());
            }
        }

        log.info("Migración de comisiones: {} creadas (renuncia/resuelto: {} omitidas, sin vendedor válido: {} omitidas)",
                creadas, omitidasRenuncia, omitidasVendedor);
        return Map.of(
                "creadas", creadas,
                "omitidasRenuncia", omitidasRenuncia,
                "omitidasSinVendedor", omitidasVendedor,
                "idsComisionesCreadas", creadasIds);
    }

    // ─── Listado (secretaría) ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ComisionVendedorDTO> listarComisiones() {
        List<ComisionVendedor> comisiones = comisionRepository
                .findAllByOrderByContratoFechaContratoDescIdComisionDesc();
        if (comisiones.isEmpty()) return new ArrayList<>();

        // ── Pre-cargar en batch (evita N+1: antes hacía ~4 queries por comisión) ──
        List<Integer> idContratos = comisiones.stream()
                .map(c -> c.getContrato() != null ? c.getContrato().getIdContrato() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // Letras pagadas por contrato
        Map<Integer, Long> letrasPagadasPorContrato = new HashMap<>();
        for (Object[] fila : letraRepository.countByContratosAndEstadoLetra(idContratos, EstadoLetra.PAGADO)) {
            letrasPagadasPorContrato.put((Integer) fila[0], (Long) fila[1]);
        }

        // Número máximo de letra PAGADO por contrato. Se asume secuencia: si la letra
        // más alta pagada es la 9, las 1-8 también están pagadas (recibos físicos).
        Map<Integer, Long> maxNumeroPorContrato = new HashMap<>();
        for (Object[] fila : letraRepository.maxNumeroLetraPagadaPorContratos(idContratos)) {
            maxNumeroPorContrato.put(((Number) fila[0]).intValue(), ((Number) fila[1]).longValue());
        }

        // Lotes (manzana / número) por contrato
        Map<Integer, List<String>> manzanasPorContrato = new HashMap<>();
        Map<Integer, List<String>> numerosLotePorContrato = new HashMap<>();
        for (Object[] fila : contratoLoteRepository.findLotesByContratos(idContratos)) {
            Integer idContrato = (Integer) fila[0];
            String manzana = (String) fila[1];
            String numeroLote = (String) fila[2];
            manzanasPorContrato.computeIfAbsent(idContrato, k -> new ArrayList<>());
            numerosLotePorContrato.computeIfAbsent(idContrato, k -> new ArrayList<>());
            if (manzana != null && !manzana.isBlank()) manzanasPorContrato.get(idContrato).add(manzana);
            if (numeroLote != null && !numeroLote.isBlank()) numerosLotePorContrato.get(idContrato).add(numeroLote);
        }

        // Programa(s) por contrato
        Map<Integer, List<Programa>> programasPorContrato = new HashMap<>();
        for (Object[] fila : contratoLoteRepository.findProgramasByContratos(idContratos)) {
            Integer idContrato = (Integer) fila[0];
            if (fila[1] instanceof Programa programa) {
                programasPorContrato.computeIfAbsent(idContrato, k -> new ArrayList<>()).add(programa);
            }
        }

        // Clientes (ordenados) por contrato — para el nombre del titular
        Map<Integer, List<ContratoCliente>> clientesPorContrato = new HashMap<>();
        for (ContratoCliente cc : contratoClienteRepository
                .findByContratoIdContratoInOrderByOrdenAsc(idContratos)) {
            clientesPorContrato.computeIfAbsent(cc.getContrato().getIdContrato(), k -> new ArrayList<>()).add(cc);
        }

        // Pagos mensuales de comisión registrados por comisión (batch)
        List<Integer> idsComisiones = comisiones.stream()
                .map(ComisionVendedor::getIdComision).collect(Collectors.toList());
        Map<Integer, Long> mensualesRegistradosPorComision = new HashMap<>();
        for (Object[] fila : pagoComisionRepository.countByComisionesAndTipo(idsComisiones, "MENSUAL")) {
            mensualesRegistradosPorComision.put((Integer) fila[0], (Long) fila[1]);
        }

        // Contratos con inicial pagada (batch) — habilita adelanto para financiados sin letras pagadas
        java.util.Set<Integer> contratosConInicial = new java.util.HashSet<>(
                pagoInicialRepository.findContratosConInicialPagada(idContratos));

        List<ComisionVendedorDTO> result = new ArrayList<>(comisiones.size());
        for (ComisionVendedor c : comisiones) {
            result.add(toDTO(c, letrasPagadasPorContrato, maxNumeroPorContrato,
                    manzanasPorContrato, numerosLotePorContrato, programasPorContrato,
                    clientesPorContrato, mensualesRegistradosPorComision, contratosConInicial));
        }
        return result;
    }

    private ComisionVendedorDTO toDTO(
            ComisionVendedor c,
            Map<Integer, Long> letrasPagadasPorContrato,
            Map<Integer, Long> maxNumeroPorContrato,
            Map<Integer, List<String>> manzanasPorContrato,
            Map<Integer, List<String>> numerosLotePorContrato,
            Map<Integer, List<Programa>> programasPorContrato,
            Map<Integer, List<ContratoCliente>> clientesPorContrato,
            Map<Integer, Long> mensualesRegistradosPorComision,
            java.util.Set<Integer> contratosConInicial) {

        ComisionVendedorDTO dto = new ComisionVendedorDTO();
        dto.setIdComision(c.getIdComision());
        Contrato contrato = c.getContrato();
        Integer idContrato = contrato != null ? contrato.getIdContrato() : null;
        dto.setIdContrato(idContrato);
        dto.setIdVendedor(c.getVendedor() != null ? c.getVendedor().getIdVendedor() : null);
        dto.setNombreVendedor(c.getVendedor() != null
                ? (c.getVendedor().getNombre() + " " + c.getVendedor().getApellidos()).trim()
                : "-");

        if (idContrato != null) {
            dto.setNombreCliente(primerClienteNombre(clientesPorContrato.get(idContrato)));
            dto.setPrograma(nombrePrograma(programasPorContrato.get(idContrato)));
            List<String> mz = manzanasPorContrato.getOrDefault(idContrato, List.of());
            List<String> lotes = numerosLotePorContrato.getOrDefault(idContrato, List.of());
            dto.setManzanas(String.join(", ", mz));
            dto.setNumeroLotes(String.join(", ", lotes));
        } else {
            dto.setNombreCliente("-");
            dto.setPrograma("-");
            dto.setManzanas("");
            dto.setNumeroLotes("");
        }

        dto.setPorcentajeComision(c.getPorcentajeComision());
        dto.setMontoTotalContrato(c.getMontoTotalContrato());
        dto.setMontoComisionTotal(c.getMontoComisionTotal());
        dto.setMoneda(c.getMoneda() != null ? c.getMoneda().name() : "USD");
        dto.setMontoAdelanto(c.getMontoAdelanto());
        dto.setSaldoPendiente(c.getSaldoPendiente());
        dto.setEstado(c.getEstado() != null ? c.getEstado().name() : "PENDIENTE");
        dto.setFechaCreacion(c.getFechaCreacion() != null ? c.getFechaCreacion().toLocalDate() : null);
        dto.setFechaContrato(contrato != null && contrato.getFechaContrato() != null
                ? contrato.getFechaContrato() : null);

        long pagadas = idContrato != null
                ? letrasPagadasPorContrato.getOrDefault(idContrato, 0L) : 0L;
        dto.setCantidadLetrasPagadas(pagadas);

        // Pagos mensuales pendientes = letras habilitables (número > 7 → letra 8+) −
        // mensuales registrados. Se cuenta por NÚMERO de letra, no por posición.
        // Si la comisión está COMPLETADA (cancelada en su totalidad), no hay pendientes.
        boolean completada = c.getEstado() == EstadoComision.COMPLETADA
                || (c.getSaldoPendiente() != null
                    && c.getSaldoPendiente().compareTo(BigDecimal.ZERO) <= 0);
        long registrados = mensualesRegistradosPorComision.getOrDefault(c.getIdComision(), 0L);
        long maxNumero = idContrato != null
                ? maxNumeroPorContrato.getOrDefault(idContrato, 0L) : 0L;
        long habilitables = Math.max(0L, maxNumero - LETRAS_PREVIAS);
        long pendientes = completada ? 0L : Math.max(0L, habilitables - registrados);
        dto.setPagosMensualesRegistrados(registrados);
        dto.setPagosMensualesPendientes(pendientes);
        dto.setNivelColor(nivelColor(pendientes));

        boolean esContado = contrato != null
                && contrato.getTipoContrato() == com.Inmobiliaria.demo.enums.TipoContrato.CONTADO;
        if (esContado) {
            // Al contado: el cliente ya pagó todo. El adelanto se habilita de inmediato
            // (no hay letras que esperar) y sugiere la comisión total.
            dto.setAdelantoHabilitado(c.getMontoAdelanto() == null);
            dto.setMontoAdelantoSugerido(c.getMontoComisionTotal());
        } else {
            // Financiado: el adelanto se habilita cuando:
            // 1) La primera letra ya fue pagada, O
            // 2) El cliente pagó la inicial (la empresa le debe el 1er pago al vendedor).
            boolean inicialPagada = idContrato != null && contratosConInicial.contains(idContrato);
            dto.setAdelantoHabilitado(c.getMontoAdelanto() == null && (pagadas >= 1 || inicialPagada));
            dto.setMontoAdelantoSugerido(calcularAdelantoSugerido(
                    c, programasPorContrato.get(idContrato)));
        }
        return dto;
    }

    /** Nivel de color según pagos de comisión pendientes: 0=VERDE, 1-2=NARANJA, 3+=ROJO. */
    private String nivelColor(long pendientes) {
        if (pendientes >= 3) return "ROJO";
        if (pendientes >= 1) return "NARANJA";
        return "VERDE";
    }

    private long letrasPagadas(Integer idContrato) {
        if (idContrato == null) return 0;
        return letraRepository.countByContratoIdContratoAndEstadoLetra(idContrato, EstadoLetra.PAGADO);
    }

    private String primerClienteNombre(List<ContratoCliente> clientes) {
        if (clientes == null || clientes.isEmpty()) return "-";
        return clientes.stream()
                .map(ContratoCliente::getCliente)
                .filter(cl -> cl != null)
                .findFirst()
                .map(cl -> (cl.getNombre() + " " + cl.getApellidos()).trim())
                .orElse("-");
    }

    private String nombrePrograma(List<Programa> programas) {
        if (programas == null || programas.isEmpty()) return "-";
        return programas.stream().map(Programa::getNombrePrograma).distinct()
                .collect(Collectors.joining(", "));
    }

    /** Adelanto sugerido: siempre el adelanto del programa (default $100), aunque el
     *  cliente pague inicial. NO se usa el 30% de la inicial (regla corregida). */
    private BigDecimal calcularAdelantoSugerido(ComisionVendedor c) {
        Contrato contrato = c.getContrato();
        List<Programa> programas = contrato != null
                ? contratoLoteRepository.findProgramasByContrato(contrato.getIdContrato())
                : List.of();
        return calcularAdelantoSugerido(c, programas);
    }

    /** Adelanto sugerido: siempre el adelanto del programa (default $100). */
    private BigDecimal calcularAdelantoSugerido(ComisionVendedor c, List<Programa> programas) {
        if (programas != null) {
            for (Programa p : programas) {
                if (p.getAdelantoVendedor() != null) {
                    return floor(p.getAdelantoVendedor());
                }
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
        // Comisión completada (cancelada en su totalidad): ya no hay pagos mensuales
        // habilitados, aunque haya letras pagadas sin pago MENSUAL individual (histórico).
        if (comision.getEstado() == EstadoComision.COMPLETADA
                || (comision.getSaldoPendiente() != null
                    && comision.getSaldoPendiente().compareTo(BigDecimal.ZERO) <= 0)) {
            return new ArrayList<>();
        }
        // Los pagos mensuales solo aplican después de registrar el adelanto.
        if (comision.getMontoAdelanto() == null) {
            return new ArrayList<>();
        }

        // Las letras se pagan en secuencia estricta. Se habilitan las letras PAGADO cuyo
        // NÚMERO (antes de '/') supera las 7 previas (letra 8 en adelante). Se usa el
        // número, NO la posición: si solo están registradas las letras 8 y 9, se asume
        // que las 1-7 también están pagadas (recibos físicos aún no pasados al sistema).
        List<LetraCambio> letras = letraRepository
                .findByContratoIdContratoAndEstadoLetraOrderByIdLetraAsc(contrato.getIdContrato(), EstadoLetra.PAGADO);

        List<PagoComisionMensualDTO> result = new ArrayList<>();
        for (LetraCambio letra : letras) {
            int numero = extraerNumeroLetra(letra.getNumeroLetra());
            // Solo generan comisión las letras posteriores a las 7 previas (letra 8+).
            if (numero <= LETRAS_PREVIAS) continue;
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
        // Solo se habilita cuando la primera letra fue pagada O el cliente pagó la inicial.
        // En CONTADO el cliente ya pagó todo al firmar, no hay letras que esperar.
        boolean esContado = contrato.getTipoContrato() == com.Inmobiliaria.demo.enums.TipoContrato.CONTADO;
        if (!esContado && letrasPagadas(contrato.getIdContrato()) < 1
                && !pagoInicialRepository.findByContratoIdContrato(contrato.getIdContrato()).isPresent()) {
            throw new NegocioException("El adelanto se habilita cuando el cliente pague la inicial o la primera letra.");
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

    // ─── Actualizar monto de comisión acordado (negociación) ──────────────────

    @Override
    @Transactional
    public ComisionVendedorDTO actualizarMontoComision(ActualizarMontoComisionRequest request) {
        if (request == null || request.getIdComision() == null) {
            throw new NegocioException("Debe indicar la comisión.");
        }
        if (request.getMonto() == null || request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("El monto acordado debe ser mayor a 0.");
        }
        ComisionVendedor comision = obtenerComision(request.getIdComision());
        Contrato contrato = comision.getContrato();
        if (contrato == null) throw new NegocioException("La comisión no tiene contrato asociado.");
        if (contratoEstadoTerminal(contrato.getEstadoContrato())) {
            throw new NegocioException("No se puede editar el monto: el contrato está " + contrato.getEstadoContrato());
        }

        // Solo editable mientras NO haya pagos registrados (adelanto ni mensuales).
        boolean tienePagos = pagoComisionRepository
                .findByComisionIdComisionOrderByIdPagoComisionAsc(comision.getIdComision()).stream()
                .anyMatch(p -> p.getTipo() != null);
        if (tienePagos) {
            throw new NegocioException("No se puede editar el monto: ya existen pagos registrados para esta comisión.");
        }

        // El monto acordado puede superar el % del vendedor si el gerente lo acepta
        // (el frontend muestra una confirmación SweetAlert). Sin tope duro.
        BigDecimal montoTotal = contrato.getMontoTotal() != null ? contrato.getMontoTotal() : BigDecimal.ZERO;
        BigDecimal montoAcordado = floor(request.getMonto());

        // Recalcular el % efectivo (monto acordado respecto al monto total).
        BigDecimal porcentajeEfectivo = montoTotal.compareTo(BigDecimal.ZERO) > 0
                ? montoAcordado.multiply(BigDecimal.valueOf(100))
                        .divide(montoTotal, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        comision.setMontoComisionTotal(montoAcordado);
        comision.setPorcentajeComision(porcentajeEfectivo);
        comision.setSaldoPendiente(montoAcordado);
        comision.setMontoAdelanto(null);
        comision.setEstado(EstadoComision.PENDIENTE);
        comisionRepository.save(comision);

        log.info("Comisión {}: monto acordado actualizado a {}", comision.getIdComision(), montoAcordado);

        // Devuelve el DTO actualizado (usa listado por id único).
        return listarComisiones().stream()
                .filter(d -> d.getIdComision().equals(comision.getIdComision()))
                .findFirst()
                .orElse(null);
    }

    // ─── Registrar pago de comisión (adelanto o mensual multi-lote) con vouchers ─

    @Override
    @Transactional
    public PagoComisionResponseDTO registrarPagoComision(PagoComisionRequestDTO request,
                                                         List<MultipartFile> vouchers) {
        if (request == null || request.getTipo() == null) {
            throw new NegocioException("Debe indicar el tipo de pago (ADELANTO o MENSUAL).");
        }

        LocalDate fechaPago = request.getFechaPago() != null ? request.getFechaPago() : LocalDate.now();
        boolean esBancario = request.getMedioPago() != null
                && request.getMedioPago() != com.Inmobiliaria.demo.enums.MedioPago.EFECTIVO;
        if (esBancario) {
            if (request.getNumeroOperacion() == null || request.getNumeroOperacion().isBlank()) {
                throw new NegocioException("Para medios bancarios debe indicar el N° de operación.");
            }
            if (request.getFechaOperacion() == null) {
                throw new NegocioException("Para medios bancarios debe indicar la fecha de operación.");
            }
        }

        if ("ADELANTO".equalsIgnoreCase(request.getTipo())) {
            return registrarAdelantoConDetalle(request, fechaPago, vouchers);
        }
        if ("MENSUAL".equalsIgnoreCase(request.getTipo())) {
            return registrarMensualConDetalle(request, fechaPago, vouchers);
        }
        throw new NegocioException("Tipo de pago inválido: " + request.getTipo());
    }

    /** Adelanto: valida reglas, genera UN egreso EG01 y guarda vouchers. */
    private PagoComisionResponseDTO registrarAdelantoConDetalle(
            PagoComisionRequestDTO request, LocalDate fechaPago, List<MultipartFile> vouchers) {

        if (request.getIdComision() == null) {
            throw new NegocioException("Debe indicar la comisión para el adelanto.");
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
        boolean esContado = contrato.getTipoContrato() == com.Inmobiliaria.demo.enums.TipoContrato.CONTADO;
        if (!esContado && letrasPagadas(contrato.getIdContrato()) < 1
                && !pagoInicialRepository.findByContratoIdContrato(contrato.getIdContrato()).isPresent()) {
            throw new NegocioException("El adelanto se habilita cuando el cliente pague la inicial o la primera letra.");
        }

        BigDecimal monto = request.getMonto() != null ? floor(request.getMonto()) : calcularAdelantoSugerido(comision);
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("El monto del adelanto debe ser mayor a 0.");
        }
        BigDecimal saldoActual = comision.getSaldoPendiente() != null ? comision.getSaldoPendiente() : BigDecimal.ZERO;
        if (monto.compareTo(saldoActual) > 0) monto = saldoActual;

        String beneficiario = comision.getVendedor() != null
                ? (comision.getVendedor().getNombre() + " " + comision.getVendedor().getApellidos()).trim() : "-";
        // Concepto con detalle del lote (igual que las observaciones por defecto)
        String mz = comision.getContrato() != null && !manzanasPorContratoLocal(comision).isEmpty()
                ? String.join(",", manzanasPorContratoLocal(comision)) : "—";
        String lt = comision.getContrato() != null && !lotesPorContratoLocal(comision).isEmpty()
                ? String.join(",", lotesPorContratoLocal(comision)) : "—";
        String programa = contrato != null ? nombrePrograma(
                contratoLoteRepository.findProgramasByContrato(contrato.getIdContrato())) : "—";
        String concepto = "Pago de la 1ra cuota de comisión de la MZ: " + mz
                + " LT: " + lt + " y programa: " + programa
                + ", Saldo: " + (comision.getMoneda().name().equals("PEN") ? "S/" : "$")
                + " " + saldoActual.subtract(monto).toPlainString();

        String dniVendedor = comision.getVendedor() != null ? comision.getVendedor().getDni() : null;
        ReciboEgreso egreso = reciboEgresoService.generarEgresoConVouchers(
                concepto, beneficiario, dniVendedor, obtenerUsuarioRegistro(),
                contrato.getIdContrato(), monto,
                comision.getMoneda().name(), request.getMedioPago(),
                request.getNumeroOperacion(), request.getFechaOperacion(), vouchers);

        PagoComisionVendedor pago = new PagoComisionVendedor();
        pago.setComision(comision);
        pago.setTipo("ADELANTO");
        pago.setMonto(monto);
        pago.setFechaPago(fechaPago);
        pago.setFechaOperacion(request.getFechaOperacion());
        pago.setMedioPago(request.getMedioPago());
        pago.setNumeroOperacion(request.getNumeroOperacion());
        pago.setNumeroEgreso(egreso.getNumeroCompleto());
        pago.setObservacion(request.getObservacion());
        pagoComisionRepository.save(pago);

        comision.setMontoAdelanto(monto);
        BigDecimal nuevoSaldo = saldoActual.subtract(monto);
        comision.setSaldoPendiente(nuevoSaldo);
        comision.setEstado(nuevoSaldo.compareTo(BigDecimal.ZERO) <= 0
                ? EstadoComision.COMPLETADA : EstadoComision.EN_PAGO);
        comisionRepository.save(comision);

        log.info("Adelanto de comisión {} registrado: {} ({})",
                comision.getIdComision(), monto, egreso.getNumeroCompleto());

        PagoComisionResponseDTO resp = new PagoComisionResponseDTO();
        resp.setNumerosEgreso(List.of(egreso.getNumeroCompleto()));
        resp.setIdsComision(List.of(comision.getIdComision()));
        resp.setSaldoPendiente(comision.getSaldoPendiente());
        resp.setEstado(comision.getEstado().name());
        resp.setFechaPago(fechaPago);
        resp.setUrlsVoucher(extraerUrlsVouchers(egreso));
        resp.setConceptoDetalle(concepto);
        return resp;
    }

    /**
     * Mensual multi-lote: las letras seleccionadas pueden cruzar varios contratos.
     * Valida las reglas por contrato y genera UN solo egreso EG01 cuyo concepto
     * lista cada lote (programa · MZ · LT · monto) y el total.
     */
    private PagoComisionResponseDTO registrarMensualConDetalle(
            PagoComisionRequestDTO request, LocalDate fechaPago, List<MultipartFile> vouchers) {

        if (request.getIdLetras() == null || request.getIdLetras().isEmpty()) {
            throw new NegocioException("Seleccione al menos un pago mensual.");
        }
        List<LetraCambio> letrasSeleccionadas = letraRepository.findAllById(request.getIdLetras());
        if (letrasSeleccionadas.isEmpty()) {
            throw new NegocioException("Las letras seleccionadas no existen.");
        }

        // Validar cada letra y agrupar los pagos por comisión (contrato).
        List<PagoComisionVendedor> pagos = new ArrayList<>();
        Map<Integer, ComisionVendedor> comisionesAfectadas = new HashMap<>();
        List<String> lineasDetalle = new ArrayList<>();
        BigDecimal totalPagado = BigDecimal.ZERO;

        for (LetraCambio letra : letrasSeleccionadas) {
            Contrato contrato = letra.getContrato();
            if (contrato == null) throw new NegocioException("La letra " + letra.getIdLetra() + " no tiene contrato.");
            if (contratoEstadoTerminal(contrato.getEstadoContrato())) {
                throw new NegocioException("No se pueden registrar pagos: el contrato está " + contrato.getEstadoContrato());
            }
            if (letra.getEstadoLetra() != EstadoLetra.PAGADO) {
                throw new NegocioException("La letra " + letra.getNumeroLetra() + " no está pagada por el cliente.");
            }
            ComisionVendedor comision = comisionRepository.findByContratoIdContrato(contrato.getIdContrato())
                    .orElseThrow(() -> new NegocioException(
                            "El contrato " + contrato.getIdContrato() + " no tiene comisión registrada."));
            if (comision.getMontoAdelanto() == null) {
                throw new NegocioException(
                        "Primero debe registrarse el adelanto de la comisión del contrato " + contrato.getIdContrato() + ".");
            }
            if (pagoComisionRepository.existsByLetraIdLetraAndTipo(letra.getIdLetra(), "MENSUAL")) {
                throw new NegocioException("La letra " + letra.getNumeroLetra() + " ya tiene pago de comisión registrado.");
            }

            // Verificar el número de la letra (debe superar las 7 previas → letra 8 en adelante).
            // Se usa el número, NO la posición: las letras 1-7 pueden estar en recibos
            // físicos aún no pasados al sistema, pero se asumen pagadas por secuencia.
            int numero = extraerNumeroLetra(letra.getNumeroLetra());
            if (numero <= LETRAS_PREVIAS) {
                throw new NegocioException(
                        "La letra " + letra.getNumeroLetra() + " no habilita pago de comisión (debe ser la letra 8 en adelante).");
            }

            BigDecimal montoComision = redondear(porcentajeDe(letra.getImporte(), BigDecimal.valueOf(10)));
            BigDecimal saldo = comision.getSaldoPendiente() != null ? comision.getSaldoPendiente() : BigDecimal.ZERO;
            if (montoComision.compareTo(saldo) > 0) montoComision = saldo;
            if (montoComision.compareTo(BigDecimal.ZERO) <= 0) continue;

            PagoComisionVendedor pago = new PagoComisionVendedor();
            pago.setComision(comision);
            pago.setLetra(letra);
            pago.setTipo("MENSUAL");
            pago.setMonto(montoComision);
            pago.setFechaPago(fechaPago);
            pago.setFechaOperacion(request.getFechaOperacion());
            pago.setMedioPago(request.getMedioPago());
            pago.setNumeroOperacion(request.getNumeroOperacion());
            pago.setObservacion(request.getObservacion());
            pagos.add(pago);

            comisionesAfectadas.put(comision.getIdComision(), comision);
            totalPagado = totalPagado.add(montoComision);
            lineasDetalle.add(detalleLote(contrato, letra) + "  " + montoComision.toPlainString());
        }

        if (pagos.isEmpty()) {
            throw new NegocioException("No hay montos que registrar (saldo pendiente en 0).");
        }

        // Beneficiario: si hay un solo vendedor se usa su nombre; si son varios, se indica "Vendedores".
        String beneficiario;
        List<ComisionVendedor> comisList = new ArrayList<>(comisionesAfectadas.values());
        if (comisList.size() == 1 && comisList.get(0).getVendedor() != null) {
            Vendedor v = comisList.get(0).getVendedor();
            beneficiario = (v.getNombre() + " " + v.getApellidos()).trim();
        } else {
            beneficiario = "VENDEDORES (comisiones)";
        }

        lineasDetalle.add("TOTAL: " + totalPagado.toPlainString());
        String concepto = String.join("\n", lineasDetalle);

        // Un solo egreso EG01 para todo el lote seleccionado.
        String dniVendedor = (comisList.size() == 1 && comisList.get(0).getVendedor() != null)
                ? comisList.get(0).getVendedor().getDni() : null;
        ReciboEgreso egreso = reciboEgresoService.generarEgresoConVouchers(
                concepto, beneficiario, dniVendedor, obtenerUsuarioRegistro(),
                null, totalPagado,
                comisList.get(0).getMoneda().name(), request.getMedioPago(),
                request.getNumeroOperacion(), request.getFechaOperacion(), vouchers);

        String numeroEgreso = egreso.getNumeroCompleto();
        for (PagoComisionVendedor p : pagos) {
            p.setNumeroEgreso(numeroEgreso);
            pagoComisionRepository.save(p);
            ComisionVendedor c = comisionesAfectadas.get(p.getComision().getIdComision());
            if (c != null) {
                BigDecimal ns = c.getSaldoPendiente().subtract(p.getMonto());
                c.setSaldoPendiente(ns);
                c.setEstado(ns.compareTo(BigDecimal.ZERO) <= 0
                        ? EstadoComision.COMPLETADA : EstadoComision.EN_PAGO);
            }
        }
        comisionRepository.saveAll(comisionesAfectadas.values());

        log.info("Pagos mensuales de comisión registrados: {} letras, total {} ({})",
                pagos.size(), totalPagado, numeroEgreso);

        PagoComisionResponseDTO resp = new PagoComisionResponseDTO();
        resp.setNumerosEgreso(List.of(numeroEgreso));
        resp.setIdsComision(new ArrayList<>(comisionesAfectadas.keySet()));
        resp.setSaldoPendiente(null); // multi-lote
        resp.setEstado("OK");
        resp.setFechaPago(fechaPago);
        resp.setUrlsVoucher(extraerUrlsVouchers(egreso));
        resp.setConceptoDetalle(concepto);
        return resp;
    }

    /** Línea de detalle del lote para el concepto del egreso. */
    private String detalleLote(Contrato contrato, LetraCambio letra) {
        List<Programa> programas = contratoLoteRepository.findProgramasByContrato(contrato.getIdContrato());
        String programa = nombrePrograma(programas);
        List<String> mz = new ArrayList<>();
        List<String> lotes = new ArrayList<>();
        List<Lote> lotesContrato = contratoLoteRepository.findLotesByContrato(contrato.getIdContrato());
        for (Lote l : lotesContrato) {
            if (l.getManzana() != null && !l.getManzana().isBlank()) mz.add(l.getManzana());
            if (l.getNumeroLote() != null && !l.getNumeroLote().isBlank()) lotes.add(l.getNumeroLote());
        }
        return "Pago de comisión MZ " + String.join(",", mz)
                + " · LT " + String.join(",", lotes)
                + " · " + programa + " · Letra " + letra.getNumeroLetra();
    }

    private List<String> manzanasPorContratoLocal(ComisionVendedor c) {
        List<String> mz = new ArrayList<>();
        if (c.getContrato() != null) {
            for (Lote l : contratoLoteRepository.findLotesByContrato(c.getContrato().getIdContrato())) {
                if (l.getManzana() != null && !l.getManzana().isBlank()) mz.add(l.getManzana());
            }
        }
        return mz;
    }

    private List<String> lotesPorContratoLocal(ComisionVendedor c) {
        List<String> lotes = new ArrayList<>();
        if (c.getContrato() != null) {
            for (Lote l : contratoLoteRepository.findLotesByContrato(c.getContrato().getIdContrato())) {
                if (l.getNumeroLote() != null && !l.getNumeroLote().isBlank()) lotes.add(l.getNumeroLote());
            }
        }
        return lotes;
    }

    private List<String> extraerUrlsVouchers(ReciboEgreso egreso) {
        return voucherRepository
                .findByTipoOrigenAndReferenciaId("PAGO_COMISION", egreso.getIdReciboEgreso().intValue())
                .stream().map(Voucher::getUrl).collect(Collectors.toList());
    }

    /** Nombre del usuario autenticado que registra el pago (para el recibo). */
    private String obtenerUsuarioRegistro() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "SECRETARIA";
        try {
            Usuario u = usuarioService.buscarByUsuario(auth.getName());
            if (u != null) {
                String nombre = ((u.getNombres() == null ? "" : u.getNombres())
                        + " " + (u.getApellidos() == null ? "" : u.getApellidos())).trim();
                if (!nombre.isBlank()) return nombre.toUpperCase();
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener el usuario actual: {}", e.getMessage());
        }
        return auth.getName();
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

    // ─── Sincronizar vendedor al editar el contrato ───────────────────────────

    @Override
    @Transactional
    public void sincronizarVendedorComision(Contrato contrato) {
        if (contrato == null || contrato.getIdContrato() == null) return;
        Integer idContrato = contrato.getIdContrato();

        boolean tieneVendedorValido = contrato.getVendedor() != null
                && !esVendedorLaPropiaInmobiliaria(contrato.getVendedor())
                && contrato.getVendedor().getComision() != null
                && contrato.getVendedor().getComision().compareTo(BigDecimal.ZERO) > 0;
        boolean esElegible = (contrato.getTipoContrato() == com.Inmobiliaria.demo.enums.TipoContrato.FINANCIADO
                || contrato.getTipoContrato() == com.Inmobiliaria.demo.enums.TipoContrato.CONTADO)
                && !contratoEstadoTerminal(contrato.getEstadoContrato());

        var existente = comisionRepository.findByContratoIdContrato(idContrato);

        if (!esElegible || !tieneVendedorValido) {
            // El contrato ya no califica para comisión → anular (si no está completada).
            existente.ifPresent(c -> {
                if (c.getEstado() != EstadoComision.COMPLETADA) {
                    c.setEstado(EstadoComision.ANULADA);
                    comisionRepository.save(c);
                    log.info("Comisión {} ANULADA al editar contrato {} (sin vendedor elegible)",
                            c.getIdComision(), idContrato);
                }
            });
            return;
        }

        ComisionVendedor comision = existente.orElseGet(() -> crearComisionSiAplica(contrato));
        if (comision == null) return;

        // Actualizar vendedor (el % se congeló al crear; si aún no hay pagos, se
        // recalcula con el % del nuevo vendedor).
        comision.setVendedor(contrato.getVendedor());
        boolean tienePagos = pagoComisionRepository
                .findByComisionIdComisionOrderByIdPagoComisionAsc(comision.getIdComision()).stream()
                .anyMatch(p -> p.getTipo() != null);
        if (!tienePagos) {
            BigDecimal montoTotal = contrato.getMontoTotal() != null ? contrato.getMontoTotal() : BigDecimal.ZERO;
            BigDecimal nuevoTotal = floor(porcentajeDe(montoTotal, contrato.getVendedor().getComision()));
            comision.setPorcentajeComision(contrato.getVendedor().getComision());
            comision.setMontoTotalContrato(montoTotal);
            comision.setMontoComisionTotal(nuevoTotal);
            comision.setSaldoPendiente(nuevoTotal);
            comision.setMontoAdelanto(null);
            comision.setEstado(EstadoComision.PENDIENTE);
        }
        comisionRepository.save(comision);
        log.info("Comisión {} sincronizada con el nuevo vendedor del contrato {}",
                comision.getIdComision(), idContrato);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ComisionVendedor obtenerComision(Integer idComision) {
        return comisionRepository.findById(idComision)
                .orElseThrow(() -> new NegocioException("Comisión no encontrada: " + idComision));
    }

    /**
     * Estados del contrato en los que NO se cobra comisión al vendedor:
     * RENUNCIA, RESUELTO o TRANSFERIDO. CANCELADO NO es terminal para la comisión:
     * un financiado pagado íntegro (CANCELADO) sí genera comisión, y los contratos
     * CONTADO se crean directamente en estado CANCELADO.
     */
    private boolean contratoEstadoTerminal(EstadoContrato estado) {
        if (estado == null) return false;
        return estado == EstadoContrato.RENUNCIA
                || estado == EstadoContrato.RESUELTO
                || estado == EstadoContrato.TRANSFERIDO;
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