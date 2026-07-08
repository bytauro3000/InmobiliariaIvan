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
import com.Inmobiliaria.demo.repository.ComprobanteRepository;
import com.Inmobiliaria.demo.repository.*;
import com.Inmobiliaria.demo.service.ComprobanteService;
import com.Inmobiliaria.demo.service.PagoLetraService;
import com.Inmobiliaria.demo.service.SunatEnvioService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

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

    private static final Logger log = LoggerFactory.getLogger(PagoLetraServiceImpl.class);

    @Value("${app.pago.pin}")
    private String pagoPin;

    private final PagoLetraRepository   pagoLetraRepository;
    private final LetraCambioRepository letraCambioRepository;
    private final VoucherRepository     voucherRepository;
    private final ContratoRepository    contratoRepository;
    private final Cloudinary            cloudinary;
    private final MoraRepository        moraRepository;
    private final MoraServiceImpl       moraService;
    private final PagoMoraRepository    pagoMoraRepository;

    private final ComprobanteService           comprobanteService;
    private final ComprobanteRepository        comprobanteRepository;
    private final SunatEnvioService            sunatEnvioService;
    private final NotificacionAdminEmailService notificacionAdminEmailService;

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

    private void validarOrdenDePago(Integer idContrato, String numeroLetraStr, String pin) {
        int numLetraAPagar = extraerNumeroLetra(numeroLetraStr);
        Optional<Integer> maxPagadoOpt = pagoLetraRepository.findMaxNumeroLetraPagadoByContrato(idContrato);
        if (maxPagadoOpt.isEmpty() || maxPagadoOpt.get() == null) return; // primer pago, cualquier letra
        int maxPagado = maxPagadoOpt.get();

        if (numLetraAPagar == maxPagado + 1) return; // orden correcto

        // Fuera de orden (anterior o saltando): verificar PIN
        if (pin != null && pin.equals(pagoPin)) return;

        // Admin también puede saltarse la validación
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"))) return;

        throw new NegocioException(
            "FUERA_DE_ORDEN:Debe pagar la letra N° " + (maxPagado + 1) + "."
        );
    }

    private void validarOrdenDePagoMultiple(Integer idContrato, List<String> numerosLetra, String pin) {
        if (numerosLetra == null || numerosLetra.isEmpty()) return;
        List<Integer> nums = numerosLetra.stream()
            .map(this::extraerNumeroLetra).sorted().collect(Collectors.toList());

        for (int i = 1; i < nums.size(); i++) {
            if (nums.get(i) != nums.get(i - 1) + 1) {
                throw new NegocioException(
                    "Las letras seleccionadas no son consecutivas: N° " +
                    nums.get(i - 1) + " y N° " + nums.get(i) + ".");
            }
        }
    }


    private LocalDate resolverFechaReferenciaMora(int numLetraActual, Integer idContrato, LocalDate fechaOperacion) {
        if (fechaOperacion == null) {
            log.warn("resolverFechaReferenciaMora: fechaOperacion es null para letra {} del contrato {}. Usando LocalDate.now() como fallback.",
                numLetraActual, idContrato);
            return LocalDate.now();
        }
        return fechaOperacion;
    }

    private static boolean esMedioBancario(MedioPago medio) {
        return medio == MedioPago.DEPOSITO
            || medio == MedioPago.TRANSFERENCIA
            || medio == MedioPago.YAPE
            || medio == MedioPago.PLIN
            || medio == MedioPago.OTROS;
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
        return mapToDTO(pago, null);
    }

    private PagoLetraResponseDTO mapToDTO(PagoLetras pago,
                                           Map<Integer, List<String>> vouchersPorPago) {
        PagoLetraResponseDTO dto = new PagoLetraResponseDTO();
        dto.setIdPago(pago.getIdPago());
        dto.setIdLetra(pago.getLetra().getIdLetra());
        dto.setNumeroLetra(pago.getLetra().getNumeroLetra());
        dto.setFechaPago(pago.getFechaPago());
        dto.setFechaOperacion(pago.getFechaOperacion());
        dto.setImportePagado(pago.getImportePagado());
        dto.setMedioPago(pago.getMedioPago());
        dto.setNumeroOperacion(pago.getNumeroOperacion());
        dto.setObservaciones(pago.getObservaciones());
        dto.setEsPagoAcuenta(pago.getEsPagoAcuenta());

        dto.setImporteLetra(pago.getLetra().getImporte());
        dto.setSaldoPendiente(pago.getLetra().getSaldoPendiente());
        dto.setEstadoLetra(pago.getLetra().getEstadoLetra());

        if (pago.getComprobante() != null) {
            dto.setIdComprobante(pago.getComprobante().getIdComprobante());
            dto.setTipoComprobante(pago.getComprobante().getTipoComprobante());
            dto.setNumeroComprobante(pago.getComprobante().getNumeroCompleto());
            dto.setSunatHash(pago.getComprobante().getHashCdr());
        }

        // Vouchers: usa mapa pre-cargado si está disponible (evita N+1 en listarTodos)
        if (vouchersPorPago != null) {
            dto.setUrlsVoucher(
                vouchersPorPago.getOrDefault(pago.getIdPago(), List.of()));
        } else {
            List<String> urls = voucherRepository
                .findByTipoOrigenAndReferenciaId("PAGO_LETRA", pago.getIdPago())
                .stream().map(Voucher::getUrl).collect(Collectors.toList());
            dto.setUrlsVoucher(urls);
        }

        // Anulación
        dto.setAnulado(Boolean.TRUE.equals(pago.getAnulado()));
        dto.setMotivoAnulacion(pago.getMotivoAnulacion());
        dto.setFechaAnulacion(pago.getFechaAnulacion());
        dto.setAnuladoPor(pago.getAnuladoPor());
        if (pago.getComprobante() != null) {
            dto.setIdNotaCredito(pago.getComprobante().getIdNotaCreditoAnulacion());
        }

        // Contexto admin (manzana / lote / programa)
        // El nombre del cliente se asigna externamente en listarTodos.
        var lotes = pago.getLetra().getContrato().getLotes();
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
        dto.setIdContrato(pago.getLetra().getContrato().getIdContrato());
        if (pago.getLetra().getContrato().getMoneda() != null) {
            dto.setMoneda(pago.getLetra().getContrato().getMoneda().name());
        }

        return dto;
    }
 
 
    @Override
    @Transactional
    @CacheEvict(value = "contratos", allEntries = true)
    public PagoLetraResponseDTO anularPago(Integer idPago, String motivo, String anuladoPor) {
        PagoLetras pago = pagoLetraRepository.findById(idPago)
            .orElseThrow(() -> new NegocioException("Pago no encontrado con id: " + idPago));
 
        if (Boolean.TRUE.equals(pago.getAnulado()))
            throw new NegocioException("Este pago ya fue anulado.");
 
        pago.setAnulado(true);
        pago.setMotivoAnulacion(motivo);
        pago.setFechaAnulacion(LocalDateTime.now());
        pago.setAnuladoPor(anuladoPor);
        pagoLetraRepository.save(pago);
 
        // Recalcular saldo de la letra (igual que al eliminar, pero sin borrar)
        LetraCambio letra = pago.getLetra();
        BigDecimal totalPagado = pagoLetraRepository.sumImportePagadoActivoByLetra(letra.getIdLetra());
        if (totalPagado == null) totalPagado = BigDecimal.ZERO;
        BigDecimal nuevoSaldo = letra.getImporte().subtract(totalPagado);
 
        long count = pagoLetraRepository.countActivosByLetraIdLetra(letra.getIdLetra());
        if (count == 0) {
            letra.setSaldoPendiente(letra.getImporte());
            letra.setEstadoLetra(letra.getFechaVencimiento().isBefore(LocalDate.now())
                ? EstadoLetra.VENCIDO : EstadoLetra.PENDIENTE);
        } else {
            letra.setSaldoPendiente(nuevoSaldo);
            letra.setEstadoLetra(nuevoSaldo.compareTo(BigDecimal.ZERO) == 0
                ? EstadoLetra.PAGADO : EstadoLetra.PARCIAL);
        }
        letraCambioRepository.save(letra);
        recalcularEstadoContrato(letra.getContrato());
 
        return mapToDTO(pago);
    }

    @Override
    @Transactional
    public void anularPagoConMoras(Integer idPago, String motivo, String anuladoPor) {
        PagoLetras pago = pagoLetraRepository.findById(idPago)
            .orElseThrow(() -> new NegocioException("Pago no encontrado con id: " + idPago));

        if (Boolean.TRUE.equals(pago.getAnulado()))
            throw new NegocioException("Este pago ya fue anulado.");

        Comprobante comprobante = pago.getComprobante();
        List<PagoLetras> pagosAAnular;
        if (comprobante != null) {
            pagosAAnular = pagoLetraRepository
                .findByComprobanteIdComprobante(comprobante.getIdComprobante())
                .stream()
                .filter(p -> !Boolean.TRUE.equals(p.getAnulado()))
                .collect(Collectors.toList());
            if (pagosAAnular.isEmpty()) {
                pagosAAnular = List.of(pago);
            }
        } else {
            pagosAAnular = List.of(pago);
        }

        Set<Contrato> contratosAfectados = new HashSet<>();

        for (PagoLetras pagoAA : pagosAAnular) {
            // 1. Anular el pago
            pagoAA.setAnulado(true);
            pagoAA.setMotivoAnulacion(motivo);
            pagoAA.setFechaAnulacion(LocalDateTime.now());
            pagoAA.setAnuladoPor(anuladoPor);
            pagoLetraRepository.save(pagoAA);

            // 2. Restaurar la letra (saldo + estado)
            LetraCambio letra = pagoAA.getLetra();
            BigDecimal totalPagado = pagoLetraRepository.sumImportePagadoActivoByLetra(letra.getIdLetra());
            if (totalPagado == null) totalPagado = BigDecimal.ZERO;
            BigDecimal nuevoSaldo = letra.getImporte().subtract(totalPagado);

            long count = pagoLetraRepository.countActivosByLetraIdLetra(letra.getIdLetra());
            if (count == 0) {
                letra.setSaldoPendiente(letra.getImporte());
                letra.setEstadoLetra(letra.getFechaVencimiento().isBefore(LocalDate.now())
                    ? EstadoLetra.VENCIDO : EstadoLetra.PENDIENTE);
            } else {
                letra.setSaldoPendiente(nuevoSaldo);
                letra.setEstadoLetra(nuevoSaldo.compareTo(BigDecimal.ZERO) == 0
                    ? EstadoLetra.PAGADO : EstadoLetra.PARCIAL);
            }
            letraCambioRepository.save(letra);

            // 3. Anular moras generadas por este pago
            List<MoraLetra> moras = moraRepository.findByPagoLetraIdPago(pagoAA.getIdPago());
            for (MoraLetra mora : moras) {
                if (mora.getEstadoMora() == EstadoMora.ANULADO) continue;

                for (PagoMora pm : mora.getPagos()) {
                    if (Boolean.TRUE.equals(pm.getAnulado())) continue;
                    pm.setAnulado(true);
                    pm.setMotivoAnulacion(motivo);
                    pm.setFechaAnulacion(LocalDateTime.now());
                    pm.setAnuladoPor(anuladoPor);
                    pagoMoraRepository.save(pm);
                }

                mora.setEstadoMora(EstadoMora.ANULADO);
                mora.setMotivoAnulacion(motivo);
                mora.setFechaAnulacion(LocalDateTime.now());
                mora.setAnuladoPor(anuladoPor);
                moraRepository.save(mora);
            }

            contratosAfectados.add(letra.getContrato());
        }

        // 4. Recalcular estado de cada contrato afectado
        for (Contrato c : contratosAfectados) {
            recalcularEstadoContrato(c);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Vouchers
    // ═══════════════════════════════════════════════════════════════════════════

    private void notificarAdminPagoLetra(PagoLetras pago) {
        try {
            var letra = pago.getLetra();
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

            String detalle = "Pago de letra N\u00B0 " + numLetra;

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
            notificacionAdminEmailService.notificarPagoLetra(detalle, clienteNombre, pago.getImportePagado(), moneda, medioPago);
        } catch (Exception e) {
            log.warn("No se pudo enviar notificacion admin para pago ID {}: {}", pago.getIdPago(), e.getMessage());
        }
    }

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
        LetraCambio letra = letraCambioRepository.findByIdWithLock(request.getIdLetra())
            .orElseThrow(() -> new NegocioException("Letra no encontrada con id: " + request.getIdLetra()));

        if (letra.getEstadoLetra() == EstadoLetra.PAGADO)
            throw new NegocioException("La letra ya se encuentra pagada.");

        boolean esPagoAcuenta = Boolean.TRUE.equals(request.getEsPagoAcuenta());

        BigDecimal nuevoSaldo = validarYCalcularNuevoSaldo(letra, request.getImportePagado(), esPagoAcuenta);

        Integer idContrato = letra.getContrato().getIdContrato();

        if (letra.getEstadoLetra() != EstadoLetra.PARCIAL) {
            validarOrdenDePago(idContrato, letra.getNumeroLetra(), request.getPin());
        }

        // ── Fechas ───────────────────────────────────────────────────────────
        // fechaPago = fecha del día del pago (del request o hoy como fallback)
        // fechaOperacion = fecha del voucher (solo referencial, nullable)
        LocalDate fechaPago = request.getFechaPago() != null ? request.getFechaPago() : LocalDate.now();
        LocalDate fechaOperacion = request.getFechaOperacion();

        if (esMedioBancario(request.getMedioPago()) && fechaOperacion == null) {
            throw new NegocioException(
                "Para pagos con " + request.getMedioPago() + " la fecha de operación es obligatoria.");
        }

        // ── Construir el pago ────────────────────────────────────────────────
        PagoLetras pago = new PagoLetras();
        pago.setLetra(letra);
        pago.setFechaPago(fechaPago);
        pago.setFechaOperacion(fechaOperacion);
        pago.setImportePagado(request.getImportePagado());
        pago.setMedioPago(request.getMedioPago());
        pago.setNumeroOperacion(request.getNumeroOperacion());
        pago.setObservaciones(request.getObservaciones());
        pago.setEsPagoAcuenta(esPagoAcuenta);
        pago.setDescuentoAplicado(BigDecimal.ZERO);
        pago.setEsLetraGratis(false);

        // ── Comprobante (enviar a APIPERU antes de guardar si es BOLETA) ───────
        Comprobante comprobante = null;
        Map<String, Object> sunatRespuesta = null;

        if (request.getTipoComprobante() != null) {
            comprobante = comprobanteService.generarComprobanteConNumeroY(
                request.getTipoComprobante(),
                TipoOrigenComprobante.PAGO_LETRA,
                null, // id temporal, se asigna después de guardar pago
                request.getImportePagado(),
                fechaPago,
                request.getNumeroComprobantePersonalizado(),
                request.getSeriePersonalizada()
            );

            // Si es BOLETA con serie B (B001), enviar a APIPERU ANTES de guardar en BD
            // Serie E (EB01) se registra localmente como RECIBO
            if (request.getTipoComprobante() == TipoComprobante.BOLETA
                && comprobante.getSerie() != null
                && comprobante.getSerie().startsWith("B")) {
                Cliente cliente = letra.getContrato().getClientes().iterator().next().getCliente();
                
                String numeroLetra = letra.getNumeroLetra();
                if (numeroLetra != null && numeroLetra.contains("/")) {
                    numeroLetra = numeroLetra.substring(0, numeroLetra.indexOf("/"));
                }
                
                String nombrePrograma = "";
                if (letra.getContrato().getLotes() != null && !letra.getContrato().getLotes().isEmpty()) {
                    ContratoLote primerLote = letra.getContrato().getLotes().iterator().next();
                    if (primerLote.getLote() != null && primerLote.getLote().getPrograma() != null) {
                        nombrePrograma = primerLote.getLote().getPrograma().getNombrePrograma();
                    }
                }
                
                String descripcion = "LETRA " + numeroLetra + " POR LA COMPRA DE UN LOTE DE TERRENO RUSTICO PROGRAMA DE VIV. " + nombrePrograma.toUpperCase();
                comprobante.setDescripcion(descripcion);
                sunatRespuesta = sunatEnvioService.enviarBoleta(
                        cliente, letra.getContrato(), comprobante,
                        request.getImportePagado(), descripcion);
                
                // Si SUNAT aceptó, guardar hash y CDR en el comprobante
                if (sunatRespuesta != null && "ACEPTADA".equals(sunatRespuesta.get("estadoSunat"))) {
                    String hash = (String) sunatRespuesta.get("hash");
                    String cdrZip = (String) sunatRespuesta.get("cdrZip");
                    if (hash != null && !hash.isBlank()) {
                        comprobante.setHashCdr(hash);
                    }
                    if (cdrZip != null && !cdrZip.isBlank()) {
                        comprobante.setCdrBase64(cdrZip);
                    }
                }
                // Si APIPERU rechaza, lanza excepción y @Transactional hace rollback
            }
        }

        PagoLetras pagoGuardado = pagoLetraRepository.save(pago);

        if (comprobante != null) {
            comprobante.setReferenciaId(pagoGuardado.getIdPago());
            comprobante = comprobanteRepository.save(comprobante);
            pagoGuardado.setComprobante(comprobante);
            pagoGuardado = pagoLetraRepository.save(pagoGuardado);
        }

        guardarVouchers(vouchers, pagoGuardado, idContrato, letra.getIdLetra());

        // ── Actualizar saldo y estado de la letra ────────────────────────────
        letra.setSaldoPendiente(nuevoSaldo);

        if (nuevoSaldo.compareTo(BigDecimal.ZERO) == 0) {
            // Pago completado: generar/recalcular mora si la letra estaba vencida.
            // FIX: también se ejecuta para letras en estado PENDIENTE cuya fecha de
            //      vencimiento ya pasó pero que el scheduler no marcó como VENCIDO aún.
            //      generarMoraParaPago verifica internamente si fechaRef > fechaVenc.
            boolean letraEsVencida = letra.getEstadoLetra() == EstadoLetra.VENCIDO
                || (letra.getFechaVencimiento() != null
                    && MoraServiceImpl.aplicarGraciaDominical(letra.getFechaVencimiento()).isBefore(fechaPago));

            if (letraEsVencida) {
                int numLetra = extraerNumeroLetra(letra.getNumeroLetra());
                LocalDate fechaRef = esMedioBancario(request.getMedioPago())
                    ? fechaOperacion
                    : resolverFechaReferenciaMora(numLetra, idContrato, fechaPago);
                moraService.generarMoraParaPago(letra, pagoGuardado, fechaRef);
            }
            letra.setEstadoLetra(EstadoLetra.PAGADO);
        } else {
            letra.setEstadoLetra(EstadoLetra.PARCIAL);
        }

        letraCambioRepository.save(letra);
        verificarYActualizarEstadoContrato(letra.getContrato());

        PagoLetras finalPago = pagoGuardado;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificarAdminPagoLetra(finalPago);
            }
        });

        PagoLetraResponseDTO dto = mapToDTO(pagoGuardado);
        if (sunatRespuesta != null) {
            dto.setSunatAceptado(true);
            dto.setSunatMensaje((String) sunatRespuesta.getOrDefault("mensaje", "ACEPTADA"));
        }
        return dto;
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
        LetraCambio letraEjemplo = letraCambioRepository.findByIdWithLock(idPrimeraLetra)
            .orElseThrow(() -> new NegocioException("Letra no encontrada: " + idPrimeraLetra));
        Integer idContrato = letraEjemplo.getContrato().getIdContrato();

        List<LetraCambio> letrasDelLote = new ArrayList<>();
        List<String> numerosLetraRequest = new ArrayList<>();
        for (PagoLetraRequestDTO req : request.getPagos()) {
            LetraCambio lc = letraCambioRepository.findByIdWithLock(req.getIdLetra())
                .orElseThrow(() -> new NegocioException("Letra no encontrada: " + req.getIdLetra()));
            letrasDelLote.add(lc);
            numerosLetraRequest.add(lc.getNumeroLetra());
        }
        String pin = request.getPagos() != null && !request.getPagos().isEmpty()
                ? request.getPagos().get(0).getPin() : null;
        validarOrdenDePagoMultiple(idContrato, numerosLetraRequest, pin);

        LetraCambio letraGratis = null;
        if (request.getIdLetraGratis() != null) {
            letraGratis = validarLetraGratis(
                request.getIdLetraGratis(), idContrato, letrasDelLote.size());
        }

        BigDecimal descuentoTotal = request.getDescuentoNegociado();
        if (descuentoTotal == null) descuentoTotal = BigDecimal.ZERO;

        BigDecimal montoTotalBruto = request.getPagos().stream()
            .map(PagoLetraRequestDTO::getImportePagado)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (descuentoTotal.compareTo(montoTotalBruto) > 0)
            throw new NegocioException("El descuento ($" + descuentoTotal +
                ") no puede superar el monto total del lote ($" + montoTotalBruto + ").");

        BigDecimal montoTotalNeto = montoTotalBruto.subtract(descuentoTotal);

        List<String> urlsVoucher = new ArrayList<>();
        if (vouchers != null && !vouchers.isEmpty()) {
            for (MultipartFile v : vouchers)
                urlsVoucher.add(subirImagen(v, idContrato, null));
        }

        PagoLetraRequestDTO primerPago = request.getPagos().get(0);
        // fechaPago = fecha del día del pago (del request o hoy como fallback)
        LocalDate fechaPago = primerPago.getFechaPago() != null ? primerPago.getFechaPago() : LocalDate.now();

        Comprobante comprobanteCompartido = null;
        if (primerPago.getTipoComprobante() != null) {
            comprobanteCompartido = comprobanteService.generarComprobanteConNumeroY(
                primerPago.getTipoComprobante(),
                TipoOrigenComprobante.PAGO_LETRA,
                null,
                montoTotalNeto,
                fechaPago,
                primerPago.getNumeroComprobantePersonalizado(),
                primerPago.getSeriePersonalizada()
            );
        }

        List<PagoLetraResponseDTO> responses = new ArrayList<>();

        for (int i = 0; i < request.getPagos().size(); i++) {
            PagoLetraRequestDTO pagoReq = request.getPagos().get(i);
            LetraCambio letra = letrasDelLote.get(i);

            if (letra.getEstadoLetra() == EstadoLetra.PAGADO)
                throw new NegocioException("La letra " + letra.getNumeroLetra() + " ya está pagada.");

            if (letraGratis != null && letra.getIdLetra().equals(letraGratis.getIdLetra()))
                continue;

            if (esMedioBancario(pagoReq.getMedioPago()) && pagoReq.getFechaOperacion() == null) {
                throw new NegocioException(
                    "Para la letra N° " + letra.getNumeroLetra() +
                    ": la fecha de operación es obligatoria para pagos con " + pagoReq.getMedioPago() + ".");
            }

            BigDecimal descuentoLetra = BigDecimal.ZERO;
            if (descuentoTotal.compareTo(BigDecimal.ZERO) > 0 && montoTotalBruto.compareTo(BigDecimal.ZERO) > 0) {
                descuentoLetra = descuentoTotal
                    .multiply(pagoReq.getImportePagado())
                    .divide(montoTotalBruto, 2, RoundingMode.HALF_UP);
            }
            BigDecimal importeNetoLetra = pagoReq.getImportePagado().subtract(descuentoLetra);

            boolean esPagoAcuenta = Boolean.TRUE.equals(pagoReq.getEsPagoAcuenta());
            BigDecimal nuevoSaldo = validarYCalcularNuevoSaldo(letra, pagoReq.getImportePagado(), esPagoAcuenta);

            PagoLetras pago = new PagoLetras();
            pago.setLetra(letra);
            pago.setFechaPago(fechaPago);
            pago.setFechaOperacion(pagoReq.getFechaOperacion());
            pago.setImportePagado(importeNetoLetra);
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

            letra.setSaldoPendiente(nuevoSaldo);
            if (nuevoSaldo.compareTo(BigDecimal.ZERO) == 0) {
                // FIX: misma lógica que en registrarPago — también cubre letras con
                //      fecha vencida aunque el scheduler no las haya marcado VENCIDO aún.
                boolean letraEsVencida = letra.getEstadoLetra() == EstadoLetra.VENCIDO
                    || (letra.getFechaVencimiento() != null
                        && MoraServiceImpl.aplicarGraciaDominical(letra.getFechaVencimiento()).isBefore(fechaPago));

                if (letraEsVencida) {
                    int numLetra = extraerNumeroLetra(letra.getNumeroLetra());
                    LocalDate fechaRef = esMedioBancario(pagoReq.getMedioPago())
                        ? pagoReq.getFechaOperacion()
                        : resolverFechaReferenciaMora(numLetra, idContrato, fechaPago);
                    moraService.generarMoraParaPago(letra, guardado, fechaRef);
                }
                letra.setEstadoLetra(EstadoLetra.PAGADO);
            } else {
                letra.setEstadoLetra(EstadoLetra.PARCIAL);
            }
            letraCambioRepository.save(letra);

            PagoLetras finalGuardado = guardado;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificarAdminPagoLetra(finalGuardado);
                }
            });
            responses.add(mapToDTO(guardado));
        }

        // ── Asignar referenciaId al comprobante compartido ──────────────────
        if (comprobanteCompartido != null && comprobanteCompartido.getReferenciaId() == null && !responses.isEmpty()) {
            Integer idPrimerPago = responses.get(0).getIdPago();
            comprobanteCompartido.setReferenciaId(idPrimerPago);
            comprobanteRepository.save(comprobanteCompartido);
        }

        Map<String, Object> sunatRespuestaMulti = null;
        if (comprobanteCompartido != null
            && comprobanteCompartido.getTipoComprobante() == TipoComprobante.BOLETA
            && comprobanteCompartido.getSerie() != null
            && comprobanteCompartido.getSerie().startsWith("B")) {
            Cliente cliente = letraEjemplo.getContrato().getClientes().iterator().next().getCliente();

            // Construir descripción dinámica: "LETRA 75, 76 Y 77 POR LA COMPRA DE..."
            List<String> numsLimpios = new ArrayList<>();
            for (String nl : numerosLetraRequest) {
                String n = nl;
                if (n != null && n.contains("/")) n = n.substring(0, n.indexOf("/"));
                numsLimpios.add(n);
            }
            String letrasStr;
            if (numsLimpios.size() == 1) {
                letrasStr = "LETRA " + numsLimpios.get(0);
            } else if (numsLimpios.size() == 2) {
                letrasStr = "LETRA " + numsLimpios.get(0) + " Y " + numsLimpios.get(1);
            } else {
                StringBuilder sb = new StringBuilder("LETRA ");
                for (int i = 0; i < numsLimpios.size() - 1; i++) {
                    sb.append(numsLimpios.get(i));
                    if (i < numsLimpios.size() - 2) sb.append(", ");
                }
                sb.append(" Y ").append(numsLimpios.get(numsLimpios.size() - 1));
                letrasStr = sb.toString();
            }
            String nombrePrograma = "";
            if (letraEjemplo.getContrato().getLotes() != null && !letraEjemplo.getContrato().getLotes().isEmpty()) {
                ContratoLote primerLote = letraEjemplo.getContrato().getLotes().iterator().next();
                if (primerLote.getLote() != null && primerLote.getLote().getPrograma() != null) {
                    nombrePrograma = primerLote.getLote().getPrograma().getNombrePrograma();
                }
            }
            String descripcion = letrasStr + " POR LA COMPRA DE UN LOTE DE TERRENO RUSTICO PROGRAMA DE VIV. " + nombrePrograma.toUpperCase();
            comprobanteCompartido.setDescripcion(descripcion);

            sunatRespuestaMulti = sunatEnvioService.enviarBoleta(
                    cliente, letraEjemplo.getContrato(),
                    comprobanteCompartido, montoTotalNeto, descripcion);
            if (sunatRespuestaMulti != null && "ACEPTADA".equals(sunatRespuestaMulti.get("estadoSunat"))) {
                String hash = (String) sunatRespuestaMulti.get("hash");
                String cdrZip = (String) sunatRespuestaMulti.get("cdrZip");
                if (hash != null && !hash.isBlank()) comprobanteCompartido.setHashCdr(hash);
                if (cdrZip != null && !cdrZip.isBlank()) comprobanteCompartido.setCdrBase64(cdrZip);
            }
        }

        PagoLetraResponseDTO responseLetraGratis = null;
        if (letraGratis != null) {
            responseLetraGratis = registrarLetraGratis(
                letraGratis, idContrato, fechaPago,
                primerPago.getMedioPago(), comprobanteCompartido,
                request.getMotivoLetraGratis(), urlsVoucher
            );
        }

        letraCambioRepository.findById(request.getPagos().get(0).getIdLetra())
            .ifPresent(l -> verificarYActualizarEstadoContrato(l.getContrato()));

        // Poblar sunatAceptado/sunatMensaje en cada response
        for (PagoLetraResponseDTO r : responses) {
            if (sunatRespuestaMulti != null) {
                r.setSunatAceptado(true);
                r.setSunatMensaje((String) sunatRespuestaMulti.getOrDefault("mensaje", "ACEPTADA"));
            }
        }

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

    private LetraCambio validarLetraGratis(Integer idLetraGratis, Integer idContrato,
                                            int letrasEnLote) {
        LetraCambio letra = letraCambioRepository.findById(idLetraGratis)
            .orElseThrow(() -> new NegocioException("Letra gratis no encontrada: " + idLetraGratis));

        if (!letra.getContrato().getIdContrato().equals(idContrato))
            throw new NegocioException("La letra gratis no pertenece al mismo contrato.");

        if (letra.getEstadoLetra() == EstadoLetra.PAGADO)
            throw new NegocioException("La letra marcada como gratis ya está pagada.");

        long pagadasEnBD = letraCambioRepository.findByContratoIdContrato(idContrato)
            .stream()
            .filter(l -> l.getEstadoLetra() == EstadoLetra.PAGADO)
            .count();
        long totalPagadas = pagadasEnBD + letrasEnLote;

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

    private PagoLetraResponseDTO registrarLetraGratis(
            LetraCambio letra, Integer idContrato, LocalDate fechaPago,
            MedioPago medioPago, Comprobante comprobante,
            String motivo, List<String> urlsVoucher) {

        if (letra.getSaldoPendiente().compareTo(BigDecimal.ZERO) == 0
                && letra.getEstadoLetra() == EstadoLetra.PENDIENTE) {
            letra.setSaldoPendiente(letra.getImporte());
        }

        PagoLetras pago = new PagoLetras();
        pago.setLetra(letra);
        pago.setFechaPago(fechaPago);
        pago.setImportePagado(BigDecimal.ZERO);
        pago.setMedioPago(medioPago);
        pago.setNumeroOperacion(null);
        pago.setObservaciones(motivo != null ? motivo : "Letra gratis por política comercial");
        pago.setEsPagoAcuenta(false);
        pago.setDescuentoAplicado(letra.getSaldoPendiente());
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
        pago.setFechaPago(request.getFechaPago() != null ? request.getFechaPago() : LocalDate.now());
        pago.setObservaciones(request.getObservaciones());

        if (vouchers != null && !vouchers.isEmpty()) {
            List<Voucher> vouchersExistentes = voucherRepository
                .findByTipoOrigenAndReferenciaId("PAGO_LETRA", pago.getIdPago());
            for (Voucher v : vouchersExistentes) {
                try {
                    String publicId = extractPublicIdFromUrl(v.getUrl());
                    if (publicId != null) cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                } catch (Exception e) {
                    log.error("Error al eliminar imagen antigua", e);
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
                log.error("Error al eliminar imagen de Cloudinary", e);
            }
            voucherRepository.delete(v);
        }

        List<MoraLetra> morasAsociadas = moraRepository.findByPagoLetraIdPago(idPago);
        String usuarioEliminacion = obtenerUsuarioActual();
        LocalDateTime ahora = LocalDateTime.now();
        String motivo = "Anulación por eliminación de pago letra #" + idPago;

        for (MoraLetra mora : morasAsociadas) {
            for (PagoMora pm : mora.getPagos()) {
                pm.setAnulado(true);
                pm.setMotivoAnulacion(motivo);
                pm.setFechaAnulacion(ahora);
                pm.setAnuladoPor(usuarioEliminacion);
                pagoMoraRepository.save(pm);
            }
            mora.setPagoLetra(null);
            mora.setEstadoMora(EstadoMora.ANULADO);
            mora.setMotivoAnulacion(motivo);
            mora.setFechaAnulacion(ahora);
            mora.setAnuladoPor(usuarioEliminacion);
            moraRepository.save(mora);
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
            long otrosPagosConMismoComprobante =
                pagoLetraRepository.countByComprobanteNumeroCompleto(numeroComprobante);

            if (otrosPagosConMismoComprobante == 0) {
                comprobanteService.eliminarComprobante(idComprobante);
            }
        }

        BigDecimal totalPagadoRestante = pagoLetraRepository.sumImportePagadoByLetra(letra.getIdLetra());
        if (totalPagadoRestante == null) totalPagadoRestante = BigDecimal.ZERO;
        BigDecimal nuevoSaldo = letra.getImporte().subtract(totalPagadoRestante);

        long count = pagoLetraRepository.countByLetraIdLetra(letra.getIdLetra());
        if (count == 0) {
            letra.setSaldoPendiente(letra.getImporte());
            letra.setEstadoLetra(letra.getFechaVencimiento().isBefore(LocalDate.now())
                ? EstadoLetra.VENCIDO : EstadoLetra.PENDIENTE);
        } else {
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
            log.error("Error al extraer publicId", e);
            return null;
        }
    }

    private String obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "SISTEMA";
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PagoLetraResponseDTO> listarTodos(
            String numeroComprobante,
            String manzana,
            String numeroLote,
            Integer idPrograma,
            LocalDate desde,
            LocalDate hasta) {

        // ── PASO 1: Query única con lotes/programa + EAGER associations.
        // La query incluye JOIN FETCH para @ManyToOne (distrito, separacion,
        // vendedor, usuario) que evita queries EAGER adicionales.
        // Los clientes se cargan por separado (MultipleBagFetchException).
        List<PagoLetras> pagos = pagoLetraRepository.findTodosConLotes(
                (numeroComprobante != null && !numeroComprobante.isBlank()) ? numeroComprobante : null,
                (manzana           != null && !manzana.isBlank())           ? manzana           : null,
                (numeroLote        != null && !numeroLote.isBlank())        ? numeroLote        : null,
                idPrograma,
                desde,
                hasta);

        if (pagos.isEmpty()) {
            return Collections.emptyList();
        }

        // ── PASO 2: Batch-fetch vouchers para TODOS los pagos en 1 consulta ──
        List<Integer> pagoIds = pagos.stream()
                .map(PagoLetras::getIdPago)
                .collect(Collectors.toList());

        List<Voucher> vouchers = voucherRepository
                .findByTipoOrigenAndReferenciaIdIn("PAGO_LETRA", pagoIds);

        Map<Integer, List<String>> vouchersPorPago = vouchers.stream()
                .collect(Collectors.groupingBy(
                        Voucher::getReferenciaId,
                        Collectors.mapping(Voucher::getUrl, Collectors.toList())));

        // ── PASO 3: Batch-fetch nombres de clientes de contratos involucrados ──
        List<Integer> contratoIds = pagos.stream()
                .map(p -> p.getLetra().getContrato().getIdContrato())
                .distinct()
                .collect(Collectors.toList());

        List<Contrato> contratosConClientes = contratoRepository
                .findAllByIdConClientes(contratoIds);

        Map<Integer, String> nombreClientePorContrato = new HashMap<>();
        for (Contrato c : contratosConClientes) {
            if (c.getClientes() != null && !c.getClientes().isEmpty()) {
                var cc = c.getClientes().iterator().next();
                if (cc.getCliente() != null) {
                    nombreClientePorContrato.put(c.getIdContrato(),
                            cc.getCliente().getNombre() + " " + cc.getCliente().getApellidos());
                }
            }
        }

        // ── PASO 4: Mapear a DTO con datos pre-cargados ──
        return pagos.stream()
                .map(p -> {
                    PagoLetraResponseDTO dto = mapToDTO(p, vouchersPorPago);
                    Integer idContrato = p.getLetra().getContrato().getIdContrato();
                    dto.setNombreCliente(nombreClientePorContrato.get(idContrato));
                    return dto;
                })
                .collect(Collectors.toList());
    }
}