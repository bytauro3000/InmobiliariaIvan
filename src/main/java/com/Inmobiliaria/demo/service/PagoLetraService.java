package com.Inmobiliaria.demo.service;

import com.Inmobiliaria.demo.dto.PagoLetraRequestDTO;
import com.Inmobiliaria.demo.dto.PagoLetraResponseDTO;
import com.Inmobiliaria.demo.dto.PagosMultiplesRequestDTO;
import com.Inmobiliaria.demo.dto.SugerenciaNumeroComprobanteDTO;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface PagoLetraService {

    List<PagoLetraResponseDTO> listarPorContrato(Integer idContrato);
    List<PagoLetraResponseDTO> listarPorLetra(Integer idLetra);
    PagoLetraResponseDTO obtenerPorId(Integer idPago);

    PagoLetraResponseDTO registrarPago(PagoLetraRequestDTO request, List<MultipartFile> vouchers) throws IOException;
    PagoLetraResponseDTO actualizarPago(Integer idPago, PagoLetraRequestDTO request, List<MultipartFile> vouchers) throws IOException;
    Map<String, Object> registrarPagosMultiples(PagosMultiplesRequestDTO request, List<MultipartFile> vouchers) throws IOException;

    void eliminarPago(Integer idPago) throws IOException;

    /** Anulación lógica. Solo ROLE_ADMINISTRADOR. */
    PagoLetraResponseDTO anularPago(Integer idPago, String motivo, String anuladoPor);

    /**
     * Anula el pago + restaura la letra + anula las moras asociadas.
     * Usado tanto por anulación simple como por nota de crédito SUNAT.
     */
    void anularPagoConMoras(Integer idPago, String motivo, String anuladoPor);

    /**
     * Listado general para el panel admin con filtros opcionales.
     * Cualquier parámetro puede ser null (sin filtro).
     */
    List<PagoLetraResponseDTO> listarTodos(
            String numeroComprobante,
            String manzana,
            String numeroLote,
            Integer idPrograma,
            LocalDate desde,
            LocalDate hasta);

    String previewSiguienteNumeroComprobante(TipoComprobante tipoComprobante);
    SugerenciaNumeroComprobanteDTO sugerirNumeroComprobante(TipoComprobante tipoComprobante);
    BigDecimal consultarSaldoPendiente(Integer idLetra);
}