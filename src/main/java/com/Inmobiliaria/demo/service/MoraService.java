package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface MoraService {

    CalculoMoraDTO calcularMora(Integer idLetra);
    CalculoMoraDTO calcularMora(Integer idLetra, LocalDate fechaReferencia);

    List<MoraResponseDTO> listarPorContrato(Integer idContrato);
    List<MoraResponseDTO> listarPendientesPorContrato(Integer idContrato);
    List<MoraResponseDTO> listarPorLetra(Integer idLetra);
    MoraResponseDTO obtenerPorId(Integer idMora);
    MoraResumenContratoDTO obtenerResumenPorContrato(Integer idContrato);

    PagoMoraResponseDTO pagarMora(PagoMoraRequestDTO request, List<MultipartFile> vouchers) throws IOException;

    /**
     * Pago múltiple de moras: N moras del mismo contrato en una sola operación
     * con comprobante y vouchers compartidos. Devuelve un mapa con:
     * pagos, numeroComprobanteGenerado, descuentoAplicado, montoTotalNeto.
     */
    Map<String, Object> pagarMorasMultiples(MoraPagosMultiplesRequestDTO request,
                                            List<MultipartFile> vouchers) throws IOException;

    /** Anula la entidad MoraLetra (estado → ANULADO). */
    MoraResponseDTO anularMora(Integer idMora, String motivo, String anuladoPor);

    /** Anulación lógica de un PagoMora. Solo ROLE_ADMINISTRADOR. */
    PagoMoraResponseDTO anularPagoMora(Integer idPagoMora, String motivo, String anuladoPor);

    /** Elimina físicamente un PagoMora. Solo ROLE_ADMINISTRADOR. */
    void eliminarPagoMora(Integer idPagoMora);

    MoraResponseDTO crearMoraPendiente(Integer idLetra);

    /**
     * Listado general de pagos de mora para el panel admin.
     * Cualquier parámetro puede ser null (sin filtro).
     */
    List<PagoMoraResponseDTO> listarPagosTodos(
            String numeroComprobante,
            String manzana,
            String numeroLote,
            Integer idPrograma,
            LocalDate desde,
            LocalDate hasta);
}