package com.Inmobiliaria.demo.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Date;
import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Inmobiliaria.demo.dto.ClienteResponseDTO;
import com.Inmobiliaria.demo.dto.ContratoRequestDTO;
import com.Inmobiliaria.demo.dto.ContratoResponseDTO;
import com.Inmobiliaria.demo.entity.*;
import com.Inmobiliaria.demo.enums.TipoContrato;
import com.Inmobiliaria.demo.enums.TipoPropietario;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import com.Inmobiliaria.demo.service.*;

@Service
public class ContratoServiceImpl implements ContratoService {

    @Autowired private ContratoRepository contratoRepository;
    @Autowired private ContratoClienteService contratoClienteService;
    @Autowired private ContratoLoteService contratoLoteService;
    @Autowired private ClienteService clienteService;
    @Autowired private LoteService loteService;
    @Autowired private UsuarioService usuarioService;

    // Antes de guardar, se verifica el tipo de contrato para setear valores por defecto
    private void setearValoresPorDefecto(Contrato contrato) {
        if (contrato.getTipoContrato() == TipoContrato.CONTADO) {
            contrato.setCantidadLetras(0);
            contrato.setInicial(BigDecimal.ZERO);
            contrato.setSaldo(BigDecimal.ZERO);
        }
    }
    
    @Override
    @Transactional
    public ContratoResponseDTO guardarContrato(ContratoRequestDTO requestDTO, Principal principal) {
        Contrato contrato = requestDTO.getContrato();
        
        // Asignación de datos que antes se hacía en el controlador
        String correo = principal.getName();
        Usuario usuario = usuarioService.buscarByUsuario(correo);
        contrato.setUsuario(usuario);
        contrato.setFechaContrato(new Date());

        // Se verifica y setean valores por defecto
        setearValoresPorDefecto(contrato);
        
        // Se guarda el contrato principal primero para obtener su ID
        Contrato contratoGuardado = contratoRepository.save(contrato);

        // Se establecen las relaciones con los clientes y lotes
        if (requestDTO.getIdClientes() != null) {
            for (Integer idCliente : requestDTO.getIdClientes()) {
                Cliente cliente = clienteService.buscarClientePorId(idCliente);
                if (cliente != null) {
                    ContratoClienteId contratoClienteId = new ContratoClienteId(contratoGuardado.getIdContrato(), idCliente);
                    ContratoCliente cc = new ContratoCliente();
                    cc.setId(contratoClienteId);
                    cc.setContrato(contratoGuardado);
                    cc.setCliente(cliente);
                    cc.setTipoPropietario(TipoPropietario.TITULAR);
                    contratoClienteService.guardar(cc);
                }
            }
        }

        if (requestDTO.getIdLotes() != null) {
            for (Integer idLote : requestDTO.getIdLotes()) {
                Lote lote = loteService.obtenerLotePorId(idLote);
                if (lote != null) {
                    ContratoLoteId contratoLoteId = new ContratoLoteId(contratoGuardado.getIdContrato(), idLote);
                    ContratoLote cl = new ContratoLote();
                    cl.setId(contratoLoteId);
                    cl.setContrato(contratoGuardado);
                    cl.setLote(lote);
                    contratoLoteService.guardar(cl);
                }
            }
        }
        
        // Después de guardar todo, se mapea la entidad a un DTO de respuesta
        return mapToContratoResponseDTO(contratoGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContratoResponseDTO> listarContratos() {
        // Obtenemos todos los contratos, y debido a la relación, las listas de ContratoCliente se cargan
        List<Contrato> contratos = contratoRepository.findAll();
        
        // Mapeamos cada entidad de Contrato a un DTO de respuesta
        return contratos.stream()
            .map(this::mapToContratoResponseDTO)
            .collect(Collectors.toList());
    }

    private ContratoResponseDTO mapToContratoResponseDTO(Contrato contrato) {
        ContratoResponseDTO dto = new ContratoResponseDTO(
            contrato.getIdContrato(),
            contrato.getFechaContrato(),
            contrato.getTipoContrato(),
            contrato.getMontoTotal(),
            contrato.getInicial(),
            contrato.getSaldo(),
            contrato.getCantidadLetras(),
            contrato.getObservaciones(),
            null
        );

        if (contrato.getClientes() != null && !contrato.getClientes().isEmpty()) {
            List<ClienteResponseDTO> clienteDTOs = contrato.getClientes().stream()
                .map(contratoCliente -> new ClienteResponseDTO(
                    contratoCliente.getCliente().getIdCliente(),
                    contratoCliente.getCliente().getNombre(),
                    contratoCliente.getCliente().getApellidos(),
                    contratoCliente.getCliente().getNumDoc()
                ))
                .collect(Collectors.toList());
            dto.setClientes(clienteDTOs);
        }
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public ContratoResponseDTO buscarPorId(Integer idContrato) {
        // Busca el contrato por su ID
        Contrato contrato = contratoRepository.findById(idContrato).orElse(null);
        // Si el contrato existe, lo mapea a un DTO de respuesta.
        // Si no se encuentra, el método devuelve null.
        if (contrato != null) {
            return mapToContratoResponseDTO(contrato);
        }
        return null;
    }
    
    @Override
    public void eliminarContrato(Integer idContrato) {
        contratoRepository.deleteById(idContrato);
    }
}