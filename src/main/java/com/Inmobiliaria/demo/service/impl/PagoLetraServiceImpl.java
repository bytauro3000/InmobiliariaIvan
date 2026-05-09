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

    // ─── INYECCIÓN DEL SERVICIO CENTRALIZADO ──────────────────────────────────
    private final ComprobanteService    comprobanteService;

    // ─── Utilidades internas ───────────────────────────────────────────────────

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
        if (maxPagado == 0 && pagoLetraRepository.findMaxNumeroLetraPagadoByContrato(idContrato).isEmpty()) {
            maxPagado = 0;
        }
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

    // ─── Estado contrato ───────────────────────────────────────────────────────

    @CacheEvict(cacheNames = "contratos", allEntries = true)
    public void verificarYActualizarEstadoContrato(Contrato contrato) {
        if (contrato.getTipoContrato() != TipoContrato.FINANCIADO) return;
        EstadoContrato estadoActual = contrato.getEstadoContrato();
        if (estadoActual == EstadoContrato.CANCELADO    ||
            estadoActual == EstadoContrato.RESUELTO      ||
            estadoActual == EstadoContrato.EN_RESOLUCION ||
            estadoActual == EstadoContrato.RENUNCIA      ||
            estadoActual == EstadoContrato.TRANSFERIDO) return;

        List<LetraCambio> letras = contrato.getLetrasCambio();
        if (letras == null || letras.isEmpty()) return;

        boolean ultimaLetraPagada = letras.stream()
            .max(Comparator.comparingInt(l -> extraerNumeroLetra(l.getNumeroLetra())))
            .map(l -> l.getEstadoLetra() == EstadoLetra.PAGADO)
            .orElse(false);

        if (ultimaLetraPagada) {
            contrato.setEstadoContrato(EstadoContrato.CANCELADO);
            contratoRepository.save(contrato);
            return;
        }

        if (estadoActual == EstadoContrato.MORA) {
            boolean sinVencidas = letras.stream().noneMatch(l -> l.getEstadoLetra() == EstadoLetra.VENCIDO);
            if (sinVencidas) {
                contrato.setEstadoContrato(EstadoContrato.ACTIVO);
                contratoRepository.save(contrato);
            }
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

    // ─── Mapper ────────────────────────────────────────────────────────────────

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

        // ── Leer comprobante desde la relación centralizada ──────────────────
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

    // ─── Vouchers ──────────────────────────────────────────────────────────────

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
    
    

    // ─── Consultas ─────────────────────────────────────────────────────────────

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

    // ─── Registro de pago (el cambio más importante) ───────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = "contratos", allEntries = true)
    public PagoLetraResponseDTO registrarPago(PagoLetraRequestDTO request,
                                               List<MultipartFile> vouchers) throws IOException {
        LetraCambio letra = letraCambioRepository.findById(request.getIdLetra())
            .orElseThrow(() -> new NegocioException("Letra no encontrada con id: " + request.getIdLetra()));

        if (letra.getEstadoLetra() == EstadoLetra.PAGADO)
            throw new NegocioException("La letra ya se encuentra pagada.");

        if (request.getImportePagado().compareTo(letra.getImporte()) != 0)
            throw new NegocioException("El importe pagado debe ser igual al importe de la letra.");

        Integer idContrato = letra.getContrato().getIdContrato();
        validarOrdenDePago(idContrato, letra.getNumeroLetra());

        // ── Construir el pago SIN comprobante aún ────────────────────────────
        PagoLetras pago = new PagoLetras();
        pago.setLetra(letra);
        pago.setFechaPago(request.getFechaPago());
        pago.setImportePagado(request.getImportePagado());
        pago.setMedioPago(request.getMedioPago());
        pago.setNumeroOperacion(request.getNumeroOperacion());
        pago.setObservaciones(request.getObservaciones());

        PagoLetras pagoGuardado = pagoLetraRepository.save(pago);

        // ── Generar comprobante centralizado ─────────────────────────────────
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

        if (letra.getEstadoLetra() == EstadoLetra.VENCIDO) {
            int numLetra = extraerNumeroLetra(letra.getNumeroLetra());
            LocalDate fechaRef = resolverFechaReferenciaMora(numLetra, idContrato, request.getFechaPago());
            moraService.generarMoraParaPago(letra, pagoGuardado, fechaRef);
        }

        letra.setEstadoLetra(EstadoLetra.PAGADO);
        letraCambioRepository.save(letra);
        verificarYActualizarEstadoContrato(letra.getContrato());

        return mapToDTO(pagoGuardado);
    }

    // ─── Registro múltiple ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public List<PagoLetraResponseDTO> registrarPagosMultiples(PagosMultiplesRequestDTO request,
                                                               List<MultipartFile> vouchers) throws IOException {
        if (request.getPagos() == null || request.getPagos().isEmpty())
            throw new NegocioException("La lista de pagos no puede estar vacía.");

        Integer idPrimeraLetra = request.getPagos().get(0).getIdLetra();
        LetraCambio letraEjemplo = letraCambioRepository.findById(idPrimeraLetra)
            .orElseThrow(() -> new NegocioException("Letra no encontrada: " + idPrimeraLetra));
        Integer idContrato = letraEjemplo.getContrato().getIdContrato();

        List<String> numerosLetraRequest = new ArrayList<>();
        for (PagoLetraRequestDTO req : request.getPagos()) {
            LetraCambio lc = letraCambioRepository.findById(req.getIdLetra())
                .orElseThrow(() -> new NegocioException("Letra no encontrada: " + req.getIdLetra()));
            numerosLetraRequest.add(lc.getNumeroLetra());
        }
        validarOrdenDePagoMultiple(idContrato, numerosLetraRequest);

        // Subir vouchers una sola vez para todos los pagos del lote
        List<String> urlsVoucher = new ArrayList<>();
        if (vouchers != null && !vouchers.isEmpty()) {
            for (MultipartFile v : vouchers)
                urlsVoucher.add(subirImagen(v, idContrato, null));
        }

        List<PagoLetraResponseDTO> responses = new ArrayList<>();

        for (PagoLetraRequestDTO pagoReq : request.getPagos()) {
            LetraCambio letra = letraCambioRepository.findById(pagoReq.getIdLetra())
                .orElseThrow(() -> new NegocioException("Letra no encontrada: " + pagoReq.getIdLetra()));

            if (letra.getEstadoLetra() == EstadoLetra.PAGADO)
                throw new NegocioException("La letra " + letra.getNumeroLetra() + " ya está pagada.");
            if (pagoReq.getImportePagado().compareTo(letra.getImporte()) != 0)
                throw new NegocioException("El importe no coincide para la letra " + letra.getNumeroLetra() + ".");

            PagoLetras pago = new PagoLetras();
            pago.setLetra(letra);
            pago.setFechaPago(pagoReq.getFechaPago());
            pago.setImportePagado(pagoReq.getImportePagado());
            pago.setMedioPago(pagoReq.getMedioPago());
            pago.setNumeroOperacion(pagoReq.getNumeroOperacion());
            pago.setObservaciones(pagoReq.getObservaciones());

            PagoLetras guardado = pagoLetraRepository.save(pago);

            // ── Comprobante por cada letra del lote ───────────────────────────
            if (pagoReq.getTipoComprobante() != null) {
                Comprobante comp = comprobanteService.generarComprobanteConNumero(
                    pagoReq.getTipoComprobante(),
                    TipoOrigenComprobante.PAGO_LETRA,
                    guardado.getIdPago(),
                    pagoReq.getImportePagado(),
                    pagoReq.getFechaPago(),
                    pagoReq.getNumeroComprobantePersonalizado()
                );
                guardado.setComprobante(comp);
                guardado = pagoLetraRepository.save(guardado);
            }

            for (String url : urlsVoucher) {
                Voucher v = new Voucher();
                v.setTipoOrigen("PAGO_LETRA");
                v.setReferenciaId(guardado.getIdPago());
                v.setUrl(url);
                voucherRepository.save(v);
            }

            responses.add(mapToDTO(guardado));

            if (letra.getEstadoLetra() == EstadoLetra.VENCIDO) {
                int numLetra = extraerNumeroLetra(letra.getNumeroLetra());
                LocalDate fechaRef = resolverFechaReferenciaMora(numLetra, idContrato, pagoReq.getFechaPago());
                moraService.generarMoraParaPago(letra, guardado, fechaRef);
            }

            letra.setEstadoLetra(EstadoLetra.PAGADO);
            letraCambioRepository.save(letra);
        }

        letraCambioRepository.findById(request.getPagos().get(0).getIdLetra())
            .ifPresent(l -> verificarYActualizarEstadoContrato(l.getContrato()));

        return responses;
    }

    // ─── Actualizar pago ───────────────────────────────────────────────────────
    
    

    @Override
    @Transactional
    public PagoLetraResponseDTO actualizarPago(Integer idPago, PagoLetraRequestDTO request,
                                                List<MultipartFile> vouchers) throws IOException {
        PagoLetras pago = pagoLetraRepository.findById(idPago)
            .orElseThrow(() -> new NegocioException("Pago no encontrado con id: " + idPago));

        // Solo actualizamos datos operativos; el comprobante YA no se modifica
        // (si se emitió, es un documento fiscal y no se puede reasignar)
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

    // ─── Eliminar pago ─────────────────────────────────────────────────────────

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

        // ── Guardar el id del comprobante antes de eliminar el pago ──────────
        Long idComprobante = pago.getComprobante() != null
                ? pago.getComprobante().getIdComprobante()
                : null;

        // ── Desvincular el comprobante del pago antes de borrar ──────────────
        pago.setComprobante(null);
        pagoLetraRepository.save(pago);

        pagoLetraRepository.delete(pago);

        // ── Eliminar el comprobante y recalcular el contador de serie ─────────
        // Esto garantiza que el siguiente número automático sea correcto
        // y no salte un número por encima del eliminado.
        if (idComprobante != null) {
            comprobanteService.eliminarComprobante(idComprobante);
        }

        long count = pagoLetraRepository.countByLetraIdLetra(letra.getIdLetra());
        if (count == 0) {
            letra.setEstadoLetra(letra.getFechaVencimiento().isBefore(LocalDate.now())
                ? EstadoLetra.VENCIDO : EstadoLetra.PENDIENTE);
            letraCambioRepository.save(letra);
        }

        recalcularEstadoContrato(letra.getContrato());
    }

    // ─── Preview número comprobante (delega en ComprobanteService) ────────────

    @Override
    public String previewSiguienteNumeroComprobante(TipoComprobante tipoComprobante) {
        return comprobanteService.previewSiguienteNumero(tipoComprobante);
    }

    // ─── Utilidad privada ──────────────────────────────────────────────────────

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