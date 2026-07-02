package com.Inmobiliaria.demo.service;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import com.Inmobiliaria.demo.dto.TransferenciaResponseDTO;
import com.Inmobiliaria.demo.dto.ContratoRequestDTO;
import com.Inmobiliaria.demo.dto.ContratoResponseDTO;
import com.Inmobiliaria.demo.dto.ContratoListItemDTO;

public interface ContratoService {

    ContratoResponseDTO guardarContrato(ContratoRequestDTO requestDTO, Principal principal);

    List<ContratoResponseDTO> listarContratos();

    List<ContratoListItemDTO> listarContratosResumen();

    void eliminarContrato(Integer idContrato);

    ContratoResponseDTO buscarPorId(Integer idContrato);

    byte[] generarPdf(Integer idContrato);

    ContratoResponseDTO actualizarContrato(Integer id, ContratoRequestDTO requestDTO);

    ContratoResponseDTO buscarPorProgramaManzanaLote(Integer idPrograma, String manzana, String numeroLote);

    ContratoResponseDTO cambiarEstado(Integer idContrato, String nuevoEstado);

    ContratoResponseDTO registrarRenuncia(Integer idContrato);

    TransferenciaResponseDTO registrarTransferencia(Integer idContrato);

    List<ContratoResponseDTO> buscarPorNombreCliente(String termino);

    List<ContratoListItemDTO> buscarPorNombreClienteResumen(String termino);

    Map<String, Object> consultarImpactoEdicion(Integer idContrato);

    ContratoResponseDTO subirVoucherInicial(Integer idContrato, org.springframework.web.multipart.MultipartFile voucher);
}