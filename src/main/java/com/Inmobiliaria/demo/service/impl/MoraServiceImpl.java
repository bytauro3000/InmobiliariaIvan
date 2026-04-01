package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.dto.*;
import com.Inmobiliaria.demo.entity.*;
import com.Inmobiliaria.demo.enums.EstadoLetra;
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

    private final MoraRepository      moraRepository;
    private final PagoMoraRepository  pagoMoraRepository;
    private final LetraCambioRepository letraCambioRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // CÁLCULO PREVIO (sin persistir)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CalculoMoraDTO calcularMora(Integer idLetra) {
        LetraCambio letra = letraCambioRepository.findById(idLetra)
                .orElseThrow(() -> new NegocioException("Letra no encontrada con id: " + idLetra));

        LocalDate hoy = LocalDate.now();
        LocalDate fechaVenc = letra.getFechaVencimiento();

        // Solo se calcula mora si la letra está vencida
        if (!fechaVenc.isBefore(hoy)) {
            throw new NegocioException(
                "La letra N° " + letra.getNumeroLetra() +
                " no está vencida. Vence el " + fechaVenc + ". No aplica mora."
            );
        }

        long dias = ChronoUnit.DAYS.between(fechaVenc, hoy);
        BigDecimal montoPct  = letra.getImporte()
                .multiply(PORCENTAJE_MORA)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoDiar = MONTO_DIARIO
                .multiply(BigDecimal.valueOf(dias))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = montoPct.add(montoDiar);

        // Verificar si ya existe mora activa para esta letra
        boolean tienePrevia = moraRepository.existeMoraActivaParaLetra(idLetra);
        Integer idMoraPrevia = null;
        if (tienePrevia) {
            Optional<MoraLetra> moraOpt = moraRepository
                    .findByLetraIdLetraAndEstadoMora(idLetra, EstadoMora.PENDIENTE);
            idMoraPrevia = moraOpt.map(MoraLetra::getIdMora).orElse(null);
        }

        return new CalculoMoraDTO(
                idLetra,
                letra.getNumeroLetra(),
                letra.getImporte(),
                fechaVenc,
                hoy,
                (int) dias,
                montoPct,
                montoDiar,
                total,
                tienePrevia,
                idMoraPrevia
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GENERACIÓN DE MORA (llamado internamente desde PagoLetraServiceImpl)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Genera y persiste una MoraLetra cuando se paga una letra vencida.
     * Es llamado desde PagoLetraServiceImpl.registrarPago().
     *
     * Si ya existe una mora activa para la letra (no anulada), NO genera una nueva
     * — simplemente la asocia al pago de letra recibido.
     *
     * @param letra       La letra vencida que se está pagando
     * @param pagoLetra   El pago de letra recién registrado
     * @return            La MoraLetra generada o existente
     */
    @Transactional
    public MoraLetra generarMoraParaPago(LetraCambio letra, PagoLetras pagoLetra) {
        LocalDate hoy = LocalDate.now();
        LocalDate fechaVenc = letra.getFechaVencimiento();

        // Guardia: si la letra no está realmente vencida, no hacemos nada
        if (!fechaVenc.isBefore(hoy)) {
            return null;
        }

        // Si ya existe mora activa para esta letra, evaluamos su estado:
        // - PAGADO   → la mora ya fue cobrada; no hacemos NADA (retornamos null)
        // - PENDIENTE → solo asociamos el pago de letra y retornamos (no creamos otra)
        boolean yaExiste = moraRepository.existeMoraActivaParaLetra(letra.getIdLetra());
        if (yaExiste) {
            // Primero verificamos si está PAGADA → no tocar nada
            Optional<MoraLetra> moraPagada = moraRepository
                    .findByLetraIdLetraAndEstadoMora(letra.getIdLetra(), EstadoMora.PAGADO);
            if (moraPagada.isPresent()) {
                // La mora ya fue cobrada previamente desde mora-alerta; no crear duplicado
                return null;
            }
            // Si está PENDIENTE → asociamos el pago de letra (para trazabilidad) y retornamos
            Optional<MoraLetra> moraExistente = moraRepository
                    .findByLetraIdLetraAndEstadoMora(letra.getIdLetra(), EstadoMora.PENDIENTE);
            if (moraExistente.isPresent()) {
                MoraLetra mora = moraExistente.get();
                mora.setPagoLetra(pagoLetra);
                return moraRepository.save(mora);
            }
        }

        // Calcular mora
        long dias = ChronoUnit.DAYS.between(fechaVenc, hoy);
        BigDecimal montoPct  = letra.getImporte()
                .multiply(PORCENTAJE_MORA)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoDiar = MONTO_DIARIO
                .multiply(BigDecimal.valueOf(dias))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = montoPct.add(montoDiar);

        MoraLetra mora = new MoraLetra();
        mora.setLetra(letra);
        mora.setPagoLetra(pagoLetra);
        mora.setDiasMora((int) dias);
        mora.setPorcentajeAplicado(PORCENTAJE_MORA);
        mora.setMontoPorcentaje(montoPct);
        mora.setMontoDiario(montoDiar);
        mora.setMontoMoraTotal(total);
        mora.setFechaGeneracion(hoy);
        mora.setFechaVencimientoLetra(fechaVenc);
        mora.setEstadoMora(EstadoMora.PENDIENTE);  // pendiente hasta que el cliente la pague

        return moraRepository.save(mora);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONSULTAS
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MoraResponseDTO> listarPorContrato(Integer idContrato) {
        return moraRepository.findByContratoIdContrato(idContrato)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MoraResponseDTO> listarPendientesPorContrato(Integer idContrato) {
        return moraRepository
                .findByContratoIdContratoAndEstado(idContrato, EstadoMora.PENDIENTE)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MoraResponseDTO> listarPorLetra(Integer idLetra) {
        return moraRepository.findByLetraIdLetra(idLetra)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
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
                .map(MoraLetra::getMontoMoraTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new MoraResumenContratoDTO(idContrato, pendientes.size(), total);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PAGO DE MORA
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PagoMoraResponseDTO pagarMora(PagoMoraRequestDTO request) {
        MoraLetra mora = moraRepository.findById(request.getIdMora())
                .orElseThrow(() -> new NegocioException("Mora no encontrada con id: " + request.getIdMora()));

        if (mora.getEstadoMora() == EstadoMora.PAGADO) {
            throw new NegocioException("Esta mora ya fue pagada.");
        }
        if (mora.getEstadoMora() == EstadoMora.ANULADO) {
            throw new NegocioException("Esta mora está anulada y no puede pagarse.");
        }

        // Validar que el monto pagado coincida con el total de la mora
        if (request.getMontoPagado().compareTo(mora.getMontoMoraTotal()) != 0) {
            throw new NegocioException(
                "El monto pagado (" + request.getMontoPagado() + ") no coincide con " +
                "el total de la mora (" + mora.getMontoMoraTotal() + ")."
            );
        }

        // Validar unicidad del comprobante
        if (request.getTipoComprobante() != null && request.getNumeroComprobante() != null) {
            boolean existe = pagoMoraRepository.existsByTipoComprobanteAndNumeroComprobante(
                    request.getTipoComprobante(), request.getNumeroComprobante());
            if (existe) {
                throw new NegocioException(
                    "Ya existe un pago de mora con el mismo tipo y número de comprobante.");
            }
        }

        // Registrar pago
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

        // Marcar mora como PAGADA
        mora.setEstadoMora(EstadoMora.PAGADO);
        moraRepository.save(mora);

        return mapPagoToDTO(pagoGuardado);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ANULACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public MoraResponseDTO anularMora(Integer idMora, String motivo) {
        MoraLetra mora = moraRepository.findById(idMora)
                .orElseThrow(() -> new NegocioException("Mora no encontrada con id: " + idMora));

        if (mora.getEstadoMora() == EstadoMora.PAGADO) {
            throw new NegocioException(
                "No se puede anular una mora que ya fue pagada. Contacte al administrador.");
        }
        if (mora.getEstadoMora() == EstadoMora.ANULADO) {
            throw new NegocioException("Esta mora ya está anulada.");
        }

        mora.setEstadoMora(EstadoMora.ANULADO);
        moraRepository.save(mora);

        return mapToDTO(mora);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAPPERS PRIVADOS
    // ─────────────────────────────────────────────────────────────────────────

    private MoraResponseDTO mapToDTO(MoraLetra mora) {
        MoraResponseDTO dto = new MoraResponseDTO();
        dto.setIdMora(mora.getIdMora());
        dto.setIdLetra(mora.getLetra().getIdLetra());
        dto.setNumeroLetra(mora.getLetra().getNumeroLetra());
        dto.setImporteLetra(mora.getLetra().getImporte());
        dto.setFechaVencimientoLetra(mora.getFechaVencimientoLetra());
        dto.setIdPagoLetra(
            mora.getPagoLetra() != null ? mora.getPagoLetra().getIdPago() : null);
        dto.setDiasMora(mora.getDiasMora());
        dto.setPorcentajeAplicado(mora.getPorcentajeAplicado());
        dto.setMontoPorcentaje(mora.getMontoPorcentaje());
        dto.setMontoDiario(mora.getMontoDiario());
        dto.setMontoMoraTotal(mora.getMontoMoraTotal());
        dto.setFechaGeneracion(mora.getFechaGeneracion());
        dto.setEstadoMora(mora.getEstadoMora());

        List<PagoMoraResponseDTO> pagos = mora.getPagos() == null
                ? List.of()
                : mora.getPagos().stream()
                      .map(this::mapPagoToDTO)
                      .collect(Collectors.toList());
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
    
    
    @Override
    @Transactional
    public MoraResponseDTO crearMoraPendiente(Integer idLetra) {
        LetraCambio letra = letraCambioRepository.findById(idLetra)
                .orElseThrow(() -> new NegocioException("Letra no encontrada con id: " + idLetra));

        LocalDate hoy = LocalDate.now();
        LocalDate fechaVenc = letra.getFechaVencimiento();

        if (!fechaVenc.isBefore(hoy)) {
            throw new NegocioException(
                "La letra N° " + letra.getNumeroLetra() + " no está vencida. No aplica mora.");
        }

        // Si ya existe mora activa, retornarla en lugar de crear duplicado
        boolean yaExiste = moraRepository.existeMoraActivaParaLetra(idLetra);
        if (yaExiste) {
            Optional<MoraLetra> moraExistente = moraRepository
                    .findByLetraIdLetraAndEstadoMora(idLetra, EstadoMora.PENDIENTE);
            if (moraExistente.isPresent()) {
                return mapToDTO(moraExistente.get());
            }
        }

        long dias = ChronoUnit.DAYS.between(fechaVenc, hoy);
        BigDecimal montoPct  = letra.getImporte()
                .multiply(PORCENTAJE_MORA)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoDiar = MONTO_DIARIO
                .multiply(BigDecimal.valueOf(dias))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = montoPct.add(montoDiar);

        MoraLetra mora = new MoraLetra();
        mora.setLetra(letra);
        mora.setPagoLetra(null); // sin pago de letra asociado aún
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
}