package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.dto.*;
import com.Inmobiliaria.demo.entity.*;
import com.Inmobiliaria.demo.enums.EstadoMora;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.enums.TipoOrigenComprobante;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.ComprobanteRepository;
import com.Inmobiliaria.demo.repository.LetraCambioRepository;
import com.Inmobiliaria.demo.repository.MoraRepository;
import com.Inmobiliaria.demo.repository.PagoMoraRepository;
import com.Inmobiliaria.demo.repository.VoucherRepository;
import com.Inmobiliaria.demo.service.ComprobanteService;
import com.Inmobiliaria.demo.service.MoraService;
import com.Inmobiliaria.demo.service.SunatEnvioService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.Inmobiliaria.demo.util.FechasUtil;
import com.Inmobiliaria.demo.util.MedioPagoUtil;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.Inmobiliaria.demo.entity.Empresa;
import com.Inmobiliaria.demo.enums.TipoCalculoMora;
import com.Inmobiliaria.demo.service.EmpresaService;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoraServiceImpl implements MoraService {

    private final EmpresaService empresaService;

    private BigDecimal porcentajeMora() {
        return empresaService.obtenerActiva().getMoraPorcentaje();
    }

    private BigDecimal montoDiario() {
        BigDecimal val = empresaService.obtenerActiva().getMoraMontoDiario();
        return val != null ? val : BigDecimal.ZERO;
    }

    private BigDecimal tasaDiaria() {
        BigDecimal val = empresaService.obtenerActiva().getMoraTasaDiaria();
        return val != null ? val : BigDecimal.ZERO;
    }

    private TipoCalculoMora tipoCalculoMora() {
        return empresaService.obtenerActiva().getTipoCalculoMora();
    }

    static LocalDate aplicarGraciaDominical(LocalDate fechaVenc) {
        if (fechaVenc == null) return null;
        return fechaVenc.getDayOfWeek() == DayOfWeek.SUNDAY
            ? fechaVenc.plusDays(1)
            : fechaVenc;
    }

    private final MoraRepository        moraRepository;
    private final PagoMoraRepository    pagoMoraRepository;
    private final LetraCambioRepository letraCambioRepository;
    private final ComprobanteService    comprobanteService;
    private final ComprobanteRepository comprobanteRepository;
    private final SunatEnvioService     sunatEnvioService;
    private final VoucherRepository     voucherRepository;
    private final Cloudinary                   cloudinary;
    private final NotificacionAdminEmailService notificacionAdminEmailService;

    // ─── Calcular mora (sin persistir) ────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CalculoMoraDTO calcularMora(Integer idLetra) {
        return calcularMora(idLetra, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public CalculoMoraDTO calcularMora(Integer idLetra, LocalDate fechaReferencia) {
        LetraCambio letra = letraCambioRepository.findById(idLetra)
            .orElseThrow(() -> new NegocioException("Letra no encontrada con id: " + idLetra));

        LocalDate fechaVenc = letra.getFechaVencimiento();
        LocalDate fechaVencEfectiva = aplicarGraciaDominical(fechaVenc);

        if (!fechaVencEfectiva.isBefore(fechaReferencia)) {
            if (fechaVenc.isBefore(fechaReferencia)) {
                return new CalculoMoraDTO(idLetra, letra.getNumeroLetra(), letra.getImporte(),
                    fechaVenc, fechaReferencia, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, null);
            }
            throw new NegocioException(
                "La letra N° " + letra.getNumeroLetra() +
                " no está vencida. Vence el " + fechaVenc + ". No aplica mora."
            );
        }

        long dias = ChronoUnit.DAYS.between(fechaVenc, fechaReferencia);
        BigDecimal montoPct;
        BigDecimal montoDiar;
        BigDecimal total;
        if (tipoCalculoMora() == TipoCalculoMora.INTERES_DIARIO) {
            montoPct  = BigDecimal.ZERO;
            montoDiar = letra.getImporte().multiply(tasaDiaria()).multiply(BigDecimal.valueOf(dias)).setScale(2, RoundingMode.HALF_UP);
            total = montoDiar;
        } else {
            montoPct  = letra.getImporte().multiply(porcentajeMora()).setScale(2, RoundingMode.HALF_UP);
            montoDiar = montoDiario().multiply(BigDecimal.valueOf(dias)).setScale(2, RoundingMode.HALF_UP);
            total = montoPct.add(montoDiar);
        }

        boolean tienePrevia = moraRepository.existeMoraActivaParaLetra(idLetra);
        Integer idMoraPrevia = null;
        if (tienePrevia) {
            idMoraPrevia = moraRepository.findByLetraIdLetraAndEstadoMora(idLetra, EstadoMora.PENDIENTE)
                .map(MoraLetra::getIdMora).orElse(null);
        }

        return new CalculoMoraDTO(idLetra, letra.getNumeroLetra(), letra.getImporte(),
            fechaVenc, fechaReferencia, (int) dias, montoPct, montoDiar, total, tienePrevia, idMoraPrevia);
    }

    // ─── Generar mora al pagar letra vencida (llamado desde PagoLetraServiceImpl) ──

    @Transactional
    public MoraLetra generarMoraParaPago(LetraCambio letra, PagoLetras pagoLetra, LocalDate fechaReferencia) {
        LocalDate fechaVenc = letra.getFechaVencimiento();
        LocalDate fechaVencEfectiva = aplicarGraciaDominical(fechaVenc);

        // Si el pago se registra con fecha <= vencimiento efectivo, no corresponde mora.
        // Cancelamos cualquier mora PENDIENTE preexistente (pudo crearse por alerta).
        if (!fechaReferencia.isAfter(fechaVencEfectiva)) {
            cancelarMoraExistenteSiHay(letra.getIdLetra());
            return null;
        }

        // ── Recalcular días/montos con la fecha real del pago ─────────────────
        long dias = ChronoUnit.DAYS.between(fechaVenc, fechaReferencia);
        BigDecimal montoPct;
        BigDecimal montoDiar;
        BigDecimal total;
        if (tipoCalculoMora() == TipoCalculoMora.INTERES_DIARIO) {
            montoPct  = BigDecimal.ZERO;
            montoDiar = letra.getImporte().multiply(tasaDiaria()).multiply(BigDecimal.valueOf(dias)).setScale(2, RoundingMode.HALF_UP);
            total = montoDiar;
        } else {
            montoPct  = letra.getImporte().multiply(porcentajeMora()).setScale(2, RoundingMode.HALF_UP);
            montoDiar = montoDiario().multiply(BigDecimal.valueOf(dias)).setScale(2, RoundingMode.HALF_UP);
            total = montoPct.add(montoDiar);
        }

        // ── Buscar mora existente en cualquier estado activo ──────────────────
        boolean yaExiste = moraRepository.existeMoraActivaParaLetra(letra.getIdLetra());

        if (yaExiste) {
            Optional<MoraLetra> moraPagada = moraRepository
                .findByLetraIdLetraAndEstadoMora(letra.getIdLetra(), EstadoMora.PAGADO);
            if (moraPagada.isPresent()) {
                MoraLetra mora = moraPagada.get();
                long diasCorrectos = ChronoUnit.DAYS.between(fechaVenc, fechaReferencia);
                if (mora.getDiasMora() != (int) diasCorrectos) {
                    BigDecimal montoDiarCorr;
                    BigDecimal totalCorr;
                    if (tipoCalculoMora() == TipoCalculoMora.INTERES_DIARIO) {
                        montoDiarCorr = letra.getImporte().multiply(tasaDiaria()).multiply(BigDecimal.valueOf(diasCorrectos)).setScale(2, RoundingMode.HALF_UP);
                        totalCorr = montoDiarCorr;
                    } else {
                        montoDiarCorr = montoDiario().multiply(BigDecimal.valueOf(diasCorrectos)).setScale(2, RoundingMode.HALF_UP);
                        totalCorr = mora.getMontoPorcentaje().add(montoDiarCorr);
                    }
                    mora.setDiasMora((int) diasCorrectos);
                    mora.setMontoDiario(montoDiarCorr);
                    mora.setMontoMoraTotal(totalCorr);
                    mora.setFechaGeneracion(fechaReferencia);
                    mora.setPagoLetra(pagoLetra);
                    moraRepository.save(mora);
                }
                return mora;
            }

            Optional<MoraLetra> moraPendiente = moraRepository
                .findByLetraIdLetraAndEstadoMora(letra.getIdLetra(), EstadoMora.PENDIENTE);
            if (moraPendiente.isPresent()) {
                MoraLetra mora = moraPendiente.get();
                mora.setDiasMora((int) dias);
                mora.setMontoPorcentaje(montoPct);
                mora.setMontoDiario(montoDiar);
                mora.setMontoMoraTotal(total);
                mora.setFechaGeneracion(fechaReferencia);
                mora.setPagoLetra(pagoLetra);
                return moraRepository.save(mora);
            }
        }

        // ── No existe mora activa → crear nueva ───────────────────────────────
        MoraLetra mora = new MoraLetra();
        mora.setLetra(letra);
        mora.setPagoLetra(pagoLetra);
        mora.setDiasMora((int) dias);
        mora.setPorcentajeAplicado(porcentajeMora());
        mora.setMontoPorcentaje(montoPct);
        mora.setMontoDiario(montoDiar);
        mora.setMontoMoraTotal(total);
        mora.setFechaGeneracion(fechaReferencia);
        mora.setFechaVencimientoLetra(fechaVenc);
        mora.setEstadoMora(EstadoMora.PENDIENTE);

        return moraRepository.save(mora);
    }

    // ─── Consultas ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MoraResponseDTO> listarPorContrato(Integer idContrato) {
        return moraRepository.findByContratoIdContrato(idContrato)
            .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MoraResponseDTO> listarPendientesPorContrato(Integer idContrato) {
        return moraRepository.findByContratoIdContratoAndEstado(idContrato, EstadoMora.PENDIENTE)
            .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MoraResponseDTO> listarPorLetra(Integer idLetra) {
        return moraRepository.findByLetraIdLetra(idLetra)
            .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MoraResponseDTO obtenerPorId(Integer idMora) {
        return mapToDTO(moraRepository.findById(idMora)
            .orElseThrow(() -> new NegocioException("Mora no encontrada con id: " + idMora)));
    }

    @Override
    @Transactional(readOnly = true)
    public MoraResumenContratoDTO obtenerResumenPorContrato(Integer idContrato) {
        List<MoraLetra> pendientes = moraRepository
            .findByContratoIdContratoAndEstado(idContrato, EstadoMora.PENDIENTE);
        // Suma de saldos (no de totales): respeta pagos parciales y descuentos
        BigDecimal total = pendientes.stream()
            .map(this::saldoMora).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MoraResumenContratoDTO(idContrato, pendientes.size(), total);
    }

    // ─── Pago de mora ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PagoMoraResponseDTO pagarMora(PagoMoraRequestDTO request, List<MultipartFile> vouchers) throws IOException {
        MoraLetra mora = moraRepository.findByIdWithLock(request.getIdMora())
            .orElseThrow(() -> new NegocioException("Mora no encontrada con id: " + request.getIdMora()));

        if (mora.getEstadoMora() == EstadoMora.PAGADO)
            throw new NegocioException("Esta mora ya fue pagada.");
        if (mora.getEstadoMora() == EstadoMora.ANULADO)
            throw new NegocioException("Esta mora está anulada y no puede pagarse.");

        // Pago parcial permitido: cualquier monto entre 0 (exclusivo) y el saldo.
        BigDecimal saldo = saldoMora(mora);
        if (saldo.signum() <= 0)
            throw new NegocioException("Esta mora ya fue pagada.");
        if (request.getMontoPagado() == null || request.getMontoPagado().signum() <= 0)
            throw new NegocioException("El monto pagado debe ser mayor a cero.");
        if (request.getMontoPagado().compareTo(saldo) > 0) {
            throw new NegocioException(
                "El monto pagado (" + request.getMontoPagado() + ") no puede superar " +
                "el saldo pendiente de la mora (" + saldo + ")."
            );
        }

        if (MedioPagoUtil.esBancario(request.getMedioPago()) && request.getFechaOperacion() == null) {
            throw new NegocioException(
                "Para pagos con " + request.getMedioPago() + " la fecha de operación es obligatoria.");
        }

        PagoMora pago = new PagoMora();
        pago.setMora(mora);
        pago.setImportePagado(request.getMontoPagado());
        LocalDate fechaPagoMora = request.getFechaPago() != null ? request.getFechaPago() : LocalDate.now();
        pago.setFechaPago(FechasUtil.aFechaHora(fechaPagoMora));
        pago.setFechaOperacion(request.getFechaOperacion() != null
            ? FechasUtil.aFechaHora(request.getFechaOperacion(), request.getHoraOperacion()) : null);
        pago.setMedioPago(request.getMedioPago());
        pago.setNumeroOperacion(request.getNumeroOperacion());
        pago.setObservaciones(request.getObservaciones());

        // ── Comprobante (enviar a APIPERU antes de guardar si es BOLETA) ───────
        Comprobante comprobante = null;
        Map<String, Object> sunatRespuesta = null;

        if (request.getTipoComprobante() != null) {
            comprobante = comprobanteService.generarComprobanteConNumero(
                request.getTipoComprobante(),
                TipoOrigenComprobante.PAGO_MORA,
                null, // id temporal
                request.getMontoPagado(),
                fechaPagoMora,
                request.getNumeroComprobantePersonalizado()
            );

            // Si es BOLETA, enviar a APIPERU ANTES de guardar en BD
            if (request.getTipoComprobante() == TipoComprobante.BOLETA) {
                Cliente cliente = mora.getLetra().getContrato().getClientes().iterator().next().getCliente();
                String descripcion = "Pago de mora";
                comprobante.setDescripcion(descripcion);
                sunatRespuesta = sunatEnvioService.enviarBoleta(cliente, mora.getLetra().getContrato(),
                        comprobante, request.getMontoPagado(), descripcion,
                        request.getNumeroOperacion());
                
                // Manejar respuesta de SUNAT
                String estadoSunat = sunatRespuesta != null
                        ? (String) sunatRespuesta.get("estadoSunat") : null;
                String codigoError = sunatRespuesta != null
                        ? (String) sunatRespuesta.get("codigoError") : null;
                
                if ("ERROR".equals(estadoSunat)) {
                    if ("0111".equals(codigoError)) {
                        comprobante.setEstadoSunat("RECHAZADA");
                        comprobante.setSunatError("0111");
                        log.warn("Boleta mora {} RECHAZADA (0111). Se reintentará.",
                                comprobante.getNumeroCompleto());
                    } else {
                        String msg = (String) sunatRespuesta.getOrDefault("mensaje", "Error SUNAT");
                        throw new NegocioException("SUNAT rechazó la boleta: " + msg);
                    }
                } else if ("ACEPTADA".equals(estadoSunat)) {
                    comprobante.setEstadoSunat("ACEPTADA");
                    String hash = (String) sunatRespuesta.get("hash");
                    String cdrZip = (String) sunatRespuesta.get("cdrZip");
                    if (hash != null && !hash.isBlank()) comprobante.setHashCdr(hash);
                    if (cdrZip != null && !cdrZip.isBlank()) comprobante.setCdrBase64(cdrZip);
                } else {
                    comprobante.setEstadoSunat("PENDIENTE");
                }
            }
        }

        PagoMora pagoGuardado = pagoMoraRepository.save(pago);

        if (comprobante != null) {
            comprobante.setReferenciaId(pagoGuardado.getIdPagoMora());
            comprobante = comprobanteRepository.save(comprobante);
            pagoGuardado.setComprobante(comprobante);
            pagoGuardado = pagoMoraRepository.save(pagoGuardado);
        }

        if (vouchers != null && !vouchers.isEmpty()) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
            for (MultipartFile archivo : vouchers) {
                String publicId = "mora-" + pagoGuardado.getIdPagoMora() + "-" + timestamp;
                Map<?, ?> result = cloudinary.uploader().upload(archivo.getBytes(),
                    ObjectUtils.asMap(
                        "folder", "vouchers/mora-" + pagoGuardado.getIdPagoMora(),
                        "public_id", publicId
                    ));
                Voucher v = new Voucher();
                v.setTipoOrigen("PAGO_MORA");
                v.setReferenciaId(pagoGuardado.getIdPagoMora());
                v.setUrl(result.get("secure_url").toString());
                voucherRepository.save(v);
            }
        }

        // Estado según saldo restante (permite pago parcial: queda PENDIENTE)
        BigDecimal nuevoSaldo = saldo.subtract(request.getMontoPagado());
        mora.setEstadoMora(nuevoSaldo.signum() <= 0 ? EstadoMora.PAGADO : EstadoMora.PENDIENTE);
        moraRepository.save(mora);

        PagoMora finalPago = pagoGuardado;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificarAdminPagoMora(finalPago);
            }
        });

        PagoMoraResponseDTO dto = mapPagoToDTO(pagoGuardado);
        dto.setSaldoRestante(nuevoSaldo.signum() < 0 ? BigDecimal.ZERO : nuevoSaldo);
        if (sunatRespuesta != null) {
            String estadoSunat = (String) sunatRespuesta.get("estadoSunat");
            if ("ACEPTADA".equals(estadoSunat)) {
                dto.setSunatAceptado(true);
                dto.setSunatMensaje((String) sunatRespuesta.getOrDefault("mensaje", "ACEPTADA"));
            } else if ("ERROR".equals(estadoSunat) && "0111".equals(sunatRespuesta.get("codigoError"))) {
                dto.setSunatAceptado(false);
                dto.setSunatAdvertencia(
                    "Pago registrado. La boleta " + comprobante.getNumeroCompleto()
                    + " está pendiente de aceptación SUNAT (error 0111). Se reintentará automáticamente.");
            }
        }
        return dto;
    }

    // ─── Pago múltiple de moras (un comprobante compartido) ──────────────────

    @Override
    @Transactional
    public Map<String, Object> pagarMorasMultiples(MoraPagosMultiplesRequestDTO request,
                                                   List<MultipartFile> vouchers) throws IOException {
        if (request.getPagos() == null || request.getPagos().isEmpty())
            throw new NegocioException("La lista de moras no puede estar vacía.");
        if (request.getMedioPago() == null)
            throw new NegocioException("Debe indicar el medio de pago.");

        // ── Cargar y validar cada mora (lock pesimista, sin duplicados) ──────
        List<MoraLetra> moras = new ArrayList<>();
        Set<Integer> idsVistos = new HashSet<>();
        Integer idContrato = null;

        for (MoraPagosMultiplesRequestDTO.Item item : request.getPagos()) {
            if (item.getIdMora() == null)
                throw new NegocioException("Cada mora seleccionada debe tener un idMora.");
            if (!idsVistos.add(item.getIdMora()))
                throw new NegocioException("La mora con id " + item.getIdMora() +
                        " está repetida en la selección.");

            MoraLetra mora = moraRepository.findByIdWithLock(item.getIdMora())
                .orElseThrow(() -> new NegocioException("Mora no encontrada con id: " + item.getIdMora()));

            String refLetra = mora.getLetra() != null ? mora.getLetra().getNumeroLetra()
                    : String.valueOf(item.getIdMora());
            if (mora.getEstadoMora() == EstadoMora.PAGADO)
                throw new NegocioException("La mora de la letra N° " + refLetra + " ya fue pagada.");
            if (mora.getEstadoMora() == EstadoMora.ANULADO)
                throw new NegocioException("La mora de la letra N° " + refLetra + " está anulada.");

            BigDecimal saldo = saldoMora(mora);
            if (saldo.signum() <= 0)
                throw new NegocioException("La mora de la letra N° " + refLetra + " ya fue pagada.");
            if (item.getImportePagado() == null || item.getImportePagado().signum() <= 0)
                throw new NegocioException("El monto para la letra N° " + refLetra +
                        " debe ser mayor a cero.");
            if (item.getImportePagado().compareTo(saldo) > 0)
                throw new NegocioException("El monto para la letra N° " + refLetra + " (" +
                        item.getImportePagado() + ") no puede superar el saldo pendiente (" +
                        saldo + ").");

            Integer idC = mora.getLetra().getContrato().getIdContrato();
            if (idContrato == null) idContrato = idC;
            else if (!idContrato.equals(idC))
                throw new NegocioException("Todas las moras seleccionadas deben pertenecer al mismo contrato.");

            moras.add(mora);
        }

        // ── Descuento negociado (solo en pago múltiple, proporcional) ─────────
        BigDecimal descuentoTotal = request.getDescuentoNegociado() != null
                ? request.getDescuentoNegociado() : BigDecimal.ZERO;
        if (descuentoTotal.signum() < 0)
            throw new NegocioException("El descuento no puede ser negativo.");

        BigDecimal totalBruto = request.getPagos().stream()
                .map(MoraPagosMultiplesRequestDTO.Item::getImportePagado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (descuentoTotal.compareTo(totalBruto) > 0)
            throw new NegocioException("El descuento (" + descuentoTotal +
                    ") no puede superar el monto total del lote (" + totalBruto + ").");
        if (descuentoTotal.signum() > 0
                && (request.getMotivoDescuento() == null || request.getMotivoDescuento().isBlank()))
            throw new NegocioException("Debe indicar el motivo del descuento.");

        BigDecimal totalNeto = totalBruto.subtract(descuentoTotal);

        if (MedioPagoUtil.esBancario(request.getMedioPago()) && request.getFechaOperacion() == null)
            throw new NegocioException(
                "Para pagos con " + request.getMedioPago() + " la fecha de operación es obligatoria.");

        LocalDate fechaPago = request.getFechaPago() != null ? request.getFechaPago() : LocalDate.now();

        // ── Vouchers → Cloudinary (una vez; se replica por cada pago) ─────────
        List<String> urlsVoucher = new ArrayList<>();
        if (vouchers != null && !vouchers.isEmpty()) {
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
            int idx = 0;
            for (MultipartFile archivo : vouchers) {
                String publicId = "mora-multiple-" + timestamp + "-" + (idx++);
                Map<?, ?> result = cloudinary.uploader().upload(archivo.getBytes(),
                    ObjectUtils.asMap(
                        "folder", "vouchers/mora-multiple",
                        "public_id", publicId
                    ));
                urlsVoucher.add(result.get("secure_url").toString());
            }
        }

        // ── Comprobante compartido (igual que pagos múltiples de letras) ──────
        Comprobante comprobanteCompartido = null;
        if (request.getTipoComprobante() != null) {
            comprobanteCompartido = comprobanteService.generarComprobanteConNumeroY(
                request.getTipoComprobante(),
                TipoOrigenComprobante.PAGO_MORA,
                null,
                totalNeto,
                fechaPago,
                request.getNumeroComprobantePersonalizado(),
                null
            );
        }

        List<PagoMoraResponseDTO> responses = new ArrayList<>();

        for (int i = 0; i < request.getPagos().size(); i++) {
            MoraPagosMultiplesRequestDTO.Item item = request.getPagos().get(i);
            MoraLetra mora = moras.get(i);
            BigDecimal bruto = item.getImportePagado();

            // Descuento proporcional a lo bruto de cada mora (HALF_UP, centavos)
            BigDecimal descProporcionada = BigDecimal.ZERO;
            if (descuentoTotal.signum() > 0 && totalBruto.signum() > 0) {
                descProporcionada = descuentoTotal
                    .multiply(bruto)
                    .divide(totalBruto, 2, RoundingMode.HALF_UP);
            }
            BigDecimal neto = bruto.subtract(descProporcionada);
            BigDecimal saldoPrevio = saldoMora(mora);

            PagoMora pago = new PagoMora();
            pago.setMora(mora);
            pago.setImportePagado(neto);
            pago.setDescuentoAplicado(descProporcionada);
            pago.setFechaPago(FechasUtil.aFechaHora(fechaPago));
            pago.setFechaOperacion(request.getFechaOperacion() != null
                ? FechasUtil.aFechaHora(request.getFechaOperacion(), request.getHoraOperacion()) : null);
            pago.setMedioPago(request.getMedioPago());
            pago.setNumeroOperacion(request.getNumeroOperacion());
            pago.setObservaciones(construirObservacionesMora(
                request.getObservaciones(), descProporcionada, request.getMotivoDescuento()));
            pago.setComprobante(comprobanteCompartido);

            PagoMora guardado = pagoMoraRepository.save(pago);

            for (String url : urlsVoucher) {
                Voucher v = new Voucher();
                v.setTipoOrigen("PAGO_MORA");
                v.setReferenciaId(guardado.getIdPagoMora());
                v.setUrl(url);
                voucherRepository.save(v);
            }

            // Saldo por mora: bruto (neto + descuento) descuenta del saldo previo
            BigDecimal nuevoSaldo = saldoPrevio.subtract(bruto);
            if (nuevoSaldo.signum() < 0) nuevoSaldo = BigDecimal.ZERO;
            mora.setEstadoMora(nuevoSaldo.signum() <= 0 ? EstadoMora.PAGADO : EstadoMora.PENDIENTE);
            moraRepository.save(mora);

            PagoMora finalGuardado = guardado;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificarAdminPagoMora(finalGuardado);
                }
            });

            PagoMoraResponseDTO resp = mapPagoToDTO(guardado);
            resp.setSaldoRestante(nuevoSaldo);
            responses.add(resp);
        }

        // ── Asignar referenciaId al comprobante compartido ────────────────────
        if (comprobanteCompartido != null && comprobanteCompartido.getReferenciaId() == null
                && !responses.isEmpty()) {
            comprobanteCompartido.setReferenciaId(responses.get(0).getIdPagoMora());
            comprobanteRepository.save(comprobanteCompartido);
        }

        // ── SUNAT: boleta electrónica (serie B) con descripción combinada ─────
        Map<String, Object> sunatRespuestaMulti = null;
        if (comprobanteCompartido != null
                && comprobanteCompartido.getTipoComprobante() == TipoComprobante.BOLETA
                && comprobanteCompartido.getSerie() != null
                && comprobanteCompartido.getSerie().startsWith("B")) {

            Contrato contrato = moras.get(0).getLetra().getContrato();
            Cliente cliente = contrato.getClientes().iterator().next().getCliente();
            String descripcion = construirDescripcionMoraMulti(moras);
            comprobanteCompartido.setDescripcion(descripcion);

            sunatRespuestaMulti = sunatEnvioService.enviarBoleta(
                    cliente, contrato,
                    comprobanteCompartido, totalNeto, descripcion,
                    request.getNumeroOperacion());

            String estadoSunatMulti = sunatRespuestaMulti != null
                    ? (String) sunatRespuestaMulti.get("estadoSunat") : null;
            String codigoErrorMulti = sunatRespuestaMulti != null
                    ? (String) sunatRespuestaMulti.get("codigoError") : null;

            if ("ERROR".equals(estadoSunatMulti)) {
                if ("0111".equals(codigoErrorMulti)) {
                    comprobanteCompartido.setEstadoSunat("RECHAZADA");
                    comprobanteCompartido.setSunatError("0111");
                    log.warn("Boleta mora múltiple {} RECHAZADA (0111). Se reintentará.",
                            comprobanteCompartido.getNumeroCompleto());
                } else {
                    String msgMulti = (String) sunatRespuestaMulti.getOrDefault("mensaje", "Error SUNAT");
                    throw new NegocioException("SUNAT rechazó la boleta: " + msgMulti);
                }
            } else if ("ACEPTADA".equals(estadoSunatMulti)) {
                comprobanteCompartido.setEstadoSunat("ACEPTADA");
                String hash = (String) sunatRespuestaMulti.get("hash");
                String cdrZip = (String) sunatRespuestaMulti.get("cdrZip");
                if (hash != null && !hash.isBlank()) comprobanteCompartido.setHashCdr(hash);
                if (cdrZip != null && !cdrZip.isBlank()) comprobanteCompartido.setCdrBase64(cdrZip);
            } else {
                comprobanteCompartido.setEstadoSunat("PENDIENTE");
            }
        }

        // ── Poblar sunatAceptado/sunatAdvertencia en cada response ────────────
        if (sunatRespuestaMulti != null) {
            String estadoMulti = (String) sunatRespuestaMulti.get("estadoSunat");
            if ("ACEPTADA".equals(estadoMulti)) {
                for (PagoMoraResponseDTO r : responses) {
                    r.setSunatAceptado(true);
                    r.setSunatMensaje((String) sunatRespuestaMulti.getOrDefault("mensaje", "ACEPTADA"));
                }
            } else if ("ERROR".equals(estadoMulti) && "0111".equals(sunatRespuestaMulti.get("codigoError"))) {
                String advertencia = "Pago registrado. La boleta " + comprobanteCompartido.getNumeroCompleto()
                        + " está pendiente de aceptación SUNAT (error 0111). Se reintentará automáticamente.";
                for (PagoMoraResponseDTO r : responses) {
                    r.setSunatAceptado(false);
                    r.setSunatAdvertencia(advertencia);
                }
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("pagos", responses);
        resultado.put("numeroComprobanteGenerado",
            comprobanteCompartido != null ? comprobanteCompartido.getNumeroCompleto() : null);
        resultado.put("descuentoAplicado", descuentoTotal);
        resultado.put("montoTotalNeto", totalNeto);
        return resultado;
    }

    // ─── Anular mora (MoraLetra) ───────────────────────────────────────────────

    @Override
    @Transactional
    public MoraResponseDTO anularMora(Integer idMora, String motivo, String anuladoPor) {
        MoraLetra mora = moraRepository.findById(idMora)
            .orElseThrow(() -> new NegocioException("Mora no encontrada con id: " + idMora));

        if (mora.getEstadoMora() == EstadoMora.PAGADO)
            throw new NegocioException("No se puede anular una mora que ya fue pagada.");
        if (mora.getEstadoMora() == EstadoMora.ANULADO)
            throw new NegocioException("Esta mora ya está anulada.");
        if (saldoMora(mora).compareTo(montoTotalSeguro(mora)) < 0)
            throw new NegocioException(
                "No se puede anular una mora con pagos registrados. Anule primero los pagos.");

        mora.setEstadoMora(EstadoMora.ANULADO);
        mora.setMotivoAnulacion(motivo);
        mora.setFechaAnulacion(LocalDateTime.now());
        mora.setAnuladoPor(anuladoPor);
        moraRepository.save(mora);

        return mapToDTO(mora);
    }
    
    @Override
    @Transactional
    public void eliminarPagoMora(Integer idPagoMora) {
        PagoMora pago = pagoMoraRepository.findById(idPagoMora)
            .orElseThrow(() -> new NegocioException("Pago de mora no encontrado con id: " + idPagoMora));
 
        // Eliminar vouchers asociados
        voucherRepository
            .findByTipoOrigenAndReferenciaId("PAGO_MORA", pago.getIdPagoMora())
            .forEach(voucherRepository::delete);
 
        // Recalcular estado de la mora sin el pago eliminado (cubre pagos parciales)
        MoraLetra mora = pago.getMora();
        pagoMoraRepository.delete(pago);

        if (mora.getEstadoMora() != EstadoMora.ANULADO) {
            BigDecimal saldo = saldoDe(mora, mora.getPagos(), idPagoMora);
            EstadoMora nuevo = saldo.signum() <= 0 ? EstadoMora.PAGADO : EstadoMora.PENDIENTE;
            if (mora.getEstadoMora() != nuevo) {
                mora.setEstadoMora(nuevo);
                moraRepository.save(mora);
            }
        }
    }

    // ─── Anular pago de mora (PagoMora) ───────────────────────────────────────

    @Override
    @Transactional
    public PagoMoraResponseDTO anularPagoMora(Integer idPagoMora, String motivo, String anuladoPor) {
        PagoMora pago = pagoMoraRepository.findById(idPagoMora)
            .orElseThrow(() -> new NegocioException("Pago de mora no encontrado con id: " + idPagoMora));

        if (Boolean.TRUE.equals(pago.getAnulado()))
            throw new NegocioException("Este pago de mora ya fue anulado.");

        pago.setAnulado(true);
        pago.setMotivoAnulacion(motivo);
        pago.setFechaAnulacion(LocalDateTime.now());
        pago.setAnuladoPor(anuladoPor);
        pagoMoraRepository.save(pago);

        // Crear NC interna para comprobantes no SUNAT
        Comprobante orig = pago.getComprobante();
        if (orig != null && orig.getSerie() != null && !orig.getSerie().startsWith("B")) {
            Comprobante nc = comprobanteService.generarNotaCredito(
                    orig, "01", motivo, anuladoPor);
            orig.setIdNotaCreditoAnulacion(nc.getIdComprobante());
            comprobanteRepository.save(orig);
        }

        // Recalcular estado de la mora a partir del saldo (cubre pagos parciales)
        actualizarEstadoMora(pago.getMora());

        return mapPagoToDTO(pago);
    }

    // ─── Crear mora pendiente manualmente ─────────────────────────────────────

    @Override
    @Transactional
    public MoraResponseDTO crearMoraPendiente(Integer idLetra) {
        LetraCambio letra = letraCambioRepository.findById(idLetra)
            .orElseThrow(() -> new NegocioException("Letra no encontrada con id: " + idLetra));

        LocalDate hoy = LocalDate.now();
        LocalDate fechaVenc = letra.getFechaVencimiento();
        LocalDate fechaVencEfectiva = aplicarGraciaDominical(fechaVenc);

        if (!fechaVencEfectiva.isBefore(hoy))
            throw new NegocioException("La letra N° " + letra.getNumeroLetra() + " no está vencida. No aplica mora.");

        if (moraRepository.existeMoraActivaParaLetra(idLetra)) {
            Optional<MoraLetra> moraExistente = moraRepository
                .findByLetraIdLetraAndEstadoMora(idLetra, EstadoMora.PENDIENTE);
            if (moraExistente.isPresent()) return mapToDTO(moraExistente.get());
        }

        long dias = ChronoUnit.DAYS.between(fechaVenc, hoy);
        BigDecimal montoPct;
        BigDecimal montoDiar;
        BigDecimal total;
        if (tipoCalculoMora() == TipoCalculoMora.INTERES_DIARIO) {
            montoPct  = BigDecimal.ZERO;
            montoDiar = letra.getImporte().multiply(tasaDiaria()).multiply(BigDecimal.valueOf(dias)).setScale(2, RoundingMode.HALF_UP);
            total = montoDiar;
        } else {
            montoPct  = letra.getImporte().multiply(porcentajeMora()).setScale(2, RoundingMode.HALF_UP);
            montoDiar = montoDiario().multiply(BigDecimal.valueOf(dias)).setScale(2, RoundingMode.HALF_UP);
            total = montoPct.add(montoDiar);
        }

        MoraLetra mora = new MoraLetra();
        mora.setLetra(letra);
        mora.setPagoLetra(null);
        mora.setDiasMora((int) dias);
        mora.setPorcentajeAplicado(porcentajeMora());
        mora.setMontoPorcentaje(montoPct);
        mora.setMontoDiario(montoDiar);
        mora.setMontoMoraTotal(total);
        mora.setFechaGeneracion(hoy);
        mora.setFechaVencimientoLetra(fechaVenc);
        mora.setEstadoMora(EstadoMora.PENDIENTE);

        return mapToDTO(moraRepository.save(mora));
    }

    // ─── Helpers privados ──────────────────────────────────────────────────────

    /**
     * Saldo pendiente de una mora: montoMoraTotal − pagos activos
     * (importePagado neto + descuentoAplicado), mínimo 0.
     */
    private BigDecimal saldoMora(MoraLetra mora) {
        return saldoDe(mora, mora.getPagos(), null);
    }

    /** Igual que saldoMora pero excluyendo un pago (p. ej. el que se elimina). */
    private BigDecimal saldoDe(MoraLetra mora, java.util.Collection<PagoMora> pagos, Integer idExcluido) {
        BigDecimal total = montoTotalSeguro(mora);
        if (pagos == null || pagos.isEmpty()) return total;
        BigDecimal pagado = pagos.stream()
            .filter(p -> idExcluido == null || !idExcluido.equals(p.getIdPagoMora()))
            .filter(p -> !Boolean.TRUE.equals(p.getAnulado()))
            .map(p -> p.getImportePagado().add(
                    p.getDescuentoAplicado() != null ? p.getDescuentoAplicado() : BigDecimal.ZERO))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldo = total.subtract(pagado);
        return saldo.signum() < 0 ? BigDecimal.ZERO : saldo;
    }

    private BigDecimal montoTotalSeguro(MoraLetra mora) {
        return mora.getMontoMoraTotal() != null ? mora.getMontoMoraTotal() : BigDecimal.ZERO;
    }

    /** Recalcula el estado de la mora a partir de su saldo (PAGADO si saldo == 0). */
    private void actualizarEstadoMora(MoraLetra mora) {
        if (mora.getEstadoMora() == EstadoMora.ANULADO) return;
        EstadoMora nuevo = saldoMora(mora).signum() <= 0 ? EstadoMora.PAGADO : EstadoMora.PENDIENTE;
        if (mora.getEstadoMora() != nuevo) {
            mora.setEstadoMora(nuevo);
            moraRepository.save(mora);
        }
    }

    private String construirObservacionesMora(String obsBase, BigDecimal descuento, String motivoDescuento) {
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

    /**
     * Descripción SUNAT para boleta de mora múltiple:
     * "MORA DE LAS LETRAS 75, 76 Y 77 POR LA COMPRA DE ..." (misma ramificación
     * de marca que los pagos múltiples de letras).
     */
    private String construirDescripcionMoraMulti(List<MoraLetra> moras) {
        List<String> numsLimpios = new ArrayList<>();
        for (MoraLetra m : moras) {
            String n = m.getLetra().getNumeroLetra();
            if (n != null && n.contains("/")) n = n.substring(0, n.indexOf('/'));
            if (n != null && !n.isBlank()) numsLimpios.add(n);
        }
        String letrasStr;
        if (numsLimpios.size() <= 1) {
            letrasStr = "MORA LETRA " + (numsLimpios.isEmpty() ? "-" : numsLimpios.get(0));
        } else if (numsLimpios.size() == 2) {
            letrasStr = "MORA LETRAS " + numsLimpios.get(0) + " Y " + numsLimpios.get(1);
        } else {
            StringBuilder sb = new StringBuilder("MORA DE LAS LETRAS ");
            for (int i = 0; i < numsLimpios.size() - 1; i++) {
                sb.append(numsLimpios.get(i));
                if (i < numsLimpios.size() - 2) sb.append(", ");
            }
            sb.append(" Y ").append(numsLimpios.get(numsLimpios.size() - 1));
            letrasStr = sb.toString();
        }

        Contrato contrato = moras.get(0).getLetra().getContrato();
        String nombrePrograma = "";
        String mz = "";
        String lt = "";
        if (contrato.getLotes() != null && !contrato.getLotes().isEmpty()) {
            ContratoLote cl = contrato.getLotes().iterator().next();
            if (cl.getLote() != null) {
                mz = cl.getLote().getManzana();
                lt = cl.getLote().getNumeroLote();
                if (cl.getLote().getPrograma() != null)
                    nombrePrograma = cl.getLote().getPrograma().getNombrePrograma();
            }
        }

        if ("20552273223".equals(empresaService.obtenerActiva().getRuc())) {
            return "Pago de mora de " + numsLimpios.size() + " letras de la Mz. " + mz
                    + " Lt. " + lt + " del Programa: " + nombrePrograma.toUpperCase();
        }
        return letrasStr + " POR LA COMPRA DE UN LOTE DE TERRENO RUSTICO PROGRAMA DE VIV. "
                + nombrePrograma.toUpperCase();
    }

    private void cancelarMoraExistenteSiHay(Integer idLetra) {
        moraRepository.findByLetraIdLetraAndEstadoMora(idLetra, EstadoMora.PENDIENTE)
            .ifPresent(mora -> {
                mora.setEstadoMora(EstadoMora.ANULADO);
                moraRepository.save(mora);
            });
    }

    // ─── Notificación al admin ─────────────────────────────────────────────────

    private void notificarAdminPagoMora(PagoMora pago) {
        if (pago.getFechaPago() == null || !pago.getFechaPago().toLocalDate().equals(LocalDate.now())) return;
        try {
            var mora = pago.getMora();
            var letra = mora.getLetra();
            var contrato = letra.getContrato();
            Moneda moneda = contrato.getMoneda() != null ? contrato.getMoneda() : Moneda.USD;

            String clienteNombre = "-";
            if (contrato.getClientes() != null && !contrato.getClientes().isEmpty()) {
                var c = contrato.getClientes().iterator().next().getCliente();
                clienteNombre = c.getNombre() + " " + c.getApellidos();
            }

            String numLetra = letra.getNumeroLetra();
            if (numLetra != null && numLetra.contains("/")) {
                numLetra = numLetra.substring(0, numLetra.indexOf("/"));
            }

            String detalle = "Mora de letra N\u00B0 " + numLetra + " - " + mora.getDiasMora() + " d\u00EDas de atraso";

            String loteInfo = "";
            if (contrato.getLotes() != null && !contrato.getLotes().isEmpty()) {
                var lote = contrato.getLotes().iterator().next().getLote();
                if (lote != null) {
                    loteInfo = " Mz. " + lote.getManzana() + " Lt. " + lote.getNumeroLote();
                    if (lote.getPrograma() != null) {
                        loteInfo += " del Programa: " + lote.getPrograma().getNombrePrograma();
                    }
                }
            }
            detalle += loteInfo;

            String medioPago = pago.getMedioPago() != null ? pago.getMedioPago().name() : "-";
            notificacionAdminEmailService.notificarPagoMora(detalle, clienteNombre, pago.getImportePagado(), moneda, medioPago,
                    pago.getFechaPago(), pago.getFechaOperacion(), pago.getIdPagoMora());
        } catch (Exception e) {
            log.warn("No se pudo enviar notificacion admin para pago mora ID {}: {}", pago.getIdPagoMora(), e.getMessage());
        }
    }

    // ─── Mappers ───────────────────────────────────────────────────────────────

    private MoraResponseDTO mapToDTO(MoraLetra mora) {
        MoraResponseDTO dto = new MoraResponseDTO();
        dto.setIdMora(mora.getIdMora());
        dto.setIdLetra(mora.getLetra().getIdLetra());
        dto.setNumeroLetra(mora.getLetra().getNumeroLetra());
        dto.setImporteLetra(mora.getLetra().getImporte());
        dto.setFechaVencimientoLetra(mora.getFechaVencimientoLetra());
        dto.setIdPagoLetra(mora.getPagoLetra() != null ? mora.getPagoLetra().getIdPago() : null);
        dto.setDiasMora(mora.getDiasMora());
        dto.setPorcentajeAplicado(mora.getPorcentajeAplicado());
        dto.setMontoPorcentaje(mora.getMontoPorcentaje());
        dto.setMontoDiario(mora.getMontoDiario());
        dto.setMontoMoraTotal(mora.getMontoMoraTotal());
        dto.setFechaGeneracion(mora.getFechaGeneracion());
        dto.setEstadoMora(mora.getEstadoMora());
        dto.setSaldoPendiente(saldoMora(mora));
        // ── Anulación ─────────────────────────────────────────────────────────
        dto.setMotivoAnulacion(mora.getMotivoAnulacion());
        dto.setFechaAnulacion(mora.getFechaAnulacion());
        dto.setAnuladoPor(mora.getAnuladoPor());
        // ── Pagos ─────────────────────────────────────────────────────────────
        List<PagoMoraResponseDTO> pagos = mora.getPagos() == null ? List.of()
            : mora.getPagos().stream().map(this::mapPagoToDTO).collect(Collectors.toList());
        dto.setPagos(pagos);
        return dto;
    }

    private PagoMoraResponseDTO mapPagoToDTO(PagoMora pago) {
        return mapPagoToDTO(pago, null);
    }

    private PagoMoraResponseDTO mapPagoToDTO(PagoMora pago,
                                               Map<Integer, List<String>> vouchersPorPago) {
        PagoMoraResponseDTO dto = new PagoMoraResponseDTO();
        dto.setIdPagoMora(pago.getIdPagoMora());
        dto.setIdMora(pago.getMora().getIdMora());
        dto.setMontoPagado(pago.getImportePagado());
        dto.setDescuentoAplicado(pago.getDescuentoAplicado());
        dto.setFechaPago(pago.getFechaPago() != null ? pago.getFechaPago().toLocalDate() : null);
        dto.setMedioPago(pago.getMedioPago());
        dto.setNumeroOperacion(pago.getNumeroOperacion());
        dto.setObservaciones(pago.getObservaciones());

        if (pago.getComprobante() != null) {
            dto.setIdComprobante(pago.getComprobante().getIdComprobante());
            dto.setTipoComprobante(pago.getComprobante().getTipoComprobante());
            dto.setNumeroComprobante(pago.getComprobante().getNumeroCompleto());
            dto.setEstadoSunat(pago.getComprobante().getEstadoSunat());
        }

        if (vouchersPorPago != null) {
            dto.setUrlsVoucher(
                vouchersPorPago.getOrDefault(pago.getIdPagoMora(), List.of()));
        } else {
            List<String> urls = voucherRepository
                .findByTipoOrigenAndReferenciaId("PAGO_MORA", pago.getIdPagoMora())
                .stream().map(Voucher::getUrl).collect(Collectors.toList());
            dto.setUrlsVoucher(urls);
        }

        // Anulación
        dto.setAnulado(Boolean.TRUE.equals(pago.getAnulado()));
        dto.setMotivoAnulacion(pago.getMotivoAnulacion());
        dto.setFechaAnulacion(pago.getFechaAnulacion());
        dto.setAnuladoPor(pago.getAnuladoPor());

        // Contexto admin (manzana / lote / programa / cliente)
        // Solo se popula cuando el query hace JOIN FETCH hasta lotes y clientes
        // (usado por listarPagosTodos). En otros contextos estos campos quedan null.
        try {
            var contrato = pago.getMora().getLetra().getContrato();
            dto.setIdContrato(contrato.getIdContrato());
            if (contrato.getMoneda() != null) {
                dto.setMoneda(contrato.getMoneda().name());
            }
            var lotes = contrato.getLotes();
            if (lotes != null && !lotes.isEmpty()) {
                var lote = lotes.iterator().next().getLote();
                if (lote != null) {
                    dto.setManzana(lote.getManzana());
                    dto.setNumeroLote(lote.getNumeroLote());
                    if (lote.getPrograma() != null) {
                        dto.setIdPrograma(lote.getPrograma().getIdPrograma());
                        dto.setNombrePrograma(lote.getPrograma().getNombrePrograma());
                    }
                }
            }
            var clientes = contrato.getClientes();
            if (clientes != null && !clientes.isEmpty()) {
                var cc = clientes.iterator().next();
                if (cc.getCliente() != null) {
                    var cli = cc.getCliente();
                    dto.setNombreCliente(cli.getNombre() + " " + cli.getApellidos());
                }
            }
        } catch (Exception ignored) {
            // Contexto no disponible (lazy not loaded en otros endpoints)
        }

        return dto;
    }
    
    
    @Override
    @Transactional(readOnly = true)
    public List<PagoMoraResponseDTO> listarPagosTodos(
            String numeroComprobante,
            String manzana,
            String numeroLote,
            Integer idPrograma,
            LocalDate desde,
            LocalDate hasta) {

        List<PagoMora> pagos = pagoMoraRepository.findTodos(
                (numeroComprobante != null && !numeroComprobante.isBlank()) ? numeroComprobante : null,
                (manzana           != null && !manzana.isBlank())           ? manzana           : null,
                (numeroLote        != null && !numeroLote.isBlank())        ? numeroLote        : null,
                idPrograma,
                desde,
                hasta);

        if (pagos.isEmpty()) return List.of();

        // Batch-fetch vouchers para todos los pagos en 1 consulta
        List<Integer> pagoIds = pagos.stream()
                .map(PagoMora::getIdPagoMora)
                .collect(Collectors.toList());

        List<Voucher> vouchers = voucherRepository
                .findByTipoOrigenAndReferenciaIdIn("PAGO_MORA", pagoIds);

        Map<Integer, List<String>> vouchersPorPago = vouchers.stream()
                .collect(Collectors.groupingBy(
                        Voucher::getReferenciaId,
                        Collectors.mapping(Voucher::getUrl, Collectors.toList())));

        return pagos.stream()
                .map(p -> mapPagoToDTO(p, vouchersPorPago))
                .collect(Collectors.toList());
    }
    
}