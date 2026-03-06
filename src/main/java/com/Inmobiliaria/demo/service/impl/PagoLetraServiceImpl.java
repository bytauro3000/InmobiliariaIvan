package com.Inmobiliaria.demo.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.Inmobiliaria.demo.dto.PagoLetraRequestDTO;
import com.Inmobiliaria.demo.dto.PagoLetraResponseDTO;
import com.Inmobiliaria.demo.entity.LetraCambio;
import com.Inmobiliaria.demo.entity.PagoLetras;
import com.Inmobiliaria.demo.enums.EstadoLetra;
import com.Inmobiliaria.demo.repository.LetraCambioRepository;
import com.Inmobiliaria.demo.repository.PagoLetraRepository;
import com.Inmobiliaria.demo.service.PagoLetraService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PagoLetraServiceImpl implements PagoLetraService {

    private final PagoLetraRepository pagoLetraRepository;
    private final LetraCambioRepository letraCambioRepository;
    private final Cloudinary cloudinary;

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
        dto.setUrlVoucher(pago.getUrlVoucher());
        dto.setTipoComprobante(pago.getTipoComprobante());
        dto.setNumeroComprobante(pago.getNumeroComprobante());
        dto.setObservaciones(pago.getObservaciones());
        return dto;
    }

    private String subirImagen(MultipartFile file, Integer idContrato, Integer idLetra) throws IOException {
        // Generar un identificador único para el archivo
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String publicId = "letra-" + idLetra + "-" + timestamp;
        
        Map params = ObjectUtils.asMap(
            "folder", "vouchers/contrato-" + idContrato,
            "public_id", publicId
        );
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
        return uploadResult.get("url").toString();
    }
    
    @Override
    public List<PagoLetraResponseDTO> listarPorContrato(Integer idContrato) {
        return pagoLetraRepository.findByLetraContratoIdContrato(idContrato)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<PagoLetraResponseDTO> listarPorLetra(Integer idLetra) {
        return pagoLetraRepository.findByLetraIdLetra(idLetra)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public PagoLetraResponseDTO obtenerPorId(Integer idPago) {
        PagoLetras pago = pagoLetraRepository.findById(idPago)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado con id: " + idPago));
        return mapToDTO(pago);
    }

    @Override
    @Transactional
    public PagoLetraResponseDTO registrarPago(PagoLetraRequestDTO request, MultipartFile voucher) throws IOException {
        LetraCambio letra = letraCambioRepository.findById(request.getIdLetra())
                .orElseThrow(() -> new RuntimeException("Letra no encontrada con id: " + request.getIdLetra()));

        if (letra.getEstadoLetra() == EstadoLetra.PAGADO) {
            throw new RuntimeException("La letra ya se encuentra pagada");
        }

        if (request.getImportePagado().compareTo(letra.getImporte()) != 0) {
            throw new RuntimeException("El importe pagado debe ser igual al importe de la letra");
        }

        String urlVoucher = null;
        if (voucher != null && !voucher.isEmpty()) {
            // Obtener el id del contrato desde la letra
            Integer idContrato = letra.getContrato().getIdContrato();
            Integer idLetra = letra.getIdLetra();
            urlVoucher = subirImagen(voucher, idContrato, idLetra);
        }

        PagoLetras pago = new PagoLetras();
        pago.setLetra(letra);
        pago.setFechaPago(LocalDate.now());
        pago.setImportePagado(request.getImportePagado());
        pago.setMedioPago(request.getMedioPago());
        pago.setNumeroOperacion(request.getNumeroOperacion());
        pago.setFechaOperacion(request.getFechaOperacion());
        pago.setUrlVoucher(urlVoucher);
        pago.setTipoComprobante(request.getTipoComprobante());
        pago.setNumeroComprobante(request.getNumeroComprobante());
        pago.setObservaciones(request.getObservaciones());

        PagoLetras pagoGuardado = pagoLetraRepository.save(pago);

        letra.setEstadoLetra(EstadoLetra.PAGADO);
        letraCambioRepository.save(letra);

        return mapToDTO(pagoGuardado);
    }

    @Override
    @Transactional
    public PagoLetraResponseDTO actualizarPago(Integer idPago, PagoLetraRequestDTO request, MultipartFile voucher) throws IOException {
        PagoLetras pago = pagoLetraRepository.findById(idPago)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado con id: " + idPago));

        pago.setImportePagado(request.getImportePagado());
        pago.setMedioPago(request.getMedioPago());
        pago.setNumeroOperacion(request.getNumeroOperacion());
        pago.setFechaOperacion(request.getFechaOperacion());
        pago.setTipoComprobante(request.getTipoComprobante());
        pago.setNumeroComprobante(request.getNumeroComprobante());
        pago.setObservaciones(request.getObservaciones());

        if (voucher != null && !voucher.isEmpty()) {
            // Obtener idContrato e idLetra del pago existente
            Integer idContrato = pago.getLetra().getContrato().getIdContrato();
            Integer idLetra = pago.getLetra().getIdLetra();
            String nuevaUrl = subirImagen(voucher, idContrato, idLetra);
            pago.setUrlVoucher(nuevaUrl);
        }

        PagoLetras pagoActualizado = pagoLetraRepository.save(pago);
        return mapToDTO(pagoActualizado);
    }

    @Override
    @Transactional
    public void eliminarPago(Integer idPago) {
        PagoLetras pago = pagoLetraRepository.findById(idPago)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado con id: " + idPago));
        // Opcional: eliminar también la imagen de Cloudinary si lo deseas
        // String publicId = extraerPublicIdDeUrl(pago.getUrlVoucher());
        // cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        pagoLetraRepository.delete(pago);
    }
}