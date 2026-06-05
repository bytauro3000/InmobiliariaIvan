package com.Inmobiliaria.demo.service.impl;

import com.Inmobiliaria.demo.dto.PagoInicialResponseDTO;
import com.Inmobiliaria.demo.entity.PagoInicial;
import com.Inmobiliaria.demo.entity.Voucher;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.PagoInicialRepository;
import com.Inmobiliaria.demo.repository.VoucherRepository;
import com.Inmobiliaria.demo.service.PagoInicialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PagoInicialServiceImpl implements PagoInicialService {

    private final PagoInicialRepository pagoInicialRepository;
    private final VoucherRepository     voucherRepository;

    // ── Obtener por contrato ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PagoInicialResponseDTO obtenerPorContrato(Integer idContrato) {
        PagoInicial pago = pagoInicialRepository.findByContratoIdContrato(idContrato)
            .orElseThrow(() -> new NegocioException(
                "No existe pago inicial para el contrato con id: " + idContrato));
        return mapToDTO(pago);
    }

    @Override
    @Transactional(readOnly = true)
    public PagoInicial obtenerEntidadPorContrato(Integer idContrato) {
        PagoInicial pago = pagoInicialRepository
                .findByContratoIdConClientesYComprobante(idContrato)
                .orElseThrow(() -> new NegocioException(
                    "No existe pago inicial para el contrato con id: " + idContrato));
        // Segunda query: hidrata lotes+lote+programa en la misma sesión
        pagoInicialRepository.findByContratoIdConLotes(idContrato);
        return pago;
    }

    // ── Anulación lógica ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public PagoInicialResponseDTO anularPagoInicial(Integer idContrato, String motivo, String anuladoPor) {
        PagoInicial pago = pagoInicialRepository.findByContratoIdContrato(idContrato)
            .orElseThrow(() -> new NegocioException(
                "No existe pago inicial para el contrato con id: " + idContrato));

        if (Boolean.TRUE.equals(pago.getAnulado()))
            throw new NegocioException("El pago inicial ya fue anulado.");

        pago.setAnulado(true);
        pago.setMotivoAnulacion(motivo);
        pago.setFechaAnulacion(LocalDateTime.now());
        pago.setAnuladoPor(anuladoPor);
        pagoInicialRepository.save(pago);

        return mapToDTO(pago);
    }

    // ── Eliminación física ────────────────────────────────────────────────────

    @Override
    @Transactional
    public void eliminarPagoInicial(Integer idContrato) {
        PagoInicial pago = pagoInicialRepository.findByContratoIdContrato(idContrato)
            .orElseThrow(() -> new NegocioException(
                "No existe pago inicial para el contrato con id: " + idContrato));

        // Eliminar vouchers asociados
        voucherRepository
            .findByTipoOrigenAndReferenciaId("PAGO_INICIAL", pago.getIdPagoInicial())
            .forEach(voucherRepository::delete);

        pagoInicialRepository.delete(pago);
    }

    // ── Listado general admin con filtros ─────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PagoInicialResponseDTO> listarTodos(
            String numeroComprobante,
            String manzana,
            String numeroLote,
            Integer idPrograma,
            LocalDate desde,
            LocalDate hasta) {

        return pagoInicialRepository
                .findTodos(
                    (numeroComprobante != null && !numeroComprobante.isBlank()) ? numeroComprobante : null,
                    (manzana           != null && !manzana.isBlank())           ? manzana           : null,
                    (numeroLote        != null && !numeroLote.isBlank())        ? numeroLote        : null,
                    idPrograma,
                    desde,
                    hasta)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ── Mapper privado ────────────────────────────────────────────────────────

    private PagoInicialResponseDTO mapToDTO(PagoInicial pago) {
        PagoInicialResponseDTO dto = new PagoInicialResponseDTO();
        dto.setIdPagoInicial(pago.getIdPagoInicial());
        dto.setImportePagado(pago.getImportePagado());
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
            .findByTipoOrigenAndReferenciaId("PAGO_INICIAL", pago.getIdPagoInicial())
            .stream().map(Voucher::getUrl).collect(Collectors.toList());
        dto.setUrlsVoucher(urls);

        // Anulación
        dto.setAnulado(Boolean.TRUE.equals(pago.getAnulado()));
        dto.setMotivoAnulacion(pago.getMotivoAnulacion());
        dto.setFechaAnulacion(pago.getFechaAnulacion());
        dto.setAnuladoPor(pago.getAnuladoPor());

        // Datos del contrato para el listado admin
        if (pago.getContrato() != null) {
            dto.setIdContrato(pago.getContrato().getIdContrato());

            // Moneda del contrato
            if (pago.getContrato().getMoneda() != null) {
                dto.setMoneda(pago.getContrato().getMoneda().name());
            }

            // Nombre del cliente principal
            if (pago.getContrato().getClientes() != null
                    && !pago.getContrato().getClientes().isEmpty()) {
                var cc = pago.getContrato().getClientes().iterator().next();
                if (cc.getCliente() != null) {
                    dto.setNombreCliente(
                        cc.getCliente().getNombre() + " " + cc.getCliente().getApellidos());
                }
            }

            // Manzana, lote y programa del primer lote
            if (pago.getContrato().getLotes() != null
                    && !pago.getContrato().getLotes().isEmpty()) {
                var cl = pago.getContrato().getLotes().iterator().next();
                if (cl.getLote() != null) {
                    dto.setManzana(cl.getLote().getManzana());
                    dto.setNumeroLote(cl.getLote().getNumeroLote());
                    if (cl.getLote().getPrograma() != null) {
                        dto.setNombrePrograma(cl.getLote().getPrograma().getNombrePrograma());
                    }
                }
            }
        }

        return dto;
    }
}