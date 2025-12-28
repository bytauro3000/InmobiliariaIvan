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
import com.Inmobiliaria.demo.entity.*;
import com.Inmobiliaria.demo.enums.EstadoLote;
import com.Inmobiliaria.demo.enums.EstadoSeparacion;
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
    @Autowired private SeparacionService separacionService; 
    @Autowired private VendedorService vendedorService;

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
        
        // 1. Parsear Fecha
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date fecha = dateFormat.parse(requestDTO.getFechaContrato());
            contrato.setFechaContrato(fecha);
        } catch (ParseException e) {
            throw new RuntimeException("Error al parsear la fecha del contrato.", e);
        }

        // 2. Asignar Valores Básicos
        contrato.setTipoContrato(TipoContrato.valueOf(requestDTO.getTipoContrato()));
        contrato.setMontoTotal(BigDecimal.valueOf(requestDTO.getMontoTotal()));
        contrato.setInicial(BigDecimal.valueOf(requestDTO.getInicial()));
        contrato.setSaldo(BigDecimal.valueOf(requestDTO.getSaldo()));
        contrato.setCantidadLetras(requestDTO.getCantidadLetras());
        contrato.setObservaciones(requestDTO.getObservaciones());

        // 3. Lógica para asignar Vendedor y SEPARACIÓN (Crucial)
        Vendedor vendedor = null;
        if (requestDTO.getIdSeparacion() != null) {
            // 🟢 SOLUCIÓN: Buscamos la entidad separación y la asignamos al contrato
            Separacion separacion = separacionService.buscarPorId(requestDTO.getIdSeparacion());
            if (separacion != null) {
                contrato.setSeparacion(separacion); // 👈 ESTA LÍNEA ES LA QUE FALTABA
                vendedor = separacion.getVendedor();
            } else {
                throw new RuntimeException("La separación con ID " + requestDTO.getIdSeparacion() + " no existe.");
            }
        } else if (requestDTO.getIdVendedor() != null) {
            vendedor = vendedorService.obtenerVendedorPorId(requestDTO.getIdVendedor())
                                      .orElseThrow(() -> new RuntimeException("Vendedor no encontrado"));
        }
        contrato.setVendedor(vendedor);

        // 4. Asignar Usuario
        String correo = principal.getName();
        Usuario usuario = usuarioService.buscarByUsuario(correo);
        contrato.setUsuario(usuario);

        setearValoresPorDefecto(contrato);

        // 5. GUARDAR CABECERA (Aquí se insertará el id_separacion en la DB)
        Contrato contratoGuardado = contratoRepository.save(contrato);

        // 6. Procesar Clientes y Lotes
        List<Integer> idsClientesAAsociar;
        
        if (requestDTO.getIdSeparacion() != null) {
            Separacion separacion = contratoGuardado.getSeparacion();
            
            // Actualizar estado de la separación
            separacion.setEstado(EstadoSeparacion.CONCRETADO);
            separacionService.actualizarSeparacion(separacion);

            idsClientesAAsociar = separacion.getClientes().stream()
                    .map(sc -> sc.getCliente().getIdCliente()).collect(Collectors.toList());

            List<Integer> idsLotesAAsociar = separacion.getLotes().stream()
                    .map(sl -> sl.getLote().getIdLote()).collect(Collectors.toList());

            for (Integer idLote : idsLotesAAsociar) {
                registrarLoteEnContrato(contratoGuardado, idLote);
            }
        } else {
            idsClientesAAsociar = requestDTO.getIdClientes();
            if (requestDTO.getIdLotes() != null) {
                for (Integer idLote : requestDTO.getIdLotes()) {
                    registrarLoteEnContrato(contratoGuardado, idLote);
                }
            }
        }

        // Asociar Clientes
        if (idsClientesAAsociar != null) {
            for (Integer idCliente : idsClientesAAsociar) {
                Cliente cliente = clienteService.buscarClientePorId(idCliente);
                if (cliente != null) {
                    ContratoCliente cc = new ContratoCliente();
                    cc.setId(new ContratoClienteId(contratoGuardado.getIdContrato(), idCliente));
                    cc.setContrato(contratoGuardado);
                    cc.setCliente(cliente);
                    cc.setTipoPropietario(TipoPropietario.TITULAR);
                    contratoClienteService.guardar(cc);
                }
            }
        }

        return mapToContratoResponseDTO(contratoGuardado);
    }

    private void registrarLoteEnContrato(Contrato contrato, Integer idLote) {
        Lote lote = loteService.obtenerLotePorId(idLote);
        if (lote != null) {
            lote.setEstado(EstadoLote.Vendido);
            loteService.actualizarLote(lote);
            ContratoLote cl = new ContratoLote();
            cl.setId(new ContratoLoteId(contrato.getIdContrato(), idLote));
            cl.setContrato(contrato);
            cl.setLote(lote);
            contratoLoteService.guardar(cl);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContratoResponseDTO> listarContratos() {
        return contratoRepository.findAll().stream()
                .map(this::mapToContratoResponseDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ContratoResponseDTO buscarPorId(Integer idContrato) {
        return contratoRepository.findById(idContrato)
                .map(this::mapToContratoResponseDTO).orElse(null);
    }

    @Override
    @Transactional
    public void eliminarContrato(Integer idContrato) {
        contratoRepository.deleteById(idContrato);
    }

    private ContratoResponseDTO mapToContratoResponseDTO(Contrato contrato) {
        if (contrato == null) return null;
        ContratoResponseDTO dto = new ContratoResponseDTO(
            contrato.getIdContrato(), contrato.getFechaContrato(), contrato.getTipoContrato(),
            contrato.getMontoTotal(), contrato.getInicial(), contrato.getSaldo(),
            contrato.getCantidadLetras(), contrato.getObservaciones(), null
        );
        if (contrato.getClientes() != null) {
            dto.setClientes(contrato.getClientes().stream()
                .map(cc -> new ClienteResponseDTO(
                    cc.getCliente().getIdCliente(), cc.getCliente().getNombre(),
                    cc.getCliente().getApellidos(), cc.getCliente().getNumDoc()
                )).collect(Collectors.toList()));
        }
        return dto;
    }
}