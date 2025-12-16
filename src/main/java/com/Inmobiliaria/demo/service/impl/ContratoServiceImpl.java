package com.Inmobiliaria.demo.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.security.Principal;
import java.text.SimpleDateFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.ParseException;
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
import com.Inmobiliaria.demo.entity.Vendedor;
import com.Inmobiliaria.demo.enums.EstadoLote;
import com.Inmobiliaria.demo.enums.EstadoSeparacion;
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
import com.Inmobiliaria.demo.service.VendedorService;

@Service
public class ContratoServiceImpl implements ContratoService {

    @Autowired private ContratoRepository contratoRepository;
    @Autowired private ContratoClienteService contratoClienteService;
    @Autowired private ContratoLoteService contratoLoteService;
    @Autowired private ClienteService clienteService;
    @Autowired private LoteService loteService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private SeparacionService separacionService; 
    @Autowired private VendedorService vendedorService;

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
        
        Contrato contrato = new Contrato();
        
        // Convertir la fecha de String a Date de forma segura
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date fecha = dateFormat.parse(requestDTO.getFechaContrato());
            contrato.setFechaContrato(fecha);
        } catch (ParseException e) {
            throw new RuntimeException("Error al parsear la fecha del contrato.", e);
        }

        // Asignar los demás valores del DTO
        contrato.setTipoContrato(TipoContrato.valueOf(requestDTO.getTipoContrato()));
        contrato.setMontoTotal(BigDecimal.valueOf(requestDTO.getMontoTotal()));
        contrato.setInicial(BigDecimal.valueOf(requestDTO.getInicial()));
        contrato.setSaldo(BigDecimal.valueOf(requestDTO.getSaldo()));
        contrato.setCantidadLetras(requestDTO.getCantidadLetras());
        contrato.setObservaciones(requestDTO.getObservaciones());

        // Asignar vendedor (lógica para DIRECTO y SEPARACION)
        Vendedor vendedor = null;
        if (requestDTO.getIdVendedor() != null) {
            vendedor = vendedorService.obtenerVendedorPorId(requestDTO.getIdVendedor())
                                      .orElseThrow(() -> new RuntimeException("Vendedor no encontrado"));
        } else if (requestDTO.getIdSeparacion() != null) {
            Separacion separacion = separacionService.buscarPorId(requestDTO.getIdSeparacion());
            if (separacion != null) {
                contrato.setSeparacion(separacion);
                vendedor = separacion.getVendedor();
            }
        }
        contrato.setVendedor(vendedor);

        // Asignar usuario desde principal
        String correo = principal.getName();
        Usuario usuario = usuarioService.buscarByUsuario(correo);
        contrato.setUsuario(usuario);

        // Ajustar valores por defecto según tipo de contrato
        setearValoresPorDefecto(contrato);

        // Guardar contrato para obtener ID
        Contrato contratoGuardado = contratoRepository.save(contrato);

        // --- INICIO DE LA MODIFICACIÓN APLICADA ---
        List<Integer> idsClientesAAsociar = null;
        
        if (requestDTO.getIdSeparacion() != null) {
            Separacion separacion = separacionService.buscarPorId(requestDTO.getIdSeparacion());
            if (separacion != null) {
                // ✅ Actualizar el estado de la separación
                separacion.setEstado(EstadoSeparacion.CONCRETADO);
                separacionService.actualizarSeparacion(separacion);

                // ✅ NUEVA LÓGICA: Obtener IDs desde las listas de Separación
                idsClientesAAsociar = separacion.getClientes().stream()
                        .map(sc -> sc.getCliente().getIdCliente())
                        .collect(Collectors.toList());

                List<Integer> idsLotesAAsociar = separacion.getLotes().stream()
                        .map(sl -> sl.getLote().getIdLote())
                        .collect(Collectors.toList());

                for (Integer idLote : idsLotesAAsociar) {
                    Lote lote = loteService.obtenerLotePorId(idLote);
                    if (lote != null) {
                        lote.setEstado(EstadoLote.Vendido);
                        loteService.actualizarLote(lote);
                        ContratoLoteId clId = new ContratoLoteId(contratoGuardado.getIdContrato(), idLote);
                        ContratoLote cl = new ContratoLote();
                        cl.setId(clId);
                        cl.setContrato(contratoGuardado);
                        cl.setLote(lote);
                        contratoLoteService.guardar(cl);
                    }
                }
            }
        } else {
            // Lógica para contrato directo (sin separación)
            idsClientesAAsociar = requestDTO.getIdClientes();
            if (requestDTO.getIdLotes() != null) {
                for (Integer idLote : requestDTO.getIdLotes()) {
                    Lote lote = loteService.obtenerLotePorId(idLote);
                    if (lote != null) {
                        lote.setEstado(EstadoLote.Vendido);
                        loteService.actualizarLote(lote);
                        ContratoLoteId clId = new ContratoLoteId(contratoGuardado.getIdContrato(), idLote);
                        ContratoLote cl = new ContratoLote();
                        cl.setId(clId);
                        cl.setContrato(contratoGuardado);
                        cl.setLote(lote);
                        contratoLoteService.guardar(cl);
                    }
                }
            }
        }
        // --- FIN DE LA MODIFICACIÓN APLICADA ---

        if (idsClientesAAsociar != null && !idsClientesAAsociar.isEmpty()) {
            for (Integer idCliente : idsClientesAAsociar) {
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
