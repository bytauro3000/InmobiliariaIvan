package com.Inmobiliaria.demo.service.impl;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.concurrent.CompletableFuture;

import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.Inmobiliaria.demo.client.InscripcionClient;
import com.Inmobiliaria.demo.dto.*;
import com.Inmobiliaria.demo.dto.TransferenciaResponseDTO;
import com.Inmobiliaria.demo.entity.*;
import com.Inmobiliaria.demo.enums.EstadoLetra;
import com.Inmobiliaria.demo.enums.EstadoLote;
import com.Inmobiliaria.demo.enums.EstadoContrato;
import com.Inmobiliaria.demo.enums.EstadoSeparacion;
import com.Inmobiliaria.demo.enums.TipoContrato;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.enums.TipoPropietario;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import com.Inmobiliaria.demo.repository.LetraCambioRepository;
import com.Inmobiliaria.demo.repository.PagoLetraRepository;
import com.Inmobiliaria.demo.service.*;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.util.PdfGenerator;

import lombok.RequiredArgsConstructor;

@Service
@CacheConfig(cacheNames = "contratos")
@RequiredArgsConstructor
public class ContratoServiceImpl implements ContratoService {

    private final ContratoRepository contratoRepository;
    private final ContratoClienteService contratoClienteService;
    private final ContratoLoteService contratoLoteService;
    private final ClienteService clienteService;
    private final LoteService loteService;
    private final UsuarioService usuarioService;
    private final SeparacionService separacionService;
    private final VendedorService vendedorService;
    private final LetraCambioRepository letraCambioRepository;
    private final PagoLetraRepository pagoLetraRepository;  // ✅ NUEVO
    private final InscripcionClient inscripcionClient;
    private final ModelMapper modelMapper;

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

        if (idsLotesAValidar != null) {
            for (Integer idLote : idsLotesAValidar) {
                Lote loteDB = loteService.obtenerLotePorId(idLote);
                if (loteDB != null) {
                    java.util.List<EstadoContrato> estadosTerminales = java.util.Arrays.asList(
                        EstadoContrato.TRANSFERIDO, EstadoContrato.RENUNCIA,
                        EstadoContrato.RESUELTO, EstadoContrato.CANCELADO
                    );
                    boolean duplicado = contratoRepository.existeContratoDuplicado(
                        loteDB.getPrograma().getIdPrograma(), loteDB.getManzana(),
                        loteDB.getNumeroLote(), estadosTerminales
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

        Contrato contrato = modelMapper.map(requestDTO, Contrato.class);
        if (contrato.getMoneda() == null) contrato.setMoneda(Moneda.USD);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            contrato.setFechaContrato(dateFormat.parse(requestDTO.getFechaContrato()));
        } catch (ParseException e) {
            throw new RuntimeException("Error al parsear la fecha del contrato.", e);
        }

        Vendedor vendedor = null;
        if (requestDTO.getIdSeparacion() != null) {
            Separacion separacion = separacionService.buscarPorId(requestDTO.getIdSeparacion());
            if (separacion != null) { contrato.setSeparacion(separacion); vendedor = separacion.getVendedor(); }
        } else if (requestDTO.getIdVendedor() != null) {
            vendedor = vendedorService.obtenerVendedorPorId(requestDTO.getIdVendedor())
                    .orElseThrow(() -> new NegocioException("Vendedor no encontrado"));
        }
        contrato.setVendedor(vendedor);
        contrato.setUsuario(usuarioService.buscarByUsuario(principal.getName()));
        setearValoresPorDefecto(contrato);
        Contrato contratoGuardado = contratoRepository.save(contrato);

        List<Integer> idsClientesAAsociar;
        if (requestDTO.getIdSeparacion() != null) {
            Separacion separacion = contratoGuardado.getSeparacion();
            separacion.setEstado(EstadoSeparacion.CONCRETADO);
            separacionService.actualizarSeparacion(separacion);
            idsClientesAAsociar = separacion.getClientes().stream()
                    .map(sc -> sc.getCliente().getIdCliente()).collect(Collectors.toList());
            separacion.getLotes().stream().map(sl -> sl.getLote().getIdLote())
                    .forEach(idLote -> registrarLoteEnContrato(contratoGuardado, idLote));
        } else {
            idsClientesAAsociar = requestDTO.getIdClientes();
            if (requestDTO.getIdLotes() != null)
                requestDTO.getIdLotes().forEach(idLote -> registrarLoteEnContrato(contratoGuardado, idLote));
        }

        if (idsClientesAAsociar != null) {
            for (Integer idCliente : idsClientesAAsociar) {
                Cliente cliente = clienteService.buscarClientePorId(idCliente);
                if (cliente != null) {
                    ContratoCliente cc = new ContratoCliente();
                    cc.setId(new ContratoClienteId(contratoGuardado.getIdContrato(), idCliente));
                    cc.setContrato(contratoGuardado); cc.setCliente(cliente);
                    cc.setTipoPropietario(TipoPropietario.TITULAR);
                    contratoClienteService.guardar(cc);
                }
            }
        }
        return mapToContratoResponseDTO(contratoGuardado);
    }

    @Override
    @Transactional
    @CacheEvict(allEntries = true)
    public ContratoResponseDTO actualizarContrato(Integer id, ContratoRequestDTO requestDTO) {
        Contrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new NegocioException("Contrato no encontrado con ID: " + id));

        if (requestDTO.getIdLotes() != null) {
            for (Integer idLote : requestDTO.getIdLotes()) {
                Lote loteDB = loteService.obtenerLotePorId(idLote);
                if (loteDB != null) {
                    java.util.List<EstadoContrato> estadosTerminales = java.util.Arrays.asList(
                        EstadoContrato.TRANSFERIDO, EstadoContrato.RENUNCIA,
                        EstadoContrato.RESUELTO, EstadoContrato.CANCELADO
                    );
                    boolean duplicado = contratoRepository.existeContratoDuplicadoParaOtroContrato(
                        loteDB.getPrograma().getIdPrograma(), loteDB.getManzana(),
                        loteDB.getNumeroLote(), id, estadosTerminales
                    );
                    if (duplicado) {
                        throw new NegocioException("El lote " + loteDB.getNumeroLote() +
                            " de la Manzana " + loteDB.getManzana() +
                            " ya está registrado en OTRO contrato activo.");
                    }
                }
            }
        }

        for (ContratoLote cl : contrato.getLotes()) {
            Lote loteAnterior = cl.getLote();
            loteAnterior.setEstado(EstadoLote.Disponible);
            loteService.actualizarLote(loteAnterior);
        }
        contrato.getLotes().clear();

        // ─── DETECCIÓN INTELIGENTE: ¿los cambios afectan los importes de las letras? ──
        // Los campos que determinan el importe por letra son:
        //   saldo (= montoTotal - inicial), cantidadLetras y moneda.
        // Si cualquiera cambia → las letras existentes quedan inconsistentes
        // → se eliminan junto con sus pagos para no dejar registros huérfanos.
        // Si NINGUNO cambia (solo se edita fecha, observaciones, vendedor, lotes, clientes)
        // → las letras y pagos se conservan intactos.
        Moneda monedaNueva    = requestDTO.getMoneda() != null ? requestDTO.getMoneda() : Moneda.USD;
        Moneda monedaActual   = contrato.getMoneda()   != null ? contrato.getMoneda()   : Moneda.USD;

        BigDecimal nuevoMonto    = BigDecimal.valueOf(requestDTO.getMontoTotal());
        BigDecimal nuevoInicial  = BigDecimal.valueOf(requestDTO.getInicial());
        BigDecimal nuevoSaldo    = BigDecimal.valueOf(requestDTO.getSaldo());
        Integer    nuevaCantidad = requestDTO.getCantidadLetras();

        boolean afectaLetras =
            nuevoMonto.compareTo(contrato.getMontoTotal())                    != 0 ||
            nuevoInicial.compareTo(contrato.getInicial())                     != 0 ||
            nuevoSaldo.compareTo(contrato.getSaldo())                         != 0 ||
            !Objects.equals(nuevaCantidad, contrato.getCantidadLetras())          ||
            !monedaNueva.equals(monedaActual);

        boolean tieneLetras = contrato.getLetrasCambio() != null
                           && !contrato.getLetrasCambio().isEmpty();

        if (afectaLetras && tieneLetras) {
            // Cascade ALL en LetraCambio → PagoLetras → Vouchers
            // elimina toda la cadena automáticamente al hacer clear().
            // El frontend ya mostró la advertencia y el usuario confirmó la acción.
            contrato.getLetrasCambio().clear();
        }
        // Si !afectaLetras → letras y pagos quedan intactos.

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            contrato.setFechaContrato(dateFormat.parse(requestDTO.getFechaContrato()));
        } catch (ParseException e) {
            throw new NegocioException("Formato de fecha invalido. Use el formato yyyy-MM-dd");
        }
        contrato.setMontoTotal(nuevoMonto);
        contrato.setInicial(nuevoInicial);
        contrato.setSaldo(nuevoSaldo);
        contrato.setCantidadLetras(nuevaCantidad);
        contrato.setObservaciones(requestDTO.getObservaciones());
        contrato.setMoneda(monedaNueva);
        contrato.getClientes().clear();

        Contrato contratoActualizado = contratoRepository.saveAndFlush(contrato);

        if (requestDTO.getIdLotes() != null)
            requestDTO.getIdLotes().forEach(idLote -> registrarLoteEnContrato(contratoActualizado, idLote));

        if (requestDTO.getIdClientes() != null) {
            for (Integer idCliente : requestDTO.getIdClientes()) {
                Cliente cliente = clienteService.buscarClientePorId(idCliente);
                ContratoCliente cc = new ContratoCliente();
                cc.setId(new ContratoClienteId(contratoActualizado.getIdContrato(), idCliente));
                cc.setContrato(contratoActualizado); cc.setCliente(cliente);
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
            cl.setContrato(contrato); cl.setLote(lote);
            contratoLoteService.guardar(cl);
        }
    }

    // ✅ NUEVO: consulta previa para que el frontend muestre la advertencia
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> consultarImpactoEdicion(Integer idContrato) {
        Contrato contrato = contratoRepository.findById(idContrato)
            .orElseThrow(() -> new NegocioException("Contrato no encontrado con ID: " + idContrato));

        boolean tieneLetras = contrato.getLetrasCambio() != null
                           && !contrato.getLetrasCambio().isEmpty();

        List<?> pagos = pagoLetraRepository.findByLetraContratoIdContrato(idContrato);
        boolean tienePagos = !pagos.isEmpty();

        return Map.of(
            "tieneLetras",     tieneLetras,
            "tienePagos",      tienePagos,
            "cantidadLetras",  tieneLetras ? contrato.getLetrasCambio().size() : 0,
            "cantidadPagos",   pagos.size()
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable
    public List<ContratoResponseDTO> listarContratos() {

        // Dos queries separadas — solución al MultipleBagFetchException
        List<Contrato> contratosConClientes = contratoRepository.findAllConClientes();
        List<Contrato> contratosConLotes    = contratoRepository.findAllConLotes();

        Map<Integer, List<ContratoLote>> lotesMap = contratosConLotes.stream()
            .collect(Collectors.toMap(Contrato::getIdContrato, Contrato::getLotes));

        contratosConClientes.forEach(c ->
            c.setLotes(lotesMap.getOrDefault(c.getIdContrato(), List.of()))
        );

        // Capturar RequestAttributes antes de los futuros para propagar JWT al hilo nuevo
        final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        List<Integer> idsConLuz  = List.of();
        List<Integer> idsConAgua = List.of();

        try {
            CompletableFuture<List<Integer>> futLuz = CompletableFuture.supplyAsync(() -> {
                RequestContextHolder.setRequestAttributes(requestAttributes);
                try { return inscripcionClient.obtenerContratosPorServicio("LUZ"); }
                finally { RequestContextHolder.resetRequestAttributes(); }
            });

            CompletableFuture<List<Integer>> futAgua = CompletableFuture.supplyAsync(() -> {
                RequestContextHolder.setRequestAttributes(requestAttributes);
                try { return inscripcionClient.obtenerContratosPorServicio("AGUA"); }
                finally { RequestContextHolder.resetRequestAttributes(); }
            });

            idsConLuz  = futLuz.get();
            idsConAgua = futAgua.get();
        } catch (Exception e) {
            System.err.println("Error al consultar Microservicio de Inscripciones: " + e.getMessage());
        }

        final List<Integer> finalIdsConLuz  = idsConLuz;
        final List<Integer> finalIdsConAgua = idsConAgua;

        return contratosConClientes.stream()
                .map(contrato -> {
                    ContratoResponseDTO dto = this.mapToContratoResponseDTO(contrato);
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
            boolean luz  = inscripcionClient.obtenerContratosPorServicio("LUZ").contains(idContrato);
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
                    ", Manzana " + manzana + ", Lote " + numeroLote));
        return mapToContratoResponseDTO(contrato);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContratoResponseDTO> buscarPorNombreCliente(String termino) {
        List<Contrato> contratos = contratoRepository.findByClienteNombreContaining(termino);

        final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        List<Integer> idsConLuz  = List.of();
        List<Integer> idsConAgua = List.of();

        try {
            CompletableFuture<List<Integer>> futLuz = CompletableFuture.supplyAsync(() -> {
                RequestContextHolder.setRequestAttributes(requestAttributes);
                try { return inscripcionClient.obtenerContratosPorServicio("LUZ"); }
                finally { RequestContextHolder.resetRequestAttributes(); }
            });

            CompletableFuture<List<Integer>> futAgua = CompletableFuture.supplyAsync(() -> {
                RequestContextHolder.setRequestAttributes(requestAttributes);
                try { return inscripcionClient.obtenerContratosPorServicio("AGUA"); }
                finally { RequestContextHolder.resetRequestAttributes(); }
            });

            idsConLuz  = futLuz.get();
            idsConAgua = futAgua.get();
        } catch (Exception e) {
            System.err.println("Error al consultar Microservicio: " + e.getMessage());
        }

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
    @CacheEvict(allEntries = true)
    public void eliminarContrato(Integer idContrato) {
        Contrato contrato = contratoRepository.findById(idContrato)
                .orElseThrow(() -> new NegocioException("No se encontro el contrato con ID: " + idContrato));

        if (contrato.getLotes() != null && !contrato.getLotes().isEmpty()) {
            for (ContratoLote contratoLote : contrato.getLotes()) {
                Lote lote = contratoLote.getLote();
                if (lote != null) { lote.setEstado(EstadoLote.Disponible); loteService.actualizarLote(lote); }
            }
        }
        if (contrato.getSeparacion() != null) {
            Separacion sep = contrato.getSeparacion();
            sep.setEstado(EstadoSeparacion.FINALIZADO);
            separacionService.actualizarSeparacion(sep);
        }
        contratoRepository.delete(contrato);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarPdf(Integer idContrato) {
        Contrato contrato = contratoRepository.findById(idContrato)
                .orElseThrow(() -> new NegocioException("No se encontro el contrato con ID: " + idContrato));
        ContratoResponseDTO dto = this.mapToContratoResponseDTO(contrato);
        LetraCambio primeraLetra = letraCambioRepository
                .findFirstByContratoIdContratoOrderByNumeroLetraAsc(idContrato).orElse(null);
        return PdfGenerator.generarContratoFlorida(dto, primeraLetra);
    }

    private ContratoResponseDTO mapToContratoResponseDTO(Contrato contrato) {
        return mapToContratoResponseDTO(contrato, false, false);
    }

    private ContratoResponseDTO mapToContratoResponseDTO(Contrato contrato, boolean luz, boolean agua) {
        if (contrato == null) return null;

        ContratoResponseDTO dto = modelMapper.map(contrato, ContratoResponseDTO.class);
        dto.setTieneLuz(luz);
        dto.setTieneAgua(agua);

        if (contrato.getVendedor() != null) {
            Vendedor v = contrato.getVendedor();
            dto.setVendedor(new VendedorResponseDTO(
                v.getIdVendedor(), v.getNombre(), v.getApellidos(), v.getDni(), v.getCelular()));
        }

        if (contrato.getClientes() != null) {
            dto.setClientes(contrato.getClientes().stream().map(cc -> {
                Cliente c = cc.getCliente();
                return new ClienteResponseDTO(c.getIdCliente(), c.getNombre(), c.getApellidos(),
                    c.getEstadoCivil(), c.getNumDoc(), c.getDireccion(), c.getCelular(),
                    c.getDistrito(), c.getGenero(), c.getTipoCliente(), c.getNacionalidad());
            }).collect(Collectors.toList()));
        }

        if (contrato.getLotes() != null) {
            dto.setLotes(contrato.getLotes().stream().map(cl -> {
                Lote l = cl.getLote();
                return new LoteResponseDTO(l.getIdLote(), l.getManzana(), l.getNumeroLote(),
                    l.getArea(), l.getLargo1(), l.getLargo2(), l.getAncho1(), l.getAncho2(),
                    l.getColindanteNorte(), l.getColindanteSur(), l.getColindanteEste(),
                    l.getColindanteOeste(), l.getPrograma().getNombrePrograma());
            }).collect(Collectors.toList()));
        }

        if (contrato.getLetrasCambio() != null) {
            dto.setLetras(contrato.getLetrasCambio().stream()
                .sorted(Comparator.comparing(l -> Integer.parseInt(l.getNumeroLetra().split("/")[0])))
                .map(letra -> new LetraResponseDTO(letra.getNumeroLetra(), letra.getFechaVencimiento(),
                    letra.getImporte(), letra.getImporteLetras()))
                .collect(Collectors.toList()));
        }
        return dto;
    }

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

        validarTransicion(estadoActual, nuevoEstado);
        contrato.setEstadoContrato(nuevoEstado);

        if (nuevoEstado == EstadoContrato.RESUELTO)
            contrato.getLotes().forEach(cl -> cl.getLote().setEstado(EstadoLote.Disponible));

        return mapToContratoResponseDTO(contratoRepository.save(contrato));
    }

    private void validarTransicion(EstadoContrato actual, EstadoContrato nuevo) {
        boolean valida = switch (actual) {
            case MORA           -> nuevo == EstadoContrato.CARTA_NOTARIAL;
            case CARTA_NOTARIAL -> nuevo == EstadoContrato.EN_RESOLUCION || nuevo == EstadoContrato.ACTIVO;
            case EN_RESOLUCION  -> nuevo == EstadoContrato.RESUELTO;
            case ACTIVO, RESUELTO, CANCELADO, RENUNCIA, TRANSFERIDO -> false;
        };
        if (!valida) throw new NegocioException("Transición no permitida: " + actual + " → " + nuevo);
    }

    @Override
    @Transactional
    @CacheEvict(allEntries = true)
    public ContratoResponseDTO registrarRenuncia(Integer idContrato) {
        Contrato contrato = contratoRepository.findById(idContrato)
            .orElseThrow(() -> new NegocioException("Contrato no encontrado: " + idContrato));

        EstadoContrato estado = contrato.getEstadoContrato();
        if (estado != EstadoContrato.ACTIVO && estado != EstadoContrato.MORA)
            throw new NegocioException("Solo se puede registrar renuncia desde estado ACTIVO o MORA. Estado actual: " + estado);

        contrato.setEstadoContrato(EstadoContrato.RENUNCIA);
        contrato.getLotes().forEach(cl -> cl.getLote().setEstado(EstadoLote.Disponible));
        contrato.getLetrasCambio().stream()
            .filter(l -> l.getEstadoLetra() == EstadoLetra.PENDIENTE || l.getEstadoLetra() == EstadoLetra.VENCIDO)
            .forEach(l -> l.setEstadoLetra(EstadoLetra.PAGADO));

        return mapToContratoResponseDTO(contratoRepository.save(contrato));
    }

    @Override
    @Transactional
    @CacheEvict(allEntries = true)
    public TransferenciaResponseDTO registrarTransferencia(Integer idContrato) {
        Contrato contrato = contratoRepository.findById(idContrato)
            .orElseThrow(() -> new NegocioException("Contrato no encontrado: " + idContrato));

        EstadoContrato estado = contrato.getEstadoContrato();
        if (estado != EstadoContrato.ACTIVO && estado != EstadoContrato.MORA)
            throw new NegocioException("Solo se puede transferir desde estado ACTIVO o MORA. Estado actual: " + estado);

        java.math.BigDecimal montoPagado = contrato.getLetrasCambio().stream()
            .filter(l -> l.getEstadoLetra() == EstadoLetra.PAGADO)
            .map(LetraCambio::getImporte)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        long letrasRestantes = contrato.getLetrasCambio().stream()
            .filter(l -> l.getEstadoLetra() == EstadoLetra.PENDIENTE || l.getEstadoLetra() == EstadoLetra.VENCIDO)
            .count();

        java.math.BigDecimal saldoPendiente = contrato.getMontoTotal().subtract(montoPagado);
        contrato.setEstadoContrato(EstadoContrato.TRANSFERIDO);
        contratoRepository.save(contrato);

        List<Integer> idLotes = contrato.getLotes().stream()
            .map(cl -> cl.getLote().getIdLote()).collect(Collectors.toList());

        List<LoteResponseDTO> lotesDto = contrato.getLotes().stream().map(cl -> {
            Lote l = cl.getLote();
            return new LoteResponseDTO(l.getIdLote(), l.getManzana(), l.getNumeroLote(), l.getArea(),
                l.getLargo1(), l.getLargo2(), l.getAncho1(), l.getAncho2(),
                l.getColindanteNorte(), l.getColindanteSur(), l.getColindanteEste(),
                l.getColindanteOeste(), l.getPrograma().getNombrePrograma());
        }).collect(Collectors.toList());

        Integer idVendedor = contrato.getVendedor() != null ? contrato.getVendedor().getIdVendedor() : null;
        String nombreVendedor = contrato.getVendedor() != null
            ? contrato.getVendedor().getNombre() + " " + contrato.getVendedor().getApellidos() : "";

        String resumen = String.format(
            "Contrato #%d transferido. Pagado: S/ %.2f → sugerido como inicial. Saldo: S/ %.2f a dividir en %d letras restantes.",
            idContrato, montoPagado, saldoPendiente, letrasRestantes);

        return new TransferenciaResponseDTO(idContrato, lotesDto, idLotes, idVendedor, nombreVendedor,
            contrato.getMontoTotal(), montoPagado, saldoPendiente, (int) letrasRestantes,
            contrato.getCantidadLetras(), resumen);
    }
}