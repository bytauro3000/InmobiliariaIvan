package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.dto.*;
import com.Inmobiliaria.demo.entity.*;
import com.Inmobiliaria.demo.enums.EstadoMora;
import com.Inmobiliaria.demo.enums.TipoOrigenComprobante;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.LetraCambioRepository;
import com.Inmobiliaria.demo.repository.MoraRepository;
import com.Inmobiliaria.demo.repository.PagoMoraRepository;
import com.Inmobiliaria.demo.repository.VoucherRepository;
import com.Inmobiliaria.demo.service.ComprobanteService;
import com.Inmobiliaria.demo.service.MoraService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
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

    private final MoraRepository        moraRepository;
    private final PagoMoraRepository    pagoMoraRepository;
    private final LetraCambioRepository letraCambioRepository;
    private final ComprobanteService    comprobanteService;
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

        if (!fechaVenc.isBefore(fechaReferencia)) {
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

        // Si el pago se registra con fecha <= vencimiento, no corresponde mora.
        // Cancelamos cualquier mora PENDIENTE preexistente (pudo crearse por alerta).
        if (!fechaReferencia.isAfter(fechaVenc)) {
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
        // existeMoraActivaParaLetra excluye ANULADO; puede ser PENDIENTE o PAGADO.
        boolean yaExiste = moraRepository.existeMoraActivaParaLetra(letra.getIdLetra());

        if (yaExiste) {
            // Caso 1: mora PAGADO → ya cobrada correctamente antes del pago de la letra.
            // FIX: en lugar de return null, verificamos si los días están mal y
            //      actualizamos los datos de la mora PAGADO para que quede correcto
            //      en el historial. No se modifica el monto (ya fue cobrado).
            Optional<MoraLetra> moraPagada = moraRepository
                .findByLetraIdLetraAndEstadoMora(letra.getIdLetra(), EstadoMora.PAGADO);
            if (moraPagada.isPresent()) {
                MoraLetra mora = moraPagada.get();
                // Solo corregimos días y fechaGeneracion si la mora fue calculada
                // con LocalDate.now() y los días no coinciden con la fechaReferencia real.
                // Esto corrige el historial sin alterar el monto ya cobrado.
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

            // Caso 2: mora PENDIENTE → recalcular con la fecha real del pago.
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

            // Caso 3: yaExiste == true pero no encontramos ni PENDIENTE ni PAGADO.
            // Puede ocurrir si solo hay registros ANULADO (raro, por inconsistencia en BD).
            // existeMoraActivaParaLetra filtra ANULADO, así que esto no debería pasar,
            // pero por seguridad creamos una mora nueva en lugar de retornar null.
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
        pago.setFechaPago(request.getFechaPago());
        pago.setMedioPago(request.getMedioPago());
        pago.setNumeroOperacion(request.getNumeroOperacion());
        pago.setObservaciones(request.getObservaciones());

        PagoMora pagoGuardado = pagoMoraRepository.save(pago);

        if (request.getTipoComprobante() != null) {
            Comprobante comprobante = comprobanteService.generarComprobanteConNumero(
                request.getTipoComprobante(),
                TipoOrigenComprobante.PAGO_MORA,
                pagoGuardado.getIdPagoMora(),
                request.getMontoPagado(),
                request.getFechaPago(),
                request.getNumeroComprobantePersonalizado()
            );
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

        return mapPagoToDTO(pagoGuardado);
    }

    // ─── Anular mora ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public MoraResponseDTO anularMora(Integer idMora, String motivo) {
        MoraLetra mora = moraRepository.findById(idMora)
            .orElseThrow(() -> new NegocioException("Mora no encontrada con id: " + idMora));

        if (mora.getEstadoMora() == EstadoMora.PAGADO)
            throw new NegocioException("No se puede anular una mora que ya fue pagada.");
        if (mora.getEstadoMora() == EstadoMora.ANULADO)
            throw new NegocioException("Esta mora ya está anulada.");

        mora.setEstadoMora(EstadoMora.ANULADO);
        moraRepository.save(mora);
        return mapToDTO(mora);
    }

    // ─── Crear mora pendiente manualmente ─────────────────────────────────────

    @Override
    @Transactional
    public MoraResponseDTO crearMoraPendiente(Integer idLetra) {
        LetraCambio letra = letraCambioRepository.findById(idLetra)
            .orElseThrow(() -> new NegocioException("Letra no encontrada con id: " + idLetra));

        LocalDate hoy = LocalDate.now();
        LocalDate fechaVenc = letra.getFechaVencimiento();

        if (!fechaVenc.isBefore(hoy))
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
        List<PagoMoraResponseDTO> pagos = mora.getPagos() == null ? List.of()
            : mora.getPagos().stream().map(this::mapPagoToDTO).collect(Collectors.toList());
        dto.setPagos(pagos);
        return dto;
    }

    private PagoMoraResponseDTO mapPagoToDTO(PagoMora pago) {
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

        List<String> urls = voucherRepository
            .findByTipoOrigenAndReferenciaId("PAGO_MORA", pago.getIdPagoMora())
            .stream().map(Voucher::getUrl).collect(Collectors.toList());
        dto.setUrlsVoucher(urls);

        return dto;
    }
}