package com.Inmobiliaria.demo.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.Inmobiliaria.demo.dto.PagoLetraRequestDTO;
import com.Inmobiliaria.demo.dto.PagoLetraResponseDTO;
import com.Inmobiliaria.demo.dto.PagosMultiplesRequestDTO;
import com.Inmobiliaria.demo.dto.SugerenciaNumeroComprobanteDTO;
import com.Inmobiliaria.demo.entity.*;
import com.Inmobiliaria.demo.enums.*;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.*;
import com.Inmobiliaria.demo.service.ComprobanteService;
import com.Inmobiliaria.demo.service.PagoLetraService;

import org.springframework.cache.annotation.CacheEvict;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PagoLetraServiceImpl implements PagoLetraService {

    private final PagoLetraRepository   pagoLetraRepository;
    private final LetraCambioRepository letraCambioRepository;
    private final VoucherRepository     voucherRepository;
    private final ContratoRepository    contratoRepository;
    private final Cloudinary            cloudinary;
    private final MoraRepository        moraRepository;
    private final MoraServiceImpl       moraService;

    private final ComprobanteService    comprobanteService;

    // ─── LETRAS PAGADAS NECESARIAS PARA OBTENER UNA GRATIS ────────────────────
    private static final int LETRAS_PARA_GRATIS = 10;

    // ═══════════════════════════════════════════════════════════════════════════
    // Utilidades internas
    // ═══════════════════════════════════════════════════════════════════════════

    private int extraerNumeroLetra(String numeroLetra) {
        if (numeroLetra == null || numeroLetra.isBlank()) return 0;
        String parte = numeroLetra.contains("/")
            ? numeroLetra.split("/")[0].trim()
            : numeroLetra.trim();
        try { return Integer.parseInt(parte); }
        catch (NumberFormatException e) { return 0; }
    }

    private void validarOrdenDePago(Integer idContrato, String numeroLetraStr) {
        int numLetraAPagar = extraerNumeroLetra(numeroLetraStr);
        Optional<Integer> maxPagadoOpt = pagoLetraRepository.findMaxNumeroLetraPagadoByContrato(idContrato);
        if (maxPagadoOpt.isEmpty() || maxPagadoOpt.get() == null) return;
        int maxPagado = maxPagadoOpt.get();
        if (numLetraAPagar > maxPagado + 1) {
            throw new NegocioException(
                "No puede pagar la letra N° " + numLetraAPagar +
                " porque el pago siguiente debe ser la letra N° " + (maxPagado + 1) + "."
            );
        }
    }

    private void validarOrdenDePagoMultiple(Integer idContrato, List<String> numerosLetra) {
        if (numerosLetra == null || numerosLetra.isEmpty()) return;
        List<Integer> nums = numerosLetra.stream()
            .map(this::extraerNumeroLetra).sorted().collect(Collectors.toList());
        int maxPagado = pagoLetraRepository.findMaxNumeroLetraPagadoByContrato(idContrato)
            .orElse(0);
        int primerNum = nums.get(0);
        if (primerNum > maxPagado + 1) {
            throw new NegocioException(
                "No puede pagar la letra N° " + primerNum +
                " porque el pago siguiente debe ser la letra N° " + (maxPagado + 1) + "."
            );
        }
        for (int i = 1; i < nums.size(); i++) {
            if (nums.get(i) != nums.get(i - 1) + 1) {
                throw new NegocioException(
                    "Las letras seleccionadas no son consecutivas: N° " +
                    nums.get(i - 1) + " y N° " + nums.get(i) + "."
                );
            }
        }
    }

    private LocalDate resolverFechaReferenciaMora(int numLetraActual, Integer idContrato, LocalDate fechaOperacion) {
        int maxPagado = pagoLetraRepository.findMaxNumeroLetraPagadoByContrato(idContrato).orElse(0);
        return numLetraActual < maxPagado ? fechaOperacion : LocalDate.now();
    }

    /**
     * Calcula el saldo pendiente de una letra sumando todos sus pagos previos
     * y restándolos del importe original.
     */
    private BigDecimal calcularSaldoPendiente(LetraCambio letra) {
        BigDecimal totalPagado = pagoLetraRepository.sumImportePagadoByLetra(letra.getIdLetra());
        if (totalPagado == null) totalPagado = BigDecimal.ZERO;
        BigDecimal saldo = letra.getImporte().subtract(totalPagado);
        return saldo.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : saldo;
    }

    /**
     * Valida y aplica los campos de pago parcial sobre la entidad letra.
     * Retorna el saldo resultante DESPUÉS del pago que se está registrando.
     *
     * Reglas:
     * - esPagoAcuenta = false (pago total): importePagado debe == saldoPendiente actual.
     * - esPagoAcuenta = true  (pago parcial): importePagado debe ser > 0 y <= saldoPendiente.
     */
    private BigDecimal validarYCalcularNuevoSaldo(LetraCambio letra, BigDecimal importePagado, boolean esPagoAcuenta) {
        BigDecimal saldoActual = letra.getSaldoPendiente();

        // Primera vez: saldo_pendiente puede venir como 0 (migración) → inicializar.
        // Se aplica a cualquier estado que no sea PAGADO (cubre PENDIENTE, VENCIDO, PARCIAL).
        if (saldoActual.compareTo(BigDecimal.ZERO) == 0
                && letra.getEstadoLetra() != EstadoLetra.PAGADO) {
            saldoActual = calcularSaldoPendiente(letra);
            // Si no hay pagos previos, el saldo real = importe completo
            if (saldoActual.compareTo(BigDecimal.ZERO) == 0) {
                saldoActual = letra.getImporte();
            }
            letra.setSaldoPendiente(saldoActual);
        }

        if (importePagado == null || importePagado.compareTo(BigDecimal.ZERO) <= 0)
            throw new NegocioException("El importe pagado debe ser mayor a cero.");

        if (importePagado.compareTo(saldoActual) > 0)
            throw new NegocioException(
                "El importe pagado ($" + importePagado + ") supera el saldo pendiente ($" + saldoActual + ").");

        if (!esPagoAcuenta && importePagado.compareTo(saldoActual) != 0)
            throw new NegocioException(
                "Para un pago total el importe debe ser igual al saldo pendiente ($" + saldoActual +
                "). Si desea abonar un monto menor, marque la opción 'Pago a cuenta'.");

        return saldoActual.subtract(importePagado);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Estado contrato
    // ═══════════════════════════════════════════════════════════════════════════

    @CacheEvict(cacheNames = "contratos", allEntries = true)
    public void verificarYActualizarEstadoContrato(Contrato contrato) {
        if (contrato.getTipoContrato() != TipoContrato.FINANCIADO) return;

        Contrato contratoFresco = contratoRepository.findById(contrato.getIdContrato()).orElse(null);
        if (contratoFresco == null) return;

        EstadoContrato estadoActualFresco = contratoFresco.getEstadoContrato();
        if (estadoActualFresco == EstadoContrato.CANCELADO    ||
            estadoActualFresco == EstadoContrato.RESUELTO      ||
            estadoActualFresco == EstadoContrato.EN_RESOLUCION ||
            estadoActualFresco == EstadoContrato.RENUNCIA      ||
            estadoActualFresco == EstadoContrato.TRANSFERIDO) return;

        List<LetraCambio> letras = contratoFresco.getLetrasCambio();
        if (letras == null || letras.isEmpty()) return;

        // ── ¿Ya se pagó la última letra? → CANCELADO ────────────────────────
        // Una letra con PARCIAL no se considera pagada para este efecto
        boolean ultimaLetraPagada = letras.stream()
            .max(Comparator.comparingInt(l -> extraerNumeroLetra(l.getNumeroLetra())))
            .map(l -> l.getEstadoLetra() == EstadoLetra.PAGADO)
            .orElse(false);

        if (ultimaLetraPagada) {
            contratoFresco.setEstadoContrato(EstadoContrato.CANCELADO);
            contratoRepository.save(contratoFresco);
            return;
        }

        int maxPagado = pagoLetraRepository
            .findMaxNumeroLetraPagadoByContrato(contratoFresco.getIdContrato())
            .orElse(0);

        boolean tieneMoraReal = letras.stream()
            .filter(l -> l.getEstadoLetra() == EstadoLetra.VENCIDO)
            .anyMatch(l -> extraerNumeroLetra(l.getNumeroLetra()) > maxPagado);

        EstadoContrato nuevoEstado = tieneMoraReal ? EstadoContrato.MORA : EstadoContrato.ACTIVO;

        if (nuevoEstado != estadoActualFresco) {
            contratoFresco.setEstadoContrato(nuevoEstado);
            contratoRepository.save(contratoFresco);
        }
    }

    @CacheEvict(cacheNames = "contratos", allEntries = true)
    public void recalcularEstadoContrato(Contrato contrato) {
        if (contrato.getTipoContrato() != TipoContrato.FINANCIADO) return;
        EstadoContrato estadoActual = contrato.getEstadoContrato();
        if (estadoActual == EstadoContrato.CANCELADO    ||
            estadoActual == EstadoContrato.RESUELTO      ||
            estadoActual == EstadoContrato.EN_RESOLUCION ||
            estadoActual == EstadoContrato.RENUNCIA      ||
            estadoActual == EstadoContrato.TRANSFERIDO) return;

        Contrato contratoFresco = contratoRepository.findById(contrato.getIdContrato()).orElse(null);
        if (contratoFresco == null) return;

        long letrasVencidas = contratoFresco.getLetrasCambio().stream()
            .filter(l -> l.getEstadoLetra() == EstadoLetra.VENCIDO).count();

        EstadoContrato nuevoEstado = letrasVencidas > 0 ? EstadoContrato.MORA : EstadoContrato.ACTIVO;
        if (nuevoEstado != estadoActual) {
            contratoFresco.setEstadoContrato(nuevoEstado);
            contratoRepository.save(contratoFresco);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Mapper
    // ═══════════════════════════════════════════════════════════════════════════

    private PagoLetraResponseDTO mapToDTO(PagoLetras pago) {
        PagoLetraResponseDTO dto = new PagoLetraResponseDTO();
        dto.setIdPago(pago.getIdPago());
        dto.setIdLetra(pago.getLetra().getIdLetra());
        dto.setNumeroLetra(pago.getLetra().getNumeroLetra());
        dto.setFechaPago(pago.getFechaPago());
        dto.setImportePagado(pago.getImportePagado());
        dto.setMedioPago(pago.getMedioPago());
        dto.setNumeroOperacion(pago.getNumeroOperacion());
        dto.setObservaciones(pago.getObservaciones());
        dto.setEsPagoAcuenta(pago.getEsPagoAcuenta());

        // ── Datos de saldo ──────────────────────────────────────────────────
        dto.setImporteLetra(pago.getLetra().getImporte());
        dto.setSaldoPendiente(pago.getLetra().getSaldoPendiente());
        dto.setEstadoLetra(pago.getLetra().getEstadoLetra());

        // ── Comprobante ─────────────────────────────────────────────────────
        if (pago.getComprobante() != null) {
            dto.setIdComprobante(pago.getComprobante().getIdComprobante());
            dto.setTipoComprobante(pago.getComprobante().getTipoComprobante());
            dto.setNumeroComprobante(pago.getComprobante().getNumeroCompleto());
        }

        List<String> urls = voucherRepository
            .findByTipoOrigenAndReferenciaId("PAGO_LETRA", pago.getIdPago())
            .stream().map(Voucher::getUrl).collect(Collectors.toList());
        dto.setUrlsVoucher(urls);
        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Vouchers
    // ═══════════════════════════════════════════════════════════════════════════

    private String subirImagen(MultipartFile file, Integer idContrato, Integer idLetra) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String publicId = "letra-" + idLetra + "-" + timestamp;
        Map<?, ?> params = ObjectUtils.asMap(
            "folder", "vouchers/contrato-" + idContrato, "public_id", publicId);
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), params);
        return result.get("url").toString();
    }

    private void guardarVouchers(List<MultipartFile> files, PagoLetras pago,
                                  Integer idContrato, Integer idLetra) throws IOException {
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                Voucher v = new Voucher();
                v.setTipoOrigen("PAGO_LETRA");
                v.setReferenciaId(pago.getIdPago());
                v.setUrl(subirImagen(file, idContrato, idLetra));
                voucherRepository.save(v);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Consultas
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public SugerenciaNumeroComprobanteDTO sugerirNumeroComprobante(TipoComprobante tipoComprobante) {
        String numero = comprobanteService.previewSiguienteNumero(tipoComprobante);
        return new SugerenciaNumeroComprobanteDTO(numero);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoLetraResponseDTO> listarPorContrato(Integer idContrato) {
        return pagoLetraRepository.findByLetraContratoIdContrato(idContrato)
            .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoLetraResponseDTO> listarPorLetra(Integer idLetra) {
        return pagoLetraRepository.findByLetraIdLetra(idLetra)
            .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagoLetraResponseDTO obtenerPorId(Integer idPago) {
        return mapToDTO(pagoLetraRepository.findById(idPago)
            .orElseThrow(() -> new NegocioException("Pago no encontrado con id: " + idPago)));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal consultarSaldoPendiente(Integer idLetra) {
        LetraCambio letra = letraCambioRepository.findById(idLetra)
            .orElseThrow(() -> new NegocioException("Letra no encontrada con id: " + idLetra));
        return letra.getSaldoPendiente();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REGISTRAR PAGO SIMPLE (con soporte de pago parcial)
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    @CacheEvict(value = "contratos", allEntries = true)
    public PagoLetraResponseDTO registrarPago(PagoLetraRequestDTO request,
                                               List<MultipartFile> vouchers) throws IOException {
        LetraCambio letra = letraCambioRepository.findById(request.getIdLetra())
            .orElseThrow(() -> new NegocioException("Letra no encontrada con id: " + request.getIdLetra()));

        if (letra.getEstadoLetra() == EstadoLetra.PAGADO)
            throw new NegocioException("La letra ya se encuentra pagada.");

        boolean esPagoAcuenta = Boolean.TRUE.equals(request.getEsPagoAcuenta());

        // ── Validar y calcular nuevo saldo ───────────────────────────────────
        BigDecimal nuevoSaldo = validarYCalcularNuevoSaldo(letra, request.getImportePagado(), esPagoAcuenta);

        Integer idContrato = letra.getContrato().getIdContrato();

        // ── Solo validar orden si la letra aún no tiene pagos parciales ──────
        // Si ya tiene pagos previos (PARCIAL) es el mismo "turno" de esa letra
        if (letra.getEstadoLetra() != EstadoLetra.PARCIAL) {
            validarOrdenDePago(idContrato, letra.getNumeroLetra());
        }

        // ── Construir el pago ────────────────────────────────────────────────
        PagoLetras pago = new PagoLetras();
        pago.setLetra(letra);
        pago.setFechaPago(request.getFechaPago());
        pago.setImportePagado(request.getImportePagado());
        pago.setMedioPago(request.getMedioPago());
        pago.setNumeroOperacion(request.getNumeroOperacion());
        pago.setObservaciones(request.getObservaciones());
        pago.setEsPagoAcuenta(esPagoAcuenta);
        pago.setDescuentoAplicado(BigDecimal.ZERO);
        pago.setEsLetraGratis(false);

        PagoLetras pagoGuardado = pagoLetraRepository.save(pago);

        // ── Comprobante ──────────────────────────────────────────────────────
        if (request.getTipoComprobante() != null) {
            Comprobante comprobante = comprobanteService.generarComprobanteConNumero(
                request.getTipoComprobante(),
                TipoOrigenComprobante.PAGO_LETRA,
                pagoGuardado.getIdPago(),
                request.getImportePagado(),
                request.getFechaPago(),
                request.getNumeroComprobantePersonalizado()
            );
            pagoGuardado.setComprobante(comprobante);
            pagoGuardado = pagoLetraRepository.save(pagoGuardado);
        }

        guardarVouchers(vouchers, pagoGuardado, idContrato, letra.getIdLetra());

        // ── Actualizar saldo y estado de la letra ────────────────────────────
        letra.setSaldoPendiente(nuevoSaldo);

        if (nuevoSaldo.compareTo(BigDecimal.ZERO) == 0) {
            // Pago completado → lógica de mora si aplica
            if (letra.getEstadoLetra() == EstadoLetra.VENCIDO) {
                int numLetra = extraerNumeroLetra(letra.getNumeroLetra());
                LocalDate fechaRef = resolverFechaReferenciaMora(numLetra, idContrato, request.getFechaPago());
                moraService.generarMoraParaPago(letra, pagoGuardado, fechaRef);
            }
            letra.setEstadoLetra(EstadoLetra.PAGADO);
        } else {
            // Pago parcial → la letra pasa a PARCIAL (o sigue PARCIAL si ya lo era)
            letra.setEstadoLetra(EstadoLetra.PARCIAL);
        }

        letraCambioRepository.save(letra);
        verificarYActualizarEstadoContrato(letra.getContrato());

        return mapToDTO(pagoGuardado);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REGISTRAR PAGOS MÚLTIPLES (con descuento negociado + letra gratis)
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public Map<String, Object> registrarPagosMultiples(PagosMultiplesRequestDTO request,
                                                               List<MultipartFile> vouchers) throws IOException {
        if (request.getPagos() == null || request.getPagos().isEmpty())
            throw new NegocioException("La lista de pagos no puede estar vacía.");

        Integer idPrimeraLetra = request.getPagos().get(0).getIdLetra();
        LetraCambio letraEjemplo = letraCambioRepository.findById(idPrimeraLetra)
            .orElseThrow(() -> new NegocioException("Letra no encontrada: " + idPrimeraLetra));
        Integer idContrato = letraEjemplo.getContrato().getIdContrato();

        // ── Cargar todas las letras del request ──────────────────────────────
        List<LetraCambio> letrasDelLote = new ArrayList<>();
        List<String> numerosLetraRequest = new ArrayList<>();
        for (PagoLetraRequestDTO req : request.getPagos()) {
            LetraCambio lc = letraCambioRepository.findById(req.getIdLetra())
                .orElseThrow(() -> new NegocioException("Letra no encontrada: " + req.getIdLetra()));
            letrasDelLote.add(lc);
            numerosLetraRequest.add(lc.getNumeroLetra());
        }
        validarOrdenDePagoMultiple(idContrato, numerosLetraRequest);

        // ── Validar y procesar LETRA GRATIS ──────────────────────────────────
        LetraCambio letraGratis = null;
        if (request.getIdLetraGratis() != null) {
            // Se pasa la cantidad de letras del lote actual que se van a pagar
            // para que se incluyan en el conteo de letras pagadas al evaluar
            // si corresponde otorgar la letra gratis.
            letraGratis = validarLetraGratis(
                request.getIdLetraGratis(), idContrato, letrasDelLote.size());
        }

        // ── Calcular descuentos ───────────────────────────────────────────────
        // El descuento se prorratea proporcionalmente al importe de cada letra.
        BigDecimal descuentoTotal = request.getDescuentoNegociado();
        if (descuentoTotal == null) descuentoTotal = BigDecimal.ZERO;

        // El total real cobrado = suma de importes del lote - descuento negociado
        BigDecimal montoTotalBruto = request.getPagos().stream()
            .map(PagoLetraRequestDTO::getImportePagado)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (descuentoTotal.compareTo(montoTotalBruto) > 0)
            throw new NegocioException("El descuento ($" + descuentoTotal +
                ") no puede superar el monto total del lote ($" + montoTotalBruto + ").");

        BigDecimal montoTotalNeto = montoTotalBruto.subtract(descuentoTotal);

        // ── Subir vouchers una sola vez ───────────────────────────────────────
        List<String> urlsVoucher = new ArrayList<>();
        if (vouchers != null && !vouchers.isEmpty()) {
            for (MultipartFile v : vouchers)
                urlsVoucher.add(subirImagen(v, idContrato, null));
        }

        // ── Crear UN SOLO comprobante para todo el lote ───────────────────────
        PagoLetraRequestDTO primerPago = request.getPagos().get(0);
        Comprobante comprobanteCompartido = null;
        if (primerPago.getTipoComprobante() != null) {
            comprobanteCompartido = comprobanteService.generarComprobanteConNumero(
                primerPago.getTipoComprobante(),
                TipoOrigenComprobante.PAGO_LETRA,
                null,
                montoTotalNeto,   // monto neto (ya con descuento aplicado)
                primerPago.getFechaPago(),
                primerPago.getNumeroComprobantePersonalizado()
            );
        }

        List<PagoLetraResponseDTO> responses = new ArrayList<>();

        // ── Procesar cada letra del lote ──────────────────────────────────────
        for (int i = 0; i < request.getPagos().size(); i++) {
            PagoLetraRequestDTO pagoReq = request.getPagos().get(i);
            LetraCambio letra = letrasDelLote.get(i);

            if (letra.getEstadoLetra() == EstadoLetra.PAGADO)
                throw new NegocioException("La letra " + letra.getNumeroLetra() + " ya está pagada.");

            // ── Si esta letra es la gratis, se procesa aparte → skip ─────────
            if (letraGratis != null && letra.getIdLetra().equals(letraGratis.getIdLetra()))
                continue;

            // ── Calcular porción de descuento para esta letra ────────────────
            // Prorrateo: descuentoLetra = descuentoTotal * (importeLetra / montoTotalBruto)
            BigDecimal descuentoLetra = BigDecimal.ZERO;
            if (descuentoTotal.compareTo(BigDecimal.ZERO) > 0 && montoTotalBruto.compareTo(BigDecimal.ZERO) > 0) {
                descuentoLetra = descuentoTotal
                    .multiply(pagoReq.getImportePagado())
                    .divide(montoTotalBruto, 2, RoundingMode.HALF_UP);
            }
            BigDecimal importeNetoLetra = pagoReq.getImportePagado().subtract(descuentoLetra);

            // ── Validar importe con saldo real de la letra ───────────────────
            boolean esPagoAcuenta = Boolean.TRUE.equals(pagoReq.getEsPagoAcuenta());
            BigDecimal nuevoSaldo = validarYCalcularNuevoSaldo(letra, pagoReq.getImportePagado(), esPagoAcuenta);

            PagoLetras pago = new PagoLetras();
            pago.setLetra(letra);
            pago.setFechaPago(pagoReq.getFechaPago());
            pago.setImportePagado(importeNetoLetra);   // se guarda el importe neto
            pago.setMedioPago(pagoReq.getMedioPago());
            pago.setNumeroOperacion(pagoReq.getNumeroOperacion());
            pago.setObservaciones(construirObservaciones(pagoReq.getObservaciones(),
                descuentoLetra, request.getMotivoDescuento(), false));
            pago.setEsPagoAcuenta(esPagoAcuenta);
            pago.setDescuentoAplicado(descuentoLetra);
            pago.setEsLetraGratis(false);
            pago.setComprobante(comprobanteCompartido);

            PagoLetras guardado = pagoLetraRepository.save(pago);

            for (String url : urlsVoucher) {
                Voucher v = new Voucher();
                v.setTipoOrigen("PAGO_LETRA");
                v.setReferenciaId(guardado.getIdPago());
                v.setUrl(url);
                voucherRepository.save(v);
            }

            // ── Actualizar saldo y estado de la letra ────────────────────────
            // El saldo se reduce por el importe bruto (sin descuento aplicado al cálculo del saldo)
            letra.setSaldoPendiente(nuevoSaldo);
            if (nuevoSaldo.compareTo(BigDecimal.ZERO) == 0) {
                if (letra.getEstadoLetra() == EstadoLetra.VENCIDO) {
                    int numLetra = extraerNumeroLetra(letra.getNumeroLetra());
                    LocalDate fechaRef = resolverFechaReferenciaMora(numLetra, idContrato, pagoReq.getFechaPago());
                    moraService.generarMoraParaPago(letra, guardado, fechaRef);
                }
                letra.setEstadoLetra(EstadoLetra.PAGADO);
            } else {
                letra.setEstadoLetra(EstadoLetra.PARCIAL);
            }
            letraCambioRepository.save(letra);

            responses.add(mapToDTO(guardado));
        }

        // ── Procesar LETRA GRATIS (si aplica) ────────────────────────────────
        PagoLetraResponseDTO responseLetraGratis = null;
        if (letraGratis != null) {
            responseLetraGratis = registrarLetraGratis(
                letraGratis, idContrato, primerPago.getFechaPago(),
                primerPago.getMedioPago(), comprobanteCompartido,
                request.getMotivoLetraGratis(), urlsVoucher
            );
        }

        letraCambioRepository.findById(request.getPagos().get(0).getIdLetra())
            .ifPresent(l -> verificarYActualizarEstadoContrato(l.getContrato()));

        // ── Armar respuesta ───────────────────────────────────────────────────
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("pagos", responses);
        resultado.put("numeroComprobanteGenerado",
            comprobanteCompartido != null ? comprobanteCompartido.getNumeroCompleto() : null);
        resultado.put("descuentoAplicado", descuentoTotal);
        resultado.put("montoTotalNeto", montoTotalNeto);
        if (responseLetraGratis != null) {
            resultado.put("letraGratis", responseLetraGratis);
        }
        return resultado;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Lógica de LETRA GRATIS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Valida que corresponda otorgar una letra gratis.
     * Regla: por cada {@value LETRAS_PARA_GRATIS} letras PAGADAS en el contrato
     * el cliente tiene derecho a 1 letra gratis.
     *
     * Cálculo: letrasGratisPosibles = (totalPagadas + letrasEnLote) / LETRAS_PARA_GRATIS
     *
     * @param letrasEnLote cantidad de letras del lote ACTUAL que se están pagando
     *                     en esta misma operación (aún no están en BD como PAGADO).
     *                     Se suman al conteo para reflejar la situación real post-pago.
     */
    private LetraCambio validarLetraGratis(Integer idLetraGratis, Integer idContrato,
                                            int letrasEnLote) {
        LetraCambio letra = letraCambioRepository.findById(idLetraGratis)
            .orElseThrow(() -> new NegocioException("Letra gratis no encontrada: " + idLetraGratis));

        if (!letra.getContrato().getIdContrato().equals(idContrato))
            throw new NegocioException("La letra gratis no pertenece al mismo contrato.");

        if (letra.getEstadoLetra() == EstadoLetra.PAGADO)
            throw new NegocioException("La letra marcada como gratis ya está pagada.");

        // Contar letras ya PAGADAS en BD + las del lote actual (se pagarán en esta misma operación)
        long pagadasEnBD = letraCambioRepository.findByContratoIdContrato(idContrato)
            .stream()
            .filter(l -> l.getEstadoLetra() == EstadoLetra.PAGADO)
            .count();
        long totalPagadas = pagadasEnBD + letrasEnLote;

        // Contar cuántas letras gratis ya se han otorgado
        long gratisYaUsadas = pagoLetraRepository
            .findByLetraContratoIdContrato(idContrato)
            .stream()
            .filter(p -> Boolean.TRUE.equals(p.getEsLetraGratis()))
            .map(p -> p.getLetra().getIdLetra())
            .distinct()
            .count();

        long gratisPosibles = totalPagadas / LETRAS_PARA_GRATIS;

        if (gratisPosibles <= gratisYaUsadas)
            throw new NegocioException(
                "No corresponde letra gratis. Se requieren " + LETRAS_PARA_GRATIS +
                " letras pagadas por cada letra gratis. Letras pagadas (incluyendo este lote): " +
                totalPagadas + ", letras gratis usadas: " + gratisYaUsadas + ".");

        return letra;
    }

    /**
     * Registra un pago de $0.00 para la letra gratis.
     * El descuentoAplicado = importe de la letra (100% condonado).
     */
    private PagoLetraResponseDTO registrarLetraGratis(
            LetraCambio letra, Integer idContrato, LocalDate fechaPago,
            MedioPago medioPago, Comprobante comprobante,
            String motivo, List<String> urlsVoucher) {

        // Inicializar saldo si es necesario
        if (letra.getSaldoPendiente().compareTo(BigDecimal.ZERO) == 0
                && letra.getEstadoLetra() == EstadoLetra.PENDIENTE) {
            letra.setSaldoPendiente(letra.getImporte());
        }

        PagoLetras pago = new PagoLetras();
        pago.setLetra(letra);
        pago.setFechaPago(fechaPago);
        pago.setImportePagado(BigDecimal.ZERO);              // no se cobra nada
        pago.setMedioPago(medioPago);
        pago.setNumeroOperacion(null);
        pago.setObservaciones(motivo != null ? motivo : "Letra gratis por política comercial");
        pago.setEsPagoAcuenta(false);
        pago.setDescuentoAplicado(letra.getSaldoPendiente()); // descuento = saldo completo
        pago.setEsLetraGratis(true);
        pago.setComprobante(comprobante);

        PagoLetras guardado = pagoLetraRepository.save(pago);

        for (String url : urlsVoucher) {
            Voucher v = new Voucher();
            v.setTipoOrigen("PAGO_LETRA");
            v.setReferenciaId(guardado.getIdPago());
            v.setUrl(url);
            voucherRepository.save(v);
        }

        // La letra gratis queda como PAGADO con saldo 0
        letra.setSaldoPendiente(BigDecimal.ZERO);
        letra.setEstadoLetra(EstadoLetra.PAGADO);
        letraCambioRepository.save(letra);

        return mapToDTO(guardado);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Actualizar pago
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public PagoLetraResponseDTO actualizarPago(Integer idPago, PagoLetraRequestDTO request,
                                                List<MultipartFile> vouchers) throws IOException {
        PagoLetras pago = pagoLetraRepository.findById(idPago)
            .orElseThrow(() -> new NegocioException("Pago no encontrado con id: " + idPago));

        pago.setImportePagado(request.getImportePagado());
        pago.setMedioPago(request.getMedioPago());
        pago.setNumeroOperacion(request.getNumeroOperacion());
        pago.setFechaPago(request.getFechaPago());
        pago.setObservaciones(request.getObservaciones());

        if (vouchers != null && !vouchers.isEmpty()) {
            List<Voucher> vouchersExistentes = voucherRepository
                .findByTipoOrigenAndReferenciaId("PAGO_LETRA", pago.getIdPago());
            for (Voucher v : vouchersExistentes) {
                try {
                    String publicId = extractPublicIdFromUrl(v.getUrl());
                    if (publicId != null) cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                } catch (Exception e) {
                    System.err.println("Error al eliminar imagen antigua: " + e.getMessage());
                }
                voucherRepository.delete(v);
            }
            Integer idContrato = pago.getLetra().getContrato().getIdContrato();
            Integer idLetra    = pago.getLetra().getIdLetra();
            for (MultipartFile file : vouchers) {
                Voucher v = new Voucher();
                v.setTipoOrigen("PAGO_LETRA");
                v.setReferenciaId(pago.getIdPago());
                v.setUrl(subirImagen(file, idContrato, idLetra));
                voucherRepository.save(v);
            }
        }

        return mapToDTO(pagoLetraRepository.save(pago));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Eliminar pago
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    @CacheEvict(cacheNames = "contratos", allEntries = true)
    public void eliminarPago(Integer idPago) throws IOException {
        PagoLetras pago = pagoLetraRepository.findById(idPago)
            .orElseThrow(() -> new NegocioException("Pago no encontrado con id: " + idPago));

        LetraCambio letra = pago.getLetra();

        List<Voucher> vouchersExistentes = voucherRepository
            .findByTipoOrigenAndReferenciaId("PAGO_LETRA", idPago);
        for (Voucher v : vouchersExistentes) {
            try {
                String publicId = extractPublicIdFromUrl(v.getUrl());
                if (publicId != null) cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            } catch (Exception e) {
                System.err.println("Error al eliminar imagen de Cloudinary: " + e.getMessage());
            }
            voucherRepository.delete(v);
        }

        List<MoraLetra> morasAsociadas = moraRepository.findByPagoLetraIdPago(idPago);
        for (MoraLetra mora : morasAsociadas) {
            if (mora.getEstadoMora() == EstadoMora.PENDIENTE) {
                moraRepository.delete(mora);
            } else {
                mora.setPagoLetra(null);
                moraRepository.save(mora);
            }
        }

        Long idComprobante = pago.getComprobante() != null
                ? pago.getComprobante().getIdComprobante()
                : null;

        String numeroComprobante = pago.getComprobante() != null
                ? pago.getComprobante().getNumeroCompleto()
                : null;

        pago.setComprobante(null);
        pagoLetraRepository.save(pago);
        pagoLetraRepository.delete(pago);

        if (idComprobante != null && numeroComprobante != null) {
            // Solo eliminar el comprobante si ningún otro pago lo sigue referenciando.
            // Se consulta DESPUÉS de haber borrado el pago actual, por lo que un
            // resultado de 0 significa que el comprobante ya no tiene dueños.
            long otrosPagosConMismoComprobante =
                pagoLetraRepository.countByComprobanteNumeroCompleto(numeroComprobante);

            if (otrosPagosConMismoComprobante == 0) {
                comprobanteService.eliminarComprobante(idComprobante);
            }
        }

        // ── Recalcular saldo de la letra después de eliminar el pago ─────────
        BigDecimal totalPagadoRestante = pagoLetraRepository.sumImportePagadoByLetra(letra.getIdLetra());
        if (totalPagadoRestante == null) totalPagadoRestante = BigDecimal.ZERO;
        BigDecimal nuevoSaldo = letra.getImporte().subtract(totalPagadoRestante);

        long count = pagoLetraRepository.countByLetraIdLetra(letra.getIdLetra());
        if (count == 0) {
            // No quedan pagos → volver a estado original
            letra.setSaldoPendiente(letra.getImporte());
            letra.setEstadoLetra(letra.getFechaVencimiento().isBefore(LocalDate.now())
                ? EstadoLetra.VENCIDO : EstadoLetra.PENDIENTE);
        } else {
            // Quedan pagos parciales
            letra.setSaldoPendiente(nuevoSaldo);
            letra.setEstadoLetra(nuevoSaldo.compareTo(BigDecimal.ZERO) == 0
                ? EstadoLetra.PAGADO : EstadoLetra.PARCIAL);
        }
        letraCambioRepository.save(letra);

        recalcularEstadoContrato(letra.getContrato());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Preview número comprobante
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public String previewSiguienteNumeroComprobante(TipoComprobante tipoComprobante) {
        return comprobanteService.previewSiguienteNumero(tipoComprobante);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Utilidades privadas
    // ═══════════════════════════════════════════════════════════════════════════

    /** Construye el texto de observaciones incluyendo info del descuento si aplica. */
    private String construirObservaciones(String obsBase, BigDecimal descuento, String motivoDescuento, boolean esGratis) {
        StringBuilder sb = new StringBuilder();
        if (obsBase != null && !obsBase.isBlank()) sb.append(obsBase);
        if (descuento != null && descuento.compareTo(BigDecimal.ZERO) > 0) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append("Descuento aplicado: $").append(descuento.setScale(2, RoundingMode.HALF_UP));
            if (motivoDescuento != null && !motivoDescuento.isBlank())
                sb.append(" (").append(motivoDescuento).append(")");
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private String extractPublicIdFromUrl(String url) {
        try {
            int uploadIndex = url.indexOf("/upload/");
            if (uploadIndex == -1) return null;
            String afterUpload = url.substring(uploadIndex + 8);
            String[] parts = afterUpload.split("/", 2);
            if (parts.length == 2) {
                String path = parts[1];
                int dotIndex = path.lastIndexOf('.');
                return dotIndex != -1 ? path.substring(0, dotIndex) : path;
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error al extraer publicId: " + e.getMessage());
            return null;
        }
    }
}