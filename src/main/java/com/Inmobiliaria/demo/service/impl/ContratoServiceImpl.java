package com.Inmobiliaria.demo.service.impl;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Inmobiliaria.demo.client.InscripcionClient;
import com.Inmobiliaria.demo.dto.*;
import com.Inmobiliaria.demo.dto.TransferenciaResponseDTO;
import com.Inmobiliaria.demo.entity.*;
import com.Inmobiliaria.demo.enums.EstadoLetra;
import com.Inmobiliaria.demo.enums.EstadoLote;
import com.Inmobiliaria.demo.enums.EstadoContrato;
import com.Inmobiliaria.demo.enums.EstadoSeparacion;
import com.Inmobiliaria.demo.enums.TipoContrato;
import com.Inmobiliaria.demo.enums.TipoPropietario;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import com.Inmobiliaria.demo.repository.LetraCambioRepository;
import com.Inmobiliaria.demo.service.*;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.util.PdfGenerator; // 👈 Importamos tu utilidad

@Service
@CacheConfig(cacheNames = "contratos")
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
    @Autowired private InscripcionClient inscripcionClient;
    @Autowired private ModelMapper modelMapper;

    private void setearValoresPorDefecto(Contrato contrato) {
        if (contrato.getTipoContrato() == TipoContrato.CONTADO) {
            contrato.setCantidadLetras(0);
            contrato.setInicial(BigDecimal.ZERO);
            contrato.setSaldo(BigDecimal.ZERO);
        }
    }

    @Override
    @Transactional
    @CacheEvict(allEntries = true)
    public ContratoResponseDTO guardarContrato(ContratoRequestDTO requestDTO, Principal principal) {
        // 1. Identificar los lotes involucrados (Mantiene tu lógica original)
        List<Integer> idsLotesAValidar;
        if (requestDTO.getIdSeparacion() != null) {
            Separacion separacion = separacionService.buscarPorId(requestDTO.getIdSeparacion());
            if (separacion == null) {
                throw new NegocioException("La separacion con ID " + requestDTO.getIdSeparacion() + " no existe.");
            }
            idsLotesAValidar = separacion.getLotes().stream()
                    .map(sl -> sl.getLote().getIdLote()).collect(Collectors.toList());
        } else {
            idsLotesAValidar = requestDTO.getIdLotes();
        }

        // 2. 🔹 VALIDACIÓN TRIPLE (Programa + MZ + Lote - Mantiene tu lógica original)
        if (idsLotesAValidar != null) {
            for (Integer idLote : idsLotesAValidar) {
                Lote loteDB = loteService.obtenerLotePorId(idLote);
                if (loteDB != null) {
                    // Estados terminales: el lote puede reasignarse a un nuevo contrato
                    java.util.List<EstadoContrato> estadosTerminales = java.util.Arrays.asList(
                        EstadoContrato.TRANSFERIDO,
                        EstadoContrato.RENUNCIA,
                        EstadoContrato.RESUELTO,
                        EstadoContrato.CANCELADO
                    );
                    boolean duplicado = contratoRepository.existeContratoDuplicado(
                        loteDB.getPrograma().getIdPrograma(),
                        loteDB.getManzana(),
                        loteDB.getNumeroLote(),
                        estadosTerminales
                    );
                    if (duplicado) {
                        throw new NegocioException("El lote " + loteDB.getNumeroLote() +
                            " de la Manzana " + loteDB.getManzana() +
                            " en el programa " + loteDB.getPrograma().getNombrePrograma() +
                            " ya tiene un contrato activo registrado.");
                    }
                }
            }
        }

        // ModelMapper convierte automáticamente el DTO a Entidad basándose en nombres de campos.
        Contrato contrato = modelMapper.map(requestDTO, Contrato.class);

        // 4. Parseo de fecha (Se mantiene manual para asegurar el formato yyyyy-MM-dd)
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date fecha = dateFormat.parse(requestDTO.getFechaContrato());
            contrato.setFechaContrato(fecha);
        } catch (ParseException e) {
            throw new RuntimeException("Error al parsear la fecha del contrato.", e);
        }

        // 5. Lógica de Vendedor (Mantiene tu lógica original)
        Vendedor vendedor = null;
        if (requestDTO.getIdSeparacion() != null) {
            Separacion separacion = separacionService.buscarPorId(requestDTO.getIdSeparacion());
            if (separacion != null) {
                contrato.setSeparacion(separacion);
                vendedor = separacion.getVendedor();
            }
        } else if (requestDTO.getIdVendedor() != null) {
            vendedor = vendedorService.obtenerVendedorPorId(requestDTO.getIdVendedor())
                    .orElseThrow(() -> new NegocioException("Vendedor no encontrado"));
        }
        contrato.setVendedor(vendedor);

        // 6. Lógica de Usuario (Mantiene tu lógica original)
        String correo = principal.getName();
        Usuario usuario = usuarioService.buscarByUsuario(correo);
        contrato.setUsuario(usuario);

        setearValoresPorDefecto(contrato);
        Contrato contratoGuardado = contratoRepository.save(contrato);

        // 7. Gestión de Relaciones (Clientes y Lotes - Mantiene tu lógica original)
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
    
    @Override
    @Transactional
    @CacheEvict(allEntries = true) //Borra el caché para forzar la actualización
    public ContratoResponseDTO actualizarContrato(Integer id, ContratoRequestDTO requestDTO) {
        // 1. Buscar el contrato existente
        Contrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new NegocioException("Contrato no encontrado con ID: " + id));

        // 2. 🔹 VALIDACIÓN DE DUPLICADOS (Excluyendo el contrato actual)
        if (requestDTO.getIdLotes() != null) {
            for (Integer idLote : requestDTO.getIdLotes()) {
                Lote loteDB = loteService.obtenerLotePorId(idLote);
                if (loteDB != null) {
                    // Pasamos el 'id' (el contrato que estamos editando) para que lo ignore en la búsqueda
                    java.util.List<EstadoContrato> estadosTerminales = java.util.Arrays.asList(
                        EstadoContrato.TRANSFERIDO,
                        EstadoContrato.RENUNCIA,
                        EstadoContrato.RESUELTO,
                        EstadoContrato.CANCELADO
                    );
                    boolean duplicado = contratoRepository.existeContratoDuplicadoParaOtroContrato(
                        loteDB.getPrograma().getIdPrograma(),
                        loteDB.getManzana(),
                        loteDB.getNumeroLote(),
                        id,
                        estadosTerminales
                    );

                    if (duplicado) {
                        throw new NegocioException("El lote " + loteDB.getNumeroLote() +
                            " de la Manzana " + loteDB.getManzana() +
                            " ya está registrado en OTRO contrato activo.");
                    }
                }
            }
        }

        // 3. GESTIÓN DE LOTES ANTERIORES
        // Antes de limpiar, ponemos los lotes actuales como 'Disponible'
        for (ContratoLote cl : contrato.getLotes()) {
            Lote loteAnterior = cl.getLote();
            loteAnterior.setEstado(EstadoLote.Disponible);
            loteService.actualizarLote(loteAnterior);
        }
        contrato.getLotes().clear();

        // 4. ACTUALIZAR DATOS BÁSICOS
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            contrato.setFechaContrato(dateFormat.parse(requestDTO.getFechaContrato()));
        } catch (ParseException e) {
            throw new NegocioException("Formato de fecha invalido. Use el formato yyyy-MM-dd");
        }
        contrato.setMontoTotal(BigDecimal.valueOf(requestDTO.getMontoTotal()));
        contrato.setInicial(BigDecimal.valueOf(requestDTO.getInicial()));
        contrato.setSaldo(BigDecimal.valueOf(requestDTO.getSaldo()));
        contrato.setCantidadLetras(requestDTO.getCantidadLetras());
        contrato.setObservaciones(requestDTO.getObservaciones());

        // 5. LIMPIAR RELACIONES ANTIGUAS (Casos de orphanRemoval)
        contrato.getLetrasCambio().clear();
        contrato.getClientes().clear();

        // 6. GUARDAR Y ASOCIAR NUEVOS DATOS
        Contrato contratoActualizado = contratoRepository.saveAndFlush(contrato);

        // Registrar nuevos lotes (esto cambiará su estado a 'Vendido')
        if (requestDTO.getIdLotes() != null) {
            for (Integer idLote : requestDTO.getIdLotes()) {
                registrarLoteEnContrato(contratoActualizado, idLote);
            }
        }

        // Registrar nuevos clientes
        if (requestDTO.getIdClientes() != null) {
            for (Integer idCliente : requestDTO.getIdClientes()) {
                Cliente cliente = clienteService.buscarClientePorId(idCliente);
                ContratoCliente cc = new ContratoCliente();
                cc.setId(new ContratoClienteId(contratoActualizado.getIdContrato(), idCliente));
                cc.setContrato(contratoActualizado);
                cc.setCliente(cliente);
                cc.setTipoPropietario(TipoPropietario.TITULAR);
                contratoClienteService.guardar(cc);
            }
        }

        return mapToContratoResponseDTO(contratoActualizado);
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
    @Cacheable
    public List<ContratoResponseDTO> listarContratos() {
        // 1. Obtener todos los contratos
        List<Contrato> contratos = contratoRepository.findAllByOrderByIdContratoDesc();

        // 2. Obtener listas de IDs inscritos desde el Microservicio
        // Usamos listas vacías por defecto por si el MS falla o no hay datos
        List<Integer> idsConLuz = List.of();
        List<Integer> idsConAgua = List.of();

        try {
            idsConLuz = inscripcionClient.obtenerContratosPorServicio("LUZ");
            idsConAgua = inscripcionClient.obtenerContratosPorServicio("AGUA");
        } catch (Exception e) {
            // Logueamos el error pero permitimos que la lista de contratos cargue
            System.err.println("Error al consultar Microservicio de Inscripciones: " + e.getMessage());
        }

        // 3. Mapear y asignar booleanos
        final List<Integer> finalIdsConLuz = idsConLuz;
        final List<Integer> finalIdsConAgua = idsConAgua;

        return contratos.stream()
                .map(contrato -> {
                    ContratoResponseDTO dto = this.mapToContratoResponseDTO(contrato);
                    //Llenamos los booleanos comparando el ID del contrato actual
                    dto.setTieneLuz(finalIdsConLuz.contains(contrato.getIdContrato()));
                    dto.setTieneAgua(finalIdsConAgua.contains(contrato.getIdContrato()));
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    @Cacheable(key = "#idContrato")
    public ContratoResponseDTO buscarPorId(Integer idContrato) {
        Contrato contrato = contratoRepository.findById(idContrato).orElse(null);
        if (contrato == null) return null;

        ContratoResponseDTO dto = this.mapToContratoResponseDTO(contrato);

        try {
            // Verificamos si este ID específico está en las listas del MS
            boolean luz = inscripcionClient.obtenerContratosPorServicio("LUZ").contains(idContrato);
            boolean agua = inscripcionClient.obtenerContratosPorServicio("AGUA").contains(idContrato);
            dto.setTieneLuz(luz);
            dto.setTieneAgua(agua);
        } catch (Exception e) {
            System.err.println("Error al consultar servicios para contrato individual: " + idContrato);
        }

        return dto;
    }
    
    @Override
    @Transactional(readOnly = true)
    public ContratoResponseDTO buscarPorProgramaManzanaLote(Integer idPrograma, String manzana, String numeroLote) {
        Contrato contrato = contratoRepository.findByProgramaManzanaLote(idPrograma, manzana, numeroLote)
                .orElseThrow(() -> new NegocioException(
                    "No se encontró un contrato para el lote: Programa " + idPrograma +
                    ", Manzana " + manzana + ", Lote " + numeroLote
                ));
        
        // Usamos el método existente para mapear a DTO (incluye clientes, lotes, letras)
        return mapToContratoResponseDTO(contrato);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ContratoResponseDTO> buscarPorNombreCliente(String termino) {
        // 1. Buscar contratos cuyos clientes coincidan con el término
        List<Contrato> contratos = contratoRepository.findByClienteNombreContaining(termino);

        // 2. Obtener IDs del microservicio (igual que listarContratos)
        List<Integer> idsConLuz = List.of();
        List<Integer> idsConAgua = List.of();

        try {
            idsConLuz  = inscripcionClient.obtenerContratosPorServicio("LUZ");
            idsConAgua = inscripcionClient.obtenerContratosPorServicio("AGUA");
        } catch (Exception e) {
            System.err.println("Error al consultar Microservicio: " + e.getMessage());
        }

        // 3. Mapear igual que listarContratos
        final List<Integer> finalIdsConLuz  = idsConLuz;
        final List<Integer> finalIdsConAgua = idsConAgua;

        return contratos.stream()
                .map(contrato -> {
                    ContratoResponseDTO dto = this.mapToContratoResponseDTO(contrato);
                    dto.setTieneLuz(finalIdsConLuz.contains(contrato.getIdContrato()));
                    dto.setTieneAgua(finalIdsConAgua.contains(contrato.getIdContrato()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(allEntries = true) //Limpia el caché tras eliminar
    public void eliminarContrato(Integer idContrato) {
        // 1. Buscamos el contrato con sus lotes asociados antes de eliminarlo
        Contrato contrato = contratoRepository.findById(idContrato)
                .orElseThrow(() -> new NegocioException("No se encontro el contrato con ID: " + idContrato));

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
        
    	// Se consulta directamente al repositorio ignorando el caché para garantizar 
        // que el PDF incluya las letras de cambio recién generadas o actualizadas.
        Contrato contrato = contratoRepository.findById(idContrato)
                .orElseThrow(() -> new NegocioException("No se encontro el contrato con ID: " + idContrato));
    
        // Mapeo manual a DTO para procesar la entidad fresca de la base de datos.
        ContratoResponseDTO dto = this.mapToContratoResponseDTO(contrato);

        LetraCambio primeraLetra = letraCambioRepository
                .findFirstByContratoIdContratoOrderByNumeroLetraAsc(idContrato)
                .orElse(null);

        return PdfGenerator.generarContratoFlorida(dto, primeraLetra);
    }
    
    
 // Este método sirve para cuando NO conoces el estado de luz/agua todavía
    private ContratoResponseDTO mapToContratoResponseDTO(Contrato contrato) {
        return mapToContratoResponseDTO(contrato, false, false);
    } 
    
    
    private ContratoResponseDTO mapToContratoResponseDTO(Contrato contrato, boolean luz, boolean agua) {
        if (contrato == null) return null;

        // 1. ModelMapper mapea los datos básicos que coinciden directamente (ID, Fecha, Montos, etc.)
        ContratoResponseDTO dto = modelMapper.map(contrato, ContratoResponseDTO.class);

        // 2. Asignamos los booleanos que vienen calculados desde el Microservicio
        dto.setTieneLuz(luz);
        dto.setTieneAgua(agua);
        dto.setVendedor(contrato.getVendedor());

        // 3. 🚨 MAPEADO DE CLIENTES: Navegamos manualmente por la tabla intermedia ContratoCliente
        if (contrato.getClientes() != null) {
            dto.setClientes(contrato.getClientes().stream()
                .map(cc -> {
                    Cliente c = cc.getCliente(); // Extraemos la entidad Cliente
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

        // 4. 🚨 MAPEADO DE LOTES: Navegamos manualmente por la tabla intermedia ContratoLote
        if (contrato.getLotes() != null) {
            dto.setLotes(contrato.getLotes().stream()
                .map(cl -> {
                    Lote l = cl.getLote(); // Extraemos la entidad Lote
                    return new LoteResponseDTO(
                        l.getIdLote(),
                        l.getManzana(), 
                        l.getNumeroLote(), 
                        l.getArea(),
                        l.getLargo1(), 
                        l.getLargo2(), 
                        l.getAncho1(), 
                        l.getAncho2(),
                        l.getColindanteNorte(), 
                        l.getColindanteSur(),
                        l.getColindanteEste(), 
                        l.getColindanteOeste(),
                        l.getPrograma().getNombrePrograma()
                    );
                }).collect(Collectors.toList()));
        }

        // 5. 🚨 MAPEADO DE LETRAS: Mantenemos la estructura de conversión de letras de cambio
        if (contrato.getLetrasCambio() != null) {
            dto.setLetras(contrato.getLetrasCambio().stream()
                .sorted(Comparator.comparing(l -> {
                    String numStr = l.getNumeroLetra().split("/")[0];
                    return Integer.parseInt(numStr);
                }))
                .map(letra -> new LetraResponseDTO(
                    letra.getNumeroLetra(), 
                    letra.getFechaVencimiento(),
                    letra.getImporte(), 
                    letra.getImporteLetras()
                )).collect(Collectors.toList()));
        }
        return dto;
    }

    /**
     * Cambia el estado de un contrato manualmente (secretaria).
     * Solo permite transiciones válidas según el flujo legal del contrato.
     * Al pasar a RESUELTO libera automáticamente los lotes asociados.
     */
    @Override
    @Transactional
    @CacheEvict(allEntries = true)
    public ContratoResponseDTO cambiarEstado(Integer idContrato, String nuevoEstadoStr) {

        Contrato contrato = contratoRepository.findById(idContrato)
            .orElseThrow(() -> new NegocioException("Contrato no encontrado con ID: " + idContrato));

        EstadoContrato estadoActual = contrato.getEstadoContrato();
        if (estadoActual == null) estadoActual = EstadoContrato.ACTIVO;

        EstadoContrato nuevoEstado;
        try {
            nuevoEstado = EstadoContrato.valueOf(nuevoEstadoStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NegocioException("Estado inválido: " + nuevoEstadoStr);
        }

        // Validar transiciones permitidas
        validarTransicion(estadoActual, nuevoEstado);

        contrato.setEstadoContrato(nuevoEstado);

        // Al resolver: liberar lotes → vuelven a DISPONIBLE (cláusula 7/8)
        if (nuevoEstado == EstadoContrato.RESUELTO) {
            contrato.getLotes().forEach(cl -> {
                Lote lote = cl.getLote();
                lote.setEstado(EstadoLote.Disponible);
            });
        }

        Contrato guardado = contratoRepository.save(contrato);
        return mapToContratoResponseDTO(guardado);
    }

    /**
     * Valida las transiciones MANUALES permitidas (secretaria).
     * - ACTIVO ↔ MORA: scheduler automático
     * - ANY → CANCELADO: automático al pagar última letra (PagoLetraServiceImpl)
     * - ANY → RENUNCIA/TRANSFERIDO: métodos dedicados (registrarRenuncia/registrarTransferencia)
     * - Transiciones manuales vía cambiarEstado():
     *     MORA → CARTA_NOTARIAL
     *     CARTA_NOTARIAL → EN_RESOLUCION | ACTIVO (reactivar)
     *     EN_RESOLUCION → RESUELTO
     */
    private void validarTransicion(EstadoContrato actual, EstadoContrato nuevo) {
        boolean valida = switch (actual) {
            case MORA           -> nuevo == EstadoContrato.CARTA_NOTARIAL;
            case CARTA_NOTARIAL -> nuevo == EstadoContrato.EN_RESOLUCION || nuevo == EstadoContrato.ACTIVO;
            case EN_RESOLUCION  -> nuevo == EstadoContrato.RESUELTO;
            case ACTIVO, RESUELTO, CANCELADO, RENUNCIA, TRANSFERIDO -> false;
        };

        if (!valida) {
            throw new NegocioException(
                "Transición no permitida: " + actual + " → " + nuevo
            );
        }
    }

    // ─── RENUNCIA ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(allEntries = true)
    public ContratoResponseDTO registrarRenuncia(Integer idContrato) {
        Contrato contrato = contratoRepository.findById(idContrato)
            .orElseThrow(() -> new NegocioException("Contrato no encontrado: " + idContrato));

        EstadoContrato estado = contrato.getEstadoContrato();
        if (estado != EstadoContrato.ACTIVO && estado != EstadoContrato.MORA) {
            throw new NegocioException(
                "Solo se puede registrar renuncia desde estado ACTIVO o MORA. Estado actual: " + estado
            );
        }

        // 1. Cambiar estado del contrato
        contrato.setEstadoContrato(EstadoContrato.RENUNCIA);

        // 2. Liberar lotes → Disponible
        contrato.getLotes().forEach(cl -> cl.getLote().setEstado(EstadoLote.Disponible));

        // 3. Cancelar letras pendientes/vencidas (las pagadas se conservan)
        contrato.getLetrasCambio().stream()
            .filter(l -> l.getEstadoLetra() == EstadoLetra.PENDIENTE
                      || l.getEstadoLetra() == EstadoLetra.VENCIDO)
            .forEach(l -> l.setEstadoLetra(EstadoLetra.PAGADO)); // marcamos como cerradas

        Contrato guardado = contratoRepository.save(contrato);
        return mapToContratoResponseDTO(guardado);
    }

    // ─── TRANSFERENCIA ────────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(allEntries = true)
    public TransferenciaResponseDTO registrarTransferencia(Integer idContrato) {
        Contrato contrato = contratoRepository.findById(idContrato)
            .orElseThrow(() -> new NegocioException("Contrato no encontrado: " + idContrato));

        EstadoContrato estado = contrato.getEstadoContrato();
        if (estado != EstadoContrato.ACTIVO && estado != EstadoContrato.MORA) {
            throw new NegocioException(
                "Solo se puede transferir desde estado ACTIVO o MORA. Estado actual: " + estado
            );
        }

        // 1. Calcular montos
        java.math.BigDecimal montoPagado = contrato.getLetrasCambio().stream()
            .filter(l -> l.getEstadoLetra() == EstadoLetra.PAGADO)
            .map(LetraCambio::getImporte)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        long letrasRestantes = contrato.getLetrasCambio().stream()
            .filter(l -> l.getEstadoLetra() == EstadoLetra.PENDIENTE
                      || l.getEstadoLetra() == EstadoLetra.VENCIDO)
            .count();

        java.math.BigDecimal saldoPendiente = contrato.getMontoTotal().subtract(montoPagado);

        // 2. Marcar contrato como TRANSFERIDO (el lote no cambia — sigue Vendido)
        contrato.setEstadoContrato(EstadoContrato.TRANSFERIDO);
        contratoRepository.save(contrato);

        // 3. Armar lista de IDs de lotes para el nuevo contrato
        List<Integer> idLotes = contrato.getLotes().stream()
            .map(cl -> cl.getLote().getIdLote())
            .collect(Collectors.toList());

        // 4. Armar lotes DTO para mostrar info
        List<LoteResponseDTO> lotesDto = contrato.getLotes().stream()
            .map(cl -> {
                Lote l = cl.getLote();
                return new LoteResponseDTO(
                    l.getIdLote(), l.getManzana(), l.getNumeroLote(), l.getArea(),
                    l.getLargo1(), l.getLargo2(), l.getAncho1(), l.getAncho2(),
                    l.getColindanteNorte(), l.getColindanteSur(),
                    l.getColindanteEste(), l.getColindanteOeste(),
                    l.getPrograma().getNombrePrograma()
                );
            }).collect(Collectors.toList());

        // 5. Info vendedor
        Integer idVendedor = contrato.getVendedor() != null ? contrato.getVendedor().getIdVendedor() : null;
        String nombreVendedor = contrato.getVendedor() != null
            ? contrato.getVendedor().getNombre() + " " + contrato.getVendedor().getApellidos()
            : "";

        String resumen = String.format(
            "Contrato #%d transferido. Pagado: S/ %.2f → sugerido como inicial. " +
            "Saldo: S/ %.2f a dividir en %d letras restantes.",
            idContrato, montoPagado, saldoPendiente, letrasRestantes
        );

        return new TransferenciaResponseDTO(
            idContrato,
            lotesDto,
            idLotes,
            idVendedor,
            nombreVendedor,
            contrato.getMontoTotal(),
            montoPagado,
            saldoPendiente,
            (int) letrasRestantes,
            contrato.getCantidadLetras(),
            resumen
        );
    }
}