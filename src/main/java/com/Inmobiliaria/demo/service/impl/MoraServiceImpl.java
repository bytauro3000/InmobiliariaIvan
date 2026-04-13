package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.dto.*;
import com.Inmobiliaria.demo.entity.*;
import com.Inmobiliaria.demo.enums.EstadoMora;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.LetraCambioRepository;
import com.Inmobiliaria.demo.repository.MoraRepository;
import com.Inmobiliaria.demo.repository.PagoMoraRepository;
import com.Inmobiliaria.demo.service.MoraService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MoraServiceImpl implements MoraService {

    private static final BigDecimal PORCENTAJE_MORA = new BigDecimal("0.05");  // 5%
    private static final BigDecimal MONTO_DIARIO    = new BigDecimal("1.00");  // $1 por día

    private final MoraRepository         moraRepository;
    private final PagoMoraRepository     pagoMoraRepository;
    private final LetraCambioRepository  letraCambioRepository;

    @Override
    @Transactional(readOnly = true)
    public CalculoMoraDTO calcularMora(Integer idLetra) {
        LetraCambio letra = letraCambioRepository.findById(idLetra)
                .orElseThrow(() -> new NegocioException("Letra no encontrada con id: " + idLetra));

        LocalDate hoy = LocalDate.now();
        LocalDate fechaVenc = letra.getFechaVencimiento();

        if (!fechaVenc.isBefore(hoy)) {
            throw new NegocioException(
                "La letra N° " + letra.getNumeroLetra() +
                " no está vencida. Vence el " + fechaVenc + ". No aplica mora."
            );
        }

        long dias = ChronoUnit.DAYS.between(fechaVenc, hoy);
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
                fechaVenc, hoy, (int) dias, montoPct, montoDiar, total, tienePrevia, idMoraPrevia);
    }

    // Genera mora al registrar el pago de una letra vencida.
    // - Si fechaReferencia <= fechaVencimiento: cancela mora existente, no genera nueva.
    // - Si ya existe mora PAGADA: no toca nada.
    // - Si ya existe mora PENDIENTE: solo la asocia al pago.
    // - Si no existe: crea mora nueva calculada con fechaReferencia.
    @Transactional
    public MoraLetra generarMoraParaPago(LetraCambio letra, PagoLetras pagoLetra, LocalDate fechaReferencia) {
        LocalDate fechaVenc = letra.getFechaVencimiento();

        if (!fechaReferencia.isAfter(fechaVenc)) {
            cancelarMoraExistenteSiHay(letra.getIdLetra());
            return null;
        }

        boolean yaExiste = moraRepository.existeMoraActivaParaLetra(letra.getIdLetra());
        if (yaExiste) {
            if (moraRepository.findByLetraIdLetraAndEstadoMora(letra.getIdLetra(), EstadoMora.PAGADO).isPresent()) {
                return null;
            }
            Optional<MoraLetra> moraExistente = moraRepository
                    .findByLetraIdLetraAndEstadoMora(letra.getIdLetra(), EstadoMora.PENDIENTE);
            if (moraExistente.isPresent()) {
                MoraLetra mora = moraExistente.get();
                mora.setPagoLetra(pagoLetra);
                return moraRepository.save(mora);
            }
        }

        long dias = ChronoUnit.DAYS.between(fechaVenc, fechaReferencia);
        BigDecimal montoPct  = letra.getImporte().multiply(PORCENTAJE_MORA).setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoDiar = MONTO_DIARIO.multiply(BigDecimal.valueOf(dias)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = montoPct.add(montoDiar);

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
        MoraLetra mora = moraRepository.findById(idMora)
                .orElseThrow(() -> new NegocioException("Mora no encontrada con id: " + idMora));
        return mapToDTO(mora);
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

    @Override
    @Transactional
    public PagoMoraResponseDTO pagarMora(PagoMoraRequestDTO request) {
        MoraLetra mora = moraRepository.findById(request.getIdMora())
                .orElseThrow(() -> new NegocioException("Mora no encontrada con id: " + request.getIdMora()));

        if (mora.getEstadoMora() == EstadoMora.PAGADO) throw new NegocioException("Esta mora ya fue pagada.");
        if (mora.getEstadoMora() == EstadoMora.ANULADO) throw new NegocioException("Esta mora está anulada y no puede pagarse.");

        if (request.getMontoPagado().compareTo(mora.getMontoMoraTotal()) != 0) {
            throw new NegocioException(
                "El monto pagado (" + request.getMontoPagado() + ") no coincide con " +
                "el total de la mora (" + mora.getMontoMoraTotal() + ")."
            );
        }

        if (request.getTipoComprobante() != null && request.getNumeroComprobante() != null) {
            boolean existe = pagoMoraRepository.existsByTipoComprobanteAndNumeroComprobante(
                    request.getTipoComprobante(), request.getNumeroComprobante());
            if (existe) throw new NegocioException("Ya existe un pago de mora con el mismo tipo y número de comprobante.");
        }

        PagoMora pago = new PagoMora();
        pago.setMora(mora);
        pago.setMontoPagado(request.getMontoPagado());
        pago.setFechaPago(request.getFechaPago());
        pago.setMedioPago(request.getMedioPago());
        pago.setNumeroOperacion(request.getNumeroOperacion());
        pago.setTipoComprobante(request.getTipoComprobante());
        pago.setNumeroComprobante(request.getNumeroComprobante());
        pago.setObservaciones(request.getObservaciones());

        PagoMora pagoGuardado = pagoMoraRepository.save(pago);
        mora.setEstadoMora(EstadoMora.PAGADO);
        moraRepository.save(mora);

        return mapPagoToDTO(pagoGuardado);
    }

    @Override
    @Transactional
    public MoraResponseDTO anularMora(Integer idMora, String motivo) {
        MoraLetra mora = moraRepository.findById(idMora)
                .orElseThrow(() -> new NegocioException("Mora no encontrada con id: " + idMora));

        if (mora.getEstadoMora() == EstadoMora.PAGADO)
            throw new NegocioException("No se puede anular una mora que ya fue pagada. Contacte al administrador.");
        if (mora.getEstadoMora() == EstadoMora.ANULADO)
            throw new NegocioException("Esta mora ya está anulada.");

        mora.setEstadoMora(EstadoMora.ANULADO);
        moraRepository.save(mora);
        return mapToDTO(mora);
    }

    @Override
    @Transactional
    public MoraResponseDTO crearMoraPendiente(Integer idLetra) {
        LetraCambio letra = letraCambioRepository.findById(idLetra)
                .orElseThrow(() -> new NegocioException("Letra no encontrada con id: " + idLetra));

        LocalDate hoy = LocalDate.now();
        LocalDate fechaVenc = letra.getFechaVencimiento();

        if (!fechaVenc.isBefore(hoy)) {
            throw new NegocioException("La letra N° " + letra.getNumeroLetra() + " no está vencida. No aplica mora.");
        }

        if (moraRepository.existeMoraActivaParaLetra(idLetra)) {
            Optional<MoraLetra> moraExistente = moraRepository
                    .findByLetraIdLetraAndEstadoMora(idLetra, EstadoMora.PENDIENTE);
            if (moraExistente.isPresent()) return mapToDTO(moraExistente.get());
        }

        long dias = ChronoUnit.DAYS.between(fechaVenc, hoy);
        BigDecimal montoPct  = letra.getImporte().multiply(PORCENTAJE_MORA).setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoDiar = MONTO_DIARIO.multiply(BigDecimal.valueOf(dias)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = montoPct.add(montoDiar);

        MoraLetra mora = new MoraLetra();
        mora.setLetra(letra);
        mora.setPagoLetra(null);
        mora.setDiasMora((int) dias);
        mora.setPorcentajeAplicado(PORCENTAJE_MORA);
        mora.setMontoPorcentaje(montoPct);
        mora.setMontoDiario(montoDiar);
        mora.setMontoMoraTotal(total);
        mora.setFechaGeneracion(hoy);
        mora.setFechaVencimientoLetra(fechaVenc);
        mora.setEstadoMora(EstadoMora.PENDIENTE);

        return mapToDTO(moraRepository.save(mora));
    }

    // Anula una mora PENDIENTE existente (se usa cuando el cliente pagó en fecha o antes)
    private void cancelarMoraExistenteSiHay(Integer idLetra) {
        moraRepository.findByLetraIdLetraAndEstadoMora(idLetra, EstadoMora.PENDIENTE)
                .ifPresent(mora -> {
                    mora.setEstadoMora(EstadoMora.ANULADO);
                    moraRepository.save(mora);
                });
    }

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
        dto.setMontoPagado(pago.getMontoPagado());
        dto.setFechaPago(pago.getFechaPago());
        dto.setMedioPago(pago.getMedioPago());
        dto.setNumeroOperacion(pago.getNumeroOperacion());
        dto.setTipoComprobante(pago.getTipoComprobante());
        dto.setNumeroComprobante(pago.getNumeroComprobante());
        dto.setObservaciones(pago.getObservaciones());
        return dto;
    }
}