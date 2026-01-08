package com.Inmobiliaria.demo.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Inmobiliaria.demo.dto.*;
import com.Inmobiliaria.demo.entity.*;
import com.Inmobiliaria.demo.enums.EstadoLote;
import com.Inmobiliaria.demo.enums.EstadoSeparacion;
import com.Inmobiliaria.demo.enums.TipoContrato;
import com.Inmobiliaria.demo.enums.TipoPropietario;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import com.Inmobiliaria.demo.service.*;
import com.Inmobiliaria.demo.util.PdfGenerator; // 👈 Importamos tu utilidad

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
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date fecha = dateFormat.parse(requestDTO.getFechaContrato());
            contrato.setFechaContrato(fecha);
        } catch (ParseException e) {
            throw new RuntimeException("Error al parsear la fecha del contrato.", e);
        }

        contrato.setTipoContrato(TipoContrato.valueOf(requestDTO.getTipoContrato()));
        contrato.setMontoTotal(BigDecimal.valueOf(requestDTO.getMontoTotal()));
        contrato.setInicial(BigDecimal.valueOf(requestDTO.getInicial()));
        contrato.setSaldo(BigDecimal.valueOf(requestDTO.getSaldo()));
        contrato.setCantidadLetras(requestDTO.getCantidadLetras());
        contrato.setObservaciones(requestDTO.getObservaciones());

        Vendedor vendedor = null;
        if (requestDTO.getIdSeparacion() != null) {
            Separacion separacion = separacionService.buscarPorId(requestDTO.getIdSeparacion());
            if (separacion != null) {
                contrato.setSeparacion(separacion);
                vendedor = separacion.getVendedor();
            } else {
                throw new RuntimeException("La separación con ID " + requestDTO.getIdSeparacion() + " no existe.");
            }
        } else if (requestDTO.getIdVendedor() != null) {
            vendedor = vendedorService.obtenerVendedorPorId(requestDTO.getIdVendedor())
                    .orElseThrow(() -> new RuntimeException("Vendedor no encontrado"));
        }
        contrato.setVendedor(vendedor);

        String correo = principal.getName();
        Usuario usuario = usuarioService.buscarByUsuario(correo);
        contrato.setUsuario(usuario);

        setearValoresPorDefecto(contrato);

        Contrato contratoGuardado = contratoRepository.save(contrato);

        List<Integer> idsClientesAAsociar;
        if (requestDTO.getIdSeparacion() != null) {
            Separacion separacion = contratoGuardado.getSeparacion();
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

    // 🟢 NUEVO: Implementación para generar el PDF
    @Override
    @Transactional(readOnly = true)
    public byte[] generarPdf(Integer idContrato) {
        ContratoResponseDTO dto = buscarPorId(idContrato);
        if (dto == null) {
            throw new RuntimeException("No se encontró el contrato con ID: " + idContrato);
        }
        return PdfGenerator.generarContratoFlorida(dto);
    }

    private ContratoResponseDTO mapToContratoResponseDTO(Contrato contrato) {
        if (contrato == null) return null;
        
        ContratoResponseDTO dto = new ContratoResponseDTO();
        dto.setIdContrato(contrato.getIdContrato());
        dto.setFechaContrato(contrato.getFechaContrato());
        dto.setTipoContrato(contrato.getTipoContrato());
        dto.setMontoTotal(contrato.getMontoTotal());
        dto.setInicial(contrato.getInicial());
        dto.setSaldo(contrato.getSaldo());
        dto.setCantidadLetras(contrato.getCantidadLetras());
        dto.setObservaciones(contrato.getObservaciones());

        if (contrato.getClientes() != null) {
            dto.setClientes(contrato.getClientes().stream()
                .map(cc -> new ClienteResponseDTO(
                    cc.getCliente().getIdCliente(), 
                    cc.getCliente().getNombre(),
                    cc.getCliente().getApellidos(), 
                    cc.getCliente().getNumDoc()
                )).collect(Collectors.toList()));
        }

        if (contrato.getLotes() != null) {
            dto.setLotes(contrato.getLotes().stream()
                .map(cl -> {
                    Lote l = cl.getLote();
                    return new LoteResponseDTO(
                        l.getManzana(), l.getNumeroLote(), l.getArea(),
                        l.getLargo1(), l.getLargo2(), l.getAncho1(), l.getAncho2(),
                        l.getColindanteNorte(), l.getColindanteSur(),
                        l.getColindanteEste(), l.getColindanteOeste(),
                        l.getPrograma().getNombrePrograma()
                    );
                }).collect(Collectors.toList()));
        }

        if (contrato.getLetrasCambio() != null) {
            dto.setLetras(contrato.getLetrasCambio().stream()
                .map(letra -> new LetraResponseDTO(
                    letra.getNumeroLetra(),
                    letra.getFechaVencimiento(),
                    letra.getImporte(),
                    letra.getImporteLetras()
                )).collect(Collectors.toList()));
        }

        return dto;
    }
}