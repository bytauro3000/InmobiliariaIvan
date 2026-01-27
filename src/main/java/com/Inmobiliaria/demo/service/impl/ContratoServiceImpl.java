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
import com.Inmobiliaria.demo.repository.LetraCambioRepository;
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
    @Autowired private LetraCambioRepository letraCambioRepository;

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
    	// 1. Identificar los lotes involucrados (Reutilizando tu lógica existente)
        List<Integer> idsLotesAValidar;
        if (requestDTO.getIdSeparacion() != null) {
            Separacion separacion = separacionService.buscarPorId(requestDTO.getIdSeparacion());
            if (separacion == null) {
                throw new RuntimeException("La separación con ID " + requestDTO.getIdSeparacion() + " no existe.");
            }
            idsLotesAValidar = separacion.getLotes().stream()
                    .map(sl -> sl.getLote().getIdLote()).collect(Collectors.toList());
        } else {
            idsLotesAValidar = requestDTO.getIdLotes();
        }

        // 2. 🔹 VALIDACIÓN TRIPLE (Programa + MZ + Lote)
        if (idsLotesAValidar != null) {
            for (Integer idLote : idsLotesAValidar) {
                // Obtenemos el objeto lote completo para saber su Programa, Mz y número
                Lote loteDB = loteService.obtenerLotePorId(idLote);
                
                if (loteDB != null) {
                    // Verificamos si existe un contrato con estos 3 datos específicos
                    boolean duplicado = contratoRepository.existeContratoDuplicado(
                        loteDB.getPrograma().getIdPrograma(), 
                        loteDB.getManzana(), 
                        loteDB.getNumeroLote()
                    );

                    if (duplicado) {
                        throw new RuntimeException("El lote " + loteDB.getNumeroLote() + 
                            " de la Manzana " + loteDB.getManzana() + 
                            " en el programa " + loteDB.getPrograma().getNombrePrograma() + 
                            " ya tiene un contrato registrado.");
                    }
                }
            }
        }
    	
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
        return contratoRepository.findAllByOrderByIdContratoDesc().stream()
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
        // 1. Buscamos el contrato con sus lotes asociados antes de eliminarlo
        Contrato contrato = contratoRepository.findById(idContrato)
                .orElseThrow(() -> new RuntimeException("No se encontró el contrato con ID: " + idContrato));

        // 2. Si el contrato tiene lotes, debemos regresarlos a estado "Disponible"
        if (contrato.getLotes() != null && !contrato.getLotes().isEmpty()) {
            for (ContratoLote contratoLote : contrato.getLotes()) {
                Lote lote = contratoLote.getLote();
                if (lote != null) {
                    // Cambiamos el estado de 'Vendido' a 'Disponible'
                    lote.setEstado(EstadoLote.Disponible); 
                    loteService.actualizarLote(lote);
                }
            }
        }

        // 3. Si el contrato vino de una separación, SE FINALIZARA
        if (contrato.getSeparacion() != null) {
            Separacion sep = contrato.getSeparacion();
            sep.setEstado(EstadoSeparacion.FINALIZADO); 
            separacionService.actualizarSeparacion(sep);
        }
        contratoRepository.delete(contrato);
    }

    // 🟢 NUEVO: Implementación para generar el PDF
    @Override
    @Transactional(readOnly = true)
    public byte[] generarPdf(Integer idContrato) {
        // 1. Buscamos los datos para el DTO
        ContratoResponseDTO dto = buscarPorId(idContrato);
        if (dto == null) {
            throw new RuntimeException("No se encontró el contrato con ID: " + idContrato);
        }

        // 2. 🔹 IMPORTANTE: Recuperamos la entidad real de la primera letra para obtener el monto exacto
        // Esto evita que el PDF intente calcular un promedio y falle con los decimales
        LetraCambio primeraLetra = letraCambioRepository
                .findFirstByContratoIdContratoOrderByNumeroLetraAsc(idContrato)
                .orElse(null);

        // 3. Pasamos el DTO y la Entidad de la letra al generador
        return PdfGenerator.generarContratoFlorida(dto, primeraLetra);
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

     // 🟢 MAPEADO CORREGIDO: Ahora incluye los 7 parámetros requeridos por el DTO
        if (contrato.getClientes() != null) {
            dto.setClientes(contrato.getClientes().stream()
                .map(cc -> {
                    Cliente c = cc.getCliente();
                    return new ClienteResponseDTO(
                        c.getIdCliente(), 
                        c.getNombre(),
                        c.getApellidos(), 
                        c.getEstadoCivil(),
                        c.getNumDoc(),
                        c.getDireccion(),
                        c.getCelular(),
                        c.getDistrito(),
                        c.getGenero()
                    );
                }).collect(Collectors.toList()));
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