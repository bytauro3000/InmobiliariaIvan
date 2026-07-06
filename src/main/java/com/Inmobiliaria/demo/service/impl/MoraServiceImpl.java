package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.dto.*;
import com.Inmobiliaria.demo.entity.*;
import com.Inmobiliaria.demo.enums.EstadoMora;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MoraServiceImpl implements MoraService {

    private static final BigDecimal PORCENTAJE_MORA = new BigDecimal("0.05");
    private static final BigDecimal MONTO_DIARIO    = new BigDecimal("1.00");

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
    private final Cloudinary            cloudinary;

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
        BigDecimal montoPct  = letra.getImporte().multiply(PORCENTAJE_MORA).setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoDiar = MONTO_DIARIO.multiply(BigDecimal.valueOf(dias)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = montoPct.add(montoDiar);

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
        BigDecimal montoPct  = letra.getImporte()
            .multiply(PORCENTAJE_MORA).setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoDiar = MONTO_DIARIO
            .multiply(BigDecimal.valueOf(dias)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = montoPct.add(montoDiar);

        // ── Buscar mora existente en cualquier estado activo ──────────────────
        boolean yaExiste = moraRepository.existeMoraActivaParaLetra(letra.getIdLetra());

        if (yaExiste) {
            Optional<MoraLetra> moraPagada = moraRepository
                .findByLetraIdLetraAndEstadoMora(letra.getIdLetra(), EstadoMora.PAGADO);
            if (moraPagada.isPresent()) {
                MoraLetra mora = moraPagada.get();
                long diasCorrectos = ChronoUnit.DAYS.between(fechaVenc, fechaReferencia);
                if (mora.getDiasMora() != (int) diasCorrectos) {
                    BigDecimal montoDiarCorr = MONTO_DIARIO
                        .multiply(BigDecimal.valueOf(diasCorrectos)).setScale(2, RoundingMode.HALF_UP);
                    mora.setDiasMora((int) diasCorrectos);
                    mora.setMontoDiario(montoDiarCorr);
                    mora.setMontoMoraTotal(mora.getMontoPorcentaje().add(montoDiarCorr));
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
        mora.setPorcentajeAplicado(PORCENTAJE_MORA);
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
        BigDecimal total = pendientes.stream()
            .map(MoraLetra::getMontoMoraTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MoraResumenContratoDTO(idContrato, pendientes.size(), total);
    }

    // ─── Pago de mora ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PagoMoraResponseDTO pagarMora(PagoMoraRequestDTO request, List<MultipartFile> vouchers) throws IOException {
        MoraLetra mora = moraRepository.findById(request.getIdMora())
            .orElseThrow(() -> new NegocioException("Mora no encontrada con id: " + request.getIdMora()));

        if (mora.getEstadoMora() == EstadoMora.PAGADO)
            throw new NegocioException("Esta mora ya fue pagada.");
        if (mora.getEstadoMora() == EstadoMora.ANULADO)
            throw new NegocioException("Esta mora está anulada y no puede pagarse.");

        if (request.getMontoPagado().compareTo(mora.getMontoMoraTotal()) != 0) {
            throw new NegocioException(
                "El monto pagado (" + request.getMontoPagado() + ") no coincide con " +
                "el total de la mora (" + mora.getMontoMoraTotal() + ")."
            );
        }

        PagoMora pago = new PagoMora();
        pago.setMora(mora);
        pago.setImportePagado(request.getMontoPagado());
        LocalDate fechaPagoMora = request.getFechaPago() != null ? request.getFechaPago() : LocalDate.now();
        pago.setFechaPago(fechaPagoMora);
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
                        comprobante, request.getMontoPagado(), descripcion);
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

        mora.setEstadoMora(EstadoMora.PAGADO);
        moraRepository.save(mora);

        PagoMoraResponseDTO dto = mapPagoToDTO(pagoGuardado);
        if (sunatRespuesta != null) {
            dto.setSunatAceptado(true);
            dto.setSunatMensaje((String) sunatRespuesta.getOrDefault("mensaje", "ACEPTADA"));
        }
        return dto;
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
 
        // Si la mora queda sin pagos activos → vuelve a PENDIENTE
        MoraLetra mora = pago.getMora();
        pagoMoraRepository.delete(pago);
 
        boolean tienePagosActivos = mora.getPagos().stream()
            .filter(p -> !p.getIdPagoMora().equals(idPagoMora))
            .anyMatch(p -> !Boolean.TRUE.equals(p.getAnulado()));
 
        if (!tienePagosActivos && mora.getEstadoMora() == EstadoMora.PAGADO) {
            mora.setEstadoMora(EstadoMora.PENDIENTE);
            moraRepository.save(mora);
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

        // Si todos los pagos de la mora están anulados → mora vuelve a PENDIENTE
        MoraLetra mora = pago.getMora();
        boolean todosAnulados = mora.getPagos().stream()
            .allMatch(p -> Boolean.TRUE.equals(p.getAnulado()));

        if (todosAnulados && mora.getEstadoMora() == EstadoMora.PAGADO) {
            mora.setEstadoMora(EstadoMora.PENDIENTE);
            moraRepository.save(mora);
        }

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
        BigDecimal montoPct  = letra.getImporte().multiply(PORCENTAJE_MORA).setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoDiar = MONTO_DIARIO.multiply(BigDecimal.valueOf(dias)).setScale(2, RoundingMode.HALF_UP);

        MoraLetra mora = new MoraLetra();
        mora.setLetra(letra);
        mora.setPagoLetra(null);
        mora.setDiasMora((int) dias);
        mora.setPorcentajeAplicado(PORCENTAJE_MORA);
        mora.setMontoPorcentaje(montoPct);
        mora.setMontoDiario(montoDiar);
        mora.setMontoMoraTotal(montoPct.add(montoDiar));
        mora.setFechaGeneracion(hoy);
        mora.setFechaVencimientoLetra(fechaVenc);
        mora.setEstadoMora(EstadoMora.PENDIENTE);

        return mapToDTO(moraRepository.save(mora));
    }

    // ─── Helpers privados ──────────────────────────────────────────────────────

    private void cancelarMoraExistenteSiHay(Integer idLetra) {
        moraRepository.findByLetraIdLetraAndEstadoMora(idLetra, EstadoMora.PENDIENTE)
            .ifPresent(mora -> {
                mora.setEstadoMora(EstadoMora.ANULADO);
                moraRepository.save(mora);
            });
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
        dto.setFechaPago(pago.getFechaPago());
        dto.setMedioPago(pago.getMedioPago());
        dto.setNumeroOperacion(pago.getNumeroOperacion());
        dto.setObservaciones(pago.getObservaciones());

        if (pago.getComprobante() != null) {
            dto.setIdComprobante(pago.getComprobante().getIdComprobante());
            dto.setTipoComprobante(pago.getComprobante().getTipoComprobante());
            dto.setNumeroComprobante(pago.getComprobante().getNumeroCompleto());
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