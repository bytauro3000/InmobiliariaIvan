package com.Inmobiliaria.demo.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.Inmobiliaria.demo.dto.PagoLetraRequestDTO;
import com.Inmobiliaria.demo.dto.PagoLetraResponseDTO;
import com.Inmobiliaria.demo.dto.PagosMultiplesRequestDTO;
import com.Inmobiliaria.demo.dto.SugerenciaNumeroComprobanteDTO;
import com.Inmobiliaria.demo.entity.LetraCambio;
import com.Inmobiliaria.demo.entity.PagoLetras;
import com.Inmobiliaria.demo.entity.Voucher;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.enums.EstadoContrato;
import com.Inmobiliaria.demo.enums.EstadoLetra;
import com.Inmobiliaria.demo.enums.TipoContrato;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import com.Inmobiliaria.demo.repository.LetraCambioRepository;
import com.Inmobiliaria.demo.repository.PagoLetraRepository;
import com.Inmobiliaria.demo.repository.VoucherRepository;
import com.Inmobiliaria.demo.exception.NegocioException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class PagoLetraServiceImpl implements PagoLetraService {

    private final PagoLetraRepository pagoLetraRepository;
    private final LetraCambioRepository letraCambioRepository;
    private final VoucherRepository voucherRepository;
    private final ContratoRepository contratoRepository;
    private final Cloudinary cloudinary;


    /**
     * Después de registrar un pago:
     * 1. Si todas las letras están PAGADAS → CANCELADO
     * 2. Si estaba en MORA y ya no tiene letras VENCIDAS → ACTIVO
     * En ambos casos invalida el caché de contratos.
     */
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

        // 1. ¿Todas pagadas? → CANCELADO
        boolean todasPagadas = letras.stream()
            .allMatch(l -> l.getEstadoLetra() == EstadoLetra.PAGADO);
        if (todasPagadas) {
            contrato.setEstadoContrato(EstadoContrato.CANCELADO);
            contratoRepository.save(contrato);
            return;
        }

        // 2. ¿Estaba en MORA y ya no quedan letras VENCIDAS? → ACTIVO
        if (estadoActual == EstadoContrato.MORA) {
            boolean sinVencidas = letras.stream()
                .noneMatch(l -> l.getEstadoLetra() == EstadoLetra.VENCIDO);
            if (sinVencidas) {
                contrato.setEstadoContrato(EstadoContrato.ACTIVO);
                contratoRepository.save(contrato);
            }
        }
    }

    // Alias para compatibilidad interna
    private void verificarYCancelarContrato(Contrato contrato) {
        verificarYActualizarEstadoContrato(contrato);
    }

    private PagoLetraResponseDTO mapToDTO(PagoLetras pago) {
        PagoLetraResponseDTO dto = new PagoLetraResponseDTO();
        dto.setIdPago(pago.getIdPago());
        dto.setIdLetra(pago.getLetra().getIdLetra());
        dto.setNumeroLetra(pago.getLetra().getNumeroLetra());
        dto.setFechaPago(pago.getFechaPago());
        dto.setImportePagado(pago.getImportePagado());
        dto.setMedioPago(pago.getMedioPago());
        dto.setNumeroOperacion(pago.getNumeroOperacion());
        dto.setFechaOperacion(pago.getFechaOperacion());
        dto.setTipoComprobante(pago.getTipoComprobante());
        dto.setNumeroComprobante(pago.getNumeroComprobante());
        dto.setObservaciones(pago.getObservaciones());
        List<String> urls = pago.getVouchers().stream()
                .map(Voucher::getUrl)
                .collect(Collectors.toList());
        dto.setUrlsVoucher(urls);
        return dto;
    }

    private String subirImagen(MultipartFile file, Integer idContrato, Integer idLetra) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String publicId = "letra-" + idLetra + "-" + timestamp;

        Map params = ObjectUtils.asMap(
            "folder", "vouchers/contrato-" + idContrato,
            "public_id", publicId
        );
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
        return uploadResult.get("url").toString();
    }

    private void guardarVouchers(List<MultipartFile> files, PagoLetras pago, Integer idContrato, Integer idLetra) throws IOException {
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                String url = subirImagen(file, idContrato, idLetra);
                Voucher v = new Voucher();
                v.setPago(pago);
                v.setUrl(url);
                voucherRepository.save(v);
            }
        }
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
        PagoLetras pago = pagoLetraRepository.findById(idPago)
                .orElseThrow(() -> new NegocioException("Pago no encontrado con id: " + idPago));
        return mapToDTO(pago);
    }

    @Override
    @Transactional
    @CacheEvict(value = "contratos", allEntries = true)
    public PagoLetraResponseDTO registrarPago(PagoLetraRequestDTO request, List<MultipartFile> vouchers) throws IOException {
        LetraCambio letra = letraCambioRepository.findById(request.getIdLetra())
                .orElseThrow(() -> new NegocioException("Letra no encontrada con id: " + request.getIdLetra()));

        if (letra.getEstadoLetra() == EstadoLetra.PAGADO) {
            throw new NegocioException("La letra ya se encuentra pagada");
        }

        if (request.getImportePagado().compareTo(letra.getImporte()) != 0) {
            throw new NegocioException("El importe pagado debe ser igual al importe de la letra");
        }

        // Validar unicidad del comprobante
        if (request.getTipoComprobante() != null && request.getNumeroComprobante() != null) {
            boolean existe = pagoLetraRepository.existsByTipoComprobanteAndNumeroComprobante(
                    request.getTipoComprobante(), request.getNumeroComprobante());
            if (existe) {
                throw new NegocioException("Ya existe un pago con el mismo tipo y numero de comprobante.");
            }
        }

        PagoLetras pago = new PagoLetras();
        pago.setLetra(letra);
        pago.setFechaPago(request.getFechaOperacion());
        pago.setImportePagado(request.getImportePagado());
        pago.setMedioPago(request.getMedioPago());
        pago.setNumeroOperacion(request.getNumeroOperacion());
        pago.setFechaOperacion(request.getFechaOperacion());
        pago.setTipoComprobante(request.getTipoComprobante());
        pago.setNumeroComprobante(request.getNumeroComprobante());
        pago.setObservaciones(request.getObservaciones());

        PagoLetras pagoGuardado = pagoLetraRepository.save(pago);

        Integer idContrato = letra.getContrato().getIdContrato();
        Integer idLetra = letra.getIdLetra();
        guardarVouchers(vouchers, pagoGuardado, idContrato, idLetra);

        letra.setEstadoLetra(EstadoLetra.PAGADO);
        letraCambioRepository.save(letra);

        // Verificar si fue la última letra → pasar contrato a CANCELADO
        verificarYCancelarContrato(letra.getContrato());

        return mapToDTO(pagoGuardado);
    }

    @Override
    @Transactional
    public List<PagoLetraResponseDTO> registrarPagosMultiples(PagosMultiplesRequestDTO request, List<MultipartFile> vouchers) throws IOException {
        List<PagoLetraResponseDTO> responses = new ArrayList<>();

        if (request.getPagos() == null || request.getPagos().isEmpty()) {
            throw new NegocioException("La lista de pagos no puede estar vacia");
        }

        // Validar unicidad del comprobante (todos los pagos usan el mismo número)
        PagoLetraRequestDTO primerPago = request.getPagos().get(0);
        if (primerPago.getTipoComprobante() != null && primerPago.getNumeroComprobante() != null) {
            boolean existe = pagoLetraRepository.existsByTipoComprobanteAndNumeroComprobante(
                    primerPago.getTipoComprobante(), primerPago.getNumeroComprobante());
            if (existe) {
                throw new NegocioException("Ya existe un pago con el mismo tipo y numero de comprobante.");
            }
        }

        List<String> urlsVoucher = new ArrayList<>();
        if (vouchers != null && !vouchers.isEmpty()) {
            Integer idPrimeraLetra = request.getPagos().get(0).getIdLetra();
            LetraCambio letraEjemplo = letraCambioRepository.findById(idPrimeraLetra)
                    .orElseThrow(() -> new NegocioException("Letra no encontrada: " + idPrimeraLetra));
            Integer idContrato = letraEjemplo.getContrato().getIdContrato();
            for (MultipartFile voucher : vouchers) {
                String url = subirImagen(voucher, idContrato, null);
                urlsVoucher.add(url);
            }
        }

        for (PagoLetraRequestDTO pagoReq : request.getPagos()) {
            LetraCambio letra = letraCambioRepository.findById(pagoReq.getIdLetra())
                    .orElseThrow(() -> new NegocioException("Letra no encontrada con id: " + pagoReq.getIdLetra()));

            if (letra.getEstadoLetra() == EstadoLetra.PAGADO) {
                throw new NegocioException("La letra " + letra.getNumeroLetra() + " ya esta pagada");
            }

            if (pagoReq.getImportePagado().compareTo(letra.getImporte()) != 0) {
                throw new NegocioException("El importe pagado no coincide con el importe de la letra " + letra.getNumeroLetra());
            }

            PagoLetras pago = new PagoLetras();
            pago.setLetra(letra);
            pago.setFechaPago(pagoReq.getFechaOperacion());
            pago.setImportePagado(pagoReq.getImportePagado());
            pago.setMedioPago(pagoReq.getMedioPago());
            pago.setNumeroOperacion(pagoReq.getNumeroOperacion());
            pago.setFechaOperacion(pagoReq.getFechaOperacion());
            pago.setTipoComprobante(pagoReq.getTipoComprobante());
            pago.setNumeroComprobante(pagoReq.getNumeroComprobante());
            pago.setObservaciones(pagoReq.getObservaciones());

            PagoLetras guardado = pagoLetraRepository.save(pago);

            for (String url : urlsVoucher) {
                Voucher v = new Voucher();
                v.setPago(guardado);
                v.setUrl(url);
                voucherRepository.save(v);
            }

            responses.add(mapToDTO(guardado));

            letra.setEstadoLetra(EstadoLetra.PAGADO);
            letraCambioRepository.save(letra);
        }

        // Verificar si todas las letras del contrato quedaron PAGADAS → CANCELADO
        // Usamos la primera letra para obtener el contrato (todas pertenecen al mismo)
        letraCambioRepository.findById(request.getPagos().get(0).getIdLetra())
            .ifPresent(l -> verificarYCancelarContrato(l.getContrato()));

        return responses;
    }

    @Override
    @Transactional
    public PagoLetraResponseDTO actualizarPago(Integer idPago, PagoLetraRequestDTO request, List<MultipartFile> vouchers) throws IOException {
        PagoLetras pago = pagoLetraRepository.findById(idPago)
                .orElseThrow(() -> new NegocioException("Pago no encontrado con id: " + idPago));

        // Validar unicidad excluyendo el propio pago
        if (request.getTipoComprobante() != null && request.getNumeroComprobante() != null) {
            boolean existe = pagoLetraRepository.existsByTipoComprobanteAndNumeroComprobanteAndIdPagoNot(
                    request.getTipoComprobante(), request.getNumeroComprobante(), idPago);
            if (existe) {
                throw new NegocioException("Ya existe otro pago con el mismo tipo y numero de comprobante.");
            }
        }

        pago.setImportePagado(request.getImportePagado());
        pago.setMedioPago(request.getMedioPago());
        pago.setNumeroOperacion(request.getNumeroOperacion());
        pago.setFechaOperacion(request.getFechaOperacion());
        pago.setFechaPago(request.getFechaOperacion());
        pago.setTipoComprobante(request.getTipoComprobante());
        pago.setNumeroComprobante(request.getNumeroComprobante());
        pago.setObservaciones(request.getObservaciones());

        if (vouchers != null && !vouchers.isEmpty()) {
            // Eliminar imágenes antiguas de Cloudinary
            for (Voucher v : pago.getVouchers()) {
                try {
                    String publicId = extractPublicIdFromUrl(v.getUrl());
                    if (publicId != null) {
                        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                    }
                } catch (Exception e) {
                    System.err.println("Error al eliminar imagen antigua: " + e.getMessage());
                }
            }
            pago.getVouchers().clear();

            // Subir nuevos vouchers
            Integer idContrato = pago.getLetra().getContrato().getIdContrato();
            Integer idLetra = pago.getLetra().getIdLetra();
            for (MultipartFile file : vouchers) {
                String url = subirImagen(file, idContrato, idLetra);
                Voucher v = new Voucher();
                v.setPago(pago);
                v.setUrl(url);
                voucherRepository.save(v);
            }
        }

        PagoLetras pagoActualizado = pagoLetraRepository.save(pago);
        return mapToDTO(pagoActualizado);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "contratos", allEntries = true)
    public void eliminarPago(Integer idPago) throws IOException {
        PagoLetras pago = pagoLetraRepository.findById(idPago)
                .orElseThrow(() -> new NegocioException("Pago no encontrado con id: " + idPago));

        LetraCambio letra = pago.getLetra();

        // Eliminar imágenes de Cloudinary
        for (Voucher v : pago.getVouchers()) {
            try {
                String publicId = extractPublicIdFromUrl(v.getUrl());
                if (publicId != null) {
                    cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                }
            } catch (Exception e) {
                System.err.println("Error al eliminar imagen de Cloudinary: " + e.getMessage());
            }
        }

        pagoLetraRepository.delete(pago);

        long count = pagoLetraRepository.countByLetraIdLetra(letra.getIdLetra());
        if (count == 0) {
            LocalDate hoy = LocalDate.now();
            if (letra.getFechaVencimiento().isBefore(hoy)) {
                letra.setEstadoLetra(EstadoLetra.VENCIDO);
            } else {
                letra.setEstadoLetra(EstadoLetra.PENDIENTE);
            }
            letraCambioRepository.save(letra);
        }

        // Después de eliminar el pago y actualizar la letra, recalcular el estado del contrato.
        // Si la letra volvió a VENCIDO y el contrato estaba ACTIVO → debe pasar a MORA.
        // Si no quedan letras vencidas y estaba en MORA → puede quedar ACTIVO (raro, pero correcto).
        recalcularEstadoContrato(letra.getContrato());
    }

    /**
     * Recalcula el estado ACTIVO/MORA del contrato basándose en las letras actuales.
     * Usado al eliminar un pago para revertir el estado si corresponde.
     */
    @CacheEvict(cacheNames = "contratos", allEntries = true)
    public void recalcularEstadoContrato(Contrato contrato) {
        if (contrato.getTipoContrato() != TipoContrato.FINANCIADO) return;

        EstadoContrato estadoActual = contrato.getEstadoContrato();
        if (estadoActual == EstadoContrato.CANCELADO    ||
            estadoActual == EstadoContrato.RESUELTO      ||
            estadoActual == EstadoContrato.EN_RESOLUCION ||
            estadoActual == EstadoContrato.RENUNCIA      ||
            estadoActual == EstadoContrato.TRANSFERIDO) return;

        // Recargar letras frescas desde BD (el estado de la letra ya fue actualizado antes)
        Contrato contratoFresco = contratoRepository.findById(contrato.getIdContrato())
            .orElse(null);
        if (contratoFresco == null) return;

        long letrasVencidas = contratoFresco.getLetrasCambio().stream()
            .filter(l -> l.getEstadoLetra() == EstadoLetra.VENCIDO)
            .count();

        EstadoContrato nuevoEstado = letrasVencidas > 0
            ? EstadoContrato.MORA
            : EstadoContrato.ACTIVO;

        if (nuevoEstado != estadoActual) {
            contratoFresco.setEstadoContrato(nuevoEstado);
            contratoRepository.save(contratoFresco);
        }
    }

    private String extractPublicIdFromUrl(String url) {
        try {
            int uploadIndex = url.indexOf("/upload/");
            if (uploadIndex == -1) return null;

            String afterUpload = url.substring(uploadIndex + 8);
            String[] parts = afterUpload.split("/", 2);
            if (parts.length == 2) {
                String pathWithExtension = parts[1];
                int dotIndex = pathWithExtension.lastIndexOf('.');
                if (dotIndex != -1) {
                    return pathWithExtension.substring(0, dotIndex);
                }
                return pathWithExtension;
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error al extraer publicId de URL: " + e.getMessage());
            return null;
        }
    }

    @Override
    public SugerenciaNumeroComprobanteDTO sugerirNumeroComprobante(TipoComprobante tipoComprobante) {
        Optional<PagoLetras> ultimo = pagoLetraRepository.findFirstByTipoComprobanteOrderByFechaOperacionDesc(tipoComprobante);
        if (ultimo.isPresent()) {
            String ultimoNumero = ultimo.get().getNumeroComprobante();
            String siguiente = generarSiguienteNumero(ultimoNumero, tipoComprobante);
            return new SugerenciaNumeroComprobanteDTO(siguiente);
        } else {
            String inicial = switch (tipoComprobante) {
                case RECIBO -> "1";
                case BOLETA -> "EB01-0001";
                case FACTURA -> "F001-0001";
                default -> "1";
            };
            return new SugerenciaNumeroComprobanteDTO(inicial);
        }
    }

    private String generarSiguienteNumero(String ultimoNumero, TipoComprobante tipo) {
        if (tipo == TipoComprobante.RECIBO) {
            try {
                int num = Integer.parseInt(ultimoNumero);
                return String.valueOf(num + 1);
            } catch (NumberFormatException e) {
                return "1";
            }
        } else if (tipo == TipoComprobante.BOLETA || tipo == TipoComprobante.FACTURA) {
            String[] parts = ultimoNumero.split("-");
            if (parts.length == 2) {
                try {
                    int num = Integer.parseInt(parts[1]);
                    String prefix = parts[0];
                    return String.format("%s-%04d", prefix, num + 1);
                } catch (NumberFormatException e) {
                    return tipo == TipoComprobante.BOLETA ? "EB01-0001" : "F001-0001";
                }
            }
            return tipo == TipoComprobante.BOLETA ? "EB01-0001" : "F001-0001";
        }
        return "1";
    }
}