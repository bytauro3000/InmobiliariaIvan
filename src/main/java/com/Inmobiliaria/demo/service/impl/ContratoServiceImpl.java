package com.Inmobiliaria.demo.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Inmobiliaria.demo.dto.ClienteResponseDTO;
import com.Inmobiliaria.demo.dto.ContratoRequestDTO;
import com.Inmobiliaria.demo.dto.ContratoResponseDTO;
import com.Inmobiliaria.demo.entity.Cliente;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.ContratoCliente;
import com.Inmobiliaria.demo.entity.ContratoClienteId;
import com.Inmobiliaria.demo.entity.ContratoLote;
import com.Inmobiliaria.demo.entity.ContratoLoteId;
import com.Inmobiliaria.demo.entity.Lote;
import com.Inmobiliaria.demo.entity.Separacion;
import com.Inmobiliaria.demo.entity.Usuario;
import com.Inmobiliaria.demo.enums.EstadoLote;
import com.Inmobiliaria.demo.enums.TipoContrato;
import com.Inmobiliaria.demo.enums.TipoPropietario;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import com.Inmobiliaria.demo.service.ClienteService;
import com.Inmobiliaria.demo.service.ContratoClienteService;
import com.Inmobiliaria.demo.service.ContratoLoteService;
import com.Inmobiliaria.demo.service.ContratoService;
import com.Inmobiliaria.demo.service.LoteService;
import com.Inmobiliaria.demo.service.SeparacionService;
import com.Inmobiliaria.demo.service.UsuarioService;

@Service
public class ContratoServiceImpl implements ContratoService {

    @Autowired private ContratoRepository contratoRepository;
    @Autowired private ContratoClienteService contratoClienteService;
    @Autowired private ContratoLoteService contratoLoteService;
    @Autowired private ClienteService clienteService;
    @Autowired private LoteService loteService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private SeparacionService separacionService; 

    // Setear valores por defecto si el contrato es de tipo CONTADO
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
        // Construir objeto Contrato desde los datos del DTO
        Contrato contrato = new Contrato();

        // Convertir y asignar valores del DTO al Contrato
        contrato.setFechaContrato(new Date()); // Fecha actual
        contrato.setTipoContrato(TipoContrato.valueOf(requestDTO.getTipoContrato())); // Enum
        contrato.setMontoTotal(BigDecimal.valueOf(requestDTO.getMontoTotal()));
        contrato.setInicial(BigDecimal.valueOf(requestDTO.getInicial()));
        contrato.setSaldo(BigDecimal.valueOf(requestDTO.getSaldo()));
        contrato.setCantidadLetras(requestDTO.getCantidadLetras());
        contrato.setObservaciones(requestDTO.getObservaciones());

        // Si se provee separacion, la asignamos
        if (requestDTO.getIdSeparacion() != null) {
            Separacion separacion = separacionService.buscarPorId(requestDTO.getIdSeparacion());
            contrato.setSeparacion(separacion);
        }

        // Asignar usuario desde principal (seguridad)
        String correo = principal.getName();
        Usuario usuario = usuarioService.buscarByUsuario(correo);
        contrato.setUsuario(usuario);

        // Ajustar valores por defecto según tipo de contrato
        setearValoresPorDefecto(contrato);

        // Guardar contrato para obtener ID
        Contrato contratoGuardado = contratoRepository.save(contrato);

        // Asociar clientes al contrato
        if (requestDTO.getIdClientes() != null) {
            for (Integer idCliente : requestDTO.getIdClientes()) {
                Cliente cliente = clienteService.buscarClientePorId(idCliente);
                if (cliente != null) {
                    ContratoClienteId ccId = new ContratoClienteId(contratoGuardado.getIdContrato(), idCliente);
                    ContratoCliente cc = new ContratoCliente();
                    cc.setId(ccId);
                    cc.setContrato(contratoGuardado);
                    cc.setCliente(cliente);
                    cc.setTipoPropietario(TipoPropietario.TITULAR);
                    contratoClienteService.guardar(cc);
                }
            }
        }

        // Asociar lotes al contrato
        if (requestDTO.getIdLotes() != null) {
            for (Integer idLote : requestDTO.getIdLotes()) {
                Lote lote = loteService.obtenerLotePorId(idLote);
                if (lote != null) {
                    
                    // ✅ PASO 1: Cambiar el estado del lote a 'Vendido'
                    lote.setEstado(EstadoLote.Vendido); 
                    
                    // ✅ PASO 2: Guardar los cambios en el lote
                    loteService.actualizarLote(lote); 

                    // ✅ PASO 3: Asociar el lote al contrato
                    ContratoLoteId clId = new ContratoLoteId(contratoGuardado.getIdContrato(), idLote);
                    ContratoLote cl = new ContratoLote();
                    cl.setId(clId);
                    cl.setContrato(contratoGuardado);
                    cl.setLote(lote);
                    contratoLoteService.guardar(cl);
                }
            }
        }
        // Mapear a DTO y devolver
        return mapToContratoResponseDTO(contratoGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContratoResponseDTO> listarContratos() {
        List<Contrato> contratos = contratoRepository.findAll();

        return contratos.stream()
                .map(this::mapToContratoResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ContratoResponseDTO buscarPorId(Integer idContrato) {
        Contrato contrato = contratoRepository.findById(idContrato).orElse(null);
        if (contrato != null) {
            return mapToContratoResponseDTO(contrato);
        }
        return null;
    }

    @Override
    public void eliminarContrato(Integer idContrato) {
        contratoRepository.deleteById(idContrato);
    }

    // Método para mapear entidad Contrato a DTO
    private ContratoResponseDTO mapToContratoResponseDTO(Contrato contrato) {
        if (contrato == null) return null;

        ContratoResponseDTO dto = new ContratoResponseDTO(
            contrato.getIdContrato(),
            contrato.getFechaContrato(),
            contrato.getTipoContrato(),
            contrato.getMontoTotal(),
            contrato.getInicial(),
            contrato.getSaldo(),
            contrato.getCantidadLetras(),
            contrato.getObservaciones(),
            null // Aquí puedes mapear la lista de clientes si quieres
        );

        // Mapear clientes si existen
        if (contrato.getClientes() != null && !contrato.getClientes().isEmpty()) {
            List<ClienteResponseDTO> clienteDTOs = contrato.getClientes().stream()
                .map(cc -> new ClienteResponseDTO(
                    cc.getCliente().getIdCliente(),
                    cc.getCliente().getNombre(),
                    cc.getCliente().getApellidos(),
                    cc.getCliente().getNumDoc()
                ))
                .collect(Collectors.toList());
            dto.setClientes(clienteDTOs);
        }

        return dto;
    }
}
