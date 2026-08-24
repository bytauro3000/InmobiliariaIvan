package com.Inmobiliaria.demo.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.Inmobiliaria.demo.dto.*;
import com.Inmobiliaria.demo.dto.TransferenciaResponseDTO;
import com.Inmobiliaria.demo.entity.*;
import com.Inmobiliaria.demo.enums.EstadoLetra;
import com.Inmobiliaria.demo.enums.EstadoLote;
import com.Inmobiliaria.demo.enums.EstadoContrato;
import com.Inmobiliaria.demo.enums.EstadoSeparacion;
import com.Inmobiliaria.demo.enums.TipoContrato;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.enums.TipoOrigenComprobante;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.enums.TipoPropietario;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import com.Inmobiliaria.demo.repository.LetraCambioRepository;
import com.Inmobiliaria.demo.repository.PagoInicialRepository;
import com.Inmobiliaria.demo.repository.PagoInscripcionComprobanteRepository;
import com.Inmobiliaria.demo.repository.PagoLetraRepository;
import com.Inmobiliaria.demo.repository.MoraRepository;
import com.Inmobiliaria.demo.entity.Voucher;
import com.Inmobiliaria.demo.service.*;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.util.ContratoFloridaPdf;
import com.Inmobiliaria.demo.util.ContratoNapolePdfMerruic;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
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
    private final PagoLetraRepository pagoLetraRepository;
    private final PagoInicialRepository pagoInicialRepository;
    private final PagoInscripcionComprobanteRepository pagoInscripcionComprobanteRepository;
    private final ModelMapper modelMapper;
    private final ComprobanteService comprobanteService;

    private final SunatEnvioService            sunatEnvioService;
    private final Cloudinary                   cloudinary;
    private final com.Inmobiliaria.demo.repository.VoucherRepository voucherRepository;
    private final NotificacionAdminEmailService notificacionAdminEmailService;
    private final MoraRepository                 moraRepository;
    private final EmpresaService                 empresaService;

    private void setearValoresPorDefecto(Contrato contrato) {
        if (contrato.getTipoContrato() == TipoContrato.CONTADO) {
            contrato.setCantidadLetras(0);
            contrato.setInicial(BigDecimal.ZERO);
            contrato.setSaldo(BigDecimal.ZERO);
            contrato.setEstadoContrato(EstadoContrato.CANCELADO);
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

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(requestDTO.getFechaContrato(), dateFormatter);
        contrato.setFechaContrato(localDate);

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
        Contrato contratoGuardado;
        contrato.setPagoInicial(null); // Evitar que ModelMapper mapee PagoInicialRequestDTO → PagoInicial transient
        contratoGuardado = contratoRepository.save(contrato);

        // ── Registrar pago de la inicial ───────────────────────────────────────
        // Solo aplica a contratos FINANCIADOS con inicial > 0 y pagoInicial informado.
        if (contratoGuardado.getTipoContrato() == TipoContrato.FINANCIADO
                && requestDTO.getPagoInicial() != null
                && contratoGuardado.getInicial() != null
                && contratoGuardado.getInicial().compareTo(BigDecimal.ZERO) > 0) {

        	PagoInicialRequestDTO piReq = requestDTO.getPagoInicial();
            LocalDate fechaPagoInicial = piReq.getFechaPago() != null ? piReq.getFechaPago() : LocalDate.now();

        	PagoInicial pago = new PagoInicial();
        	pago.setContrato(contratoGuardado);
        	pago.setImportePagado(piReq.getImportePagado());
        	pago.setFechaPago(fechaPagoInicial);
        	pago.setMedioPago(piReq.getMedioPago());
        	pago.setNumeroOperacion(piReq.getNumeroOperacion());
        	pago.setObservaciones(piReq.getObservaciones());

            // ── PASO 1: guardar PagoInicial SIN comprobante todavía ──────────
            PagoInicial pagoGuardado = pagoInicialRepository.save(pago);

            // ── PASO 2: ahora que pagoGuardado tiene ID, generar comprobante ──
            if (piReq.getTipoComprobante() != null) {
                // ── Comprobante inicial (enviar a APIPERU antes de guardar si es BOLETA) ──
                Comprobante compInicial = comprobanteService.generarComprobanteConNumero(
                    piReq.getTipoComprobante(),
                    TipoOrigenComprobante.PAGO_INICIAL,
                    pagoGuardado.getIdPagoInicial(), // referencia: id del pago inicial
                    pagoGuardado.getImportePagado(),
                    fechaPagoInicial,
                    piReq.getNumeroComprobantePersonalizado()
                );

                // Si es BOLETA con serie B (B001), enviar a APIPERU ANTES de guardar en BD
                // Serie E (EB01) se registra localmente como RECIBO (igual que pago de letras)
                if (piReq.getTipoComprobante() == TipoComprobante.BOLETA
                        && compInicial.getSerie() != null
                        && compInicial.getSerie().startsWith("B")) {

                    // El cliente todavía NO está asociado al contrato (se asocia más abajo),
                    // así que se obtiene del request / de la separación.
                    Cliente cliente = null;
                    if (requestDTO.getIdSeparacion() != null) {
                        Separacion sep = separacionService.buscarPorId(requestDTO.getIdSeparacion());
                        if (sep != null && sep.getClientes() != null && !sep.getClientes().isEmpty()) {
                            cliente = sep.getClientes().iterator().next().getCliente();
                        }
                    } else if (requestDTO.getIdClientes() != null && !requestDTO.getIdClientes().isEmpty()) {
                        cliente = clienteService.buscarClientePorId(requestDTO.getIdClientes().get(0));
                    }
                    if (cliente == null) {
                        throw new NegocioException(
                                "No se pudo determinar el cliente para emitir la boleta de la inicial.");
                    }

                    String descripcion = "Pago inicial de contrato";
                    compInicial.setDescripcion(descripcion);
                    Map<String, Object> sunatRespuesta = sunatEnvioService.enviarBoleta(
                            cliente, contratoGuardado, compInicial,
                            pagoGuardado.getImportePagado(), descripcion);

                    // Si SUNAT aceptó, guardar hash y CDR en el comprobante
                    if (sunatRespuesta != null && "ACEPTADA".equals(sunatRespuesta.get("estadoSunat"))) {
                        String hash = (String) sunatRespuesta.get("hash");
                        String cdrZip = (String) sunatRespuesta.get("cdrZip");
                        if (hash != null && !hash.isBlank()) compInicial.setHashCdr(hash);
                        if (cdrZip != null && !cdrZip.isBlank()) compInicial.setCdrBase64(cdrZip);
                    }
                }

                pagoGuardado.setComprobante(compInicial);
                contratoGuardado.setComprobanteInicial(compInicial);
                pagoGuardado = pagoInicialRepository.save(pagoGuardado);
            }

            // ── PASO 3: enlazar el pago (con comprobante) al contrato ────────
            contratoGuardado.setPagoInicial(pagoGuardado);
            contratoGuardado = contratoRepository.save(contratoGuardado);

            PagoInicial finalPagoInicial = pagoGuardado;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificarAdminPagoInicial(finalPagoInicial);
                }
            });
        }
        // ── Fin registro pago inicial ──────────────────────────────────────────

        final Contrato contratoFinal = contratoGuardado;

        List<Integer> idsClientesAAsociar;
        if (requestDTO.getIdSeparacion() != null) {
            Separacion separacion = contratoFinal.getSeparacion();
            separacion.setEstado(EstadoSeparacion.CONCRETADO);
            separacionService.actualizarSeparacion(separacion);
            idsClientesAAsociar = separacion.getClientes().stream()
                    .map(sc -> sc.getCliente().getIdCliente()).collect(Collectors.toList());
            separacion.getLotes().stream().map(sl -> sl.getLote().getIdLote())
                    .forEach(idLote -> registrarLoteEnContrato(contratoFinal, idLote));
        } else {
            idsClientesAAsociar = requestDTO.getIdClientes();
            if (requestDTO.getIdLotes() != null)
                requestDTO.getIdLotes().forEach(idLote -> registrarLoteEnContrato(contratoFinal, idLote));
        }

        if (idsClientesAAsociar != null) {
            int index = 0;
            for (Integer idCliente : idsClientesAAsociar) {
                Cliente cliente = clienteService.buscarClientePorId(idCliente);
                if (cliente != null) {
                    ContratoCliente cc = new ContratoCliente();
                    cc.setId(new ContratoClienteId(contratoFinal.getIdContrato(), idCliente));
                    cc.setContrato(contratoFinal);
                    cc.setCliente(cliente);
                    cc.setTipoPropietario(TipoPropietario.TITULAR);
                    cc.setOrden(index++);
                    contratoClienteService.guardar(cc);
                }
            }
        }
        return mapToContratoResponseDTO(contratoFinal);
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
            contrato.getLetrasCambio().clear();
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate localDate = LocalDate.parse(requestDTO.getFechaContrato(), dateFormatter);
        contrato.setFechaContrato(localDate);
        contrato.setMontoTotal(nuevoMonto);
        contrato.setInicial(nuevoInicial);
        contrato.setSaldo(nuevoSaldo);
        contrato.setCantidadLetras(nuevaCantidad);
        contrato.setObservaciones(requestDTO.getObservaciones());
        contrato.setMoneda(monedaNueva);

        // ── Actualizar vendedor ────────────────────────────────────────────────
        if (requestDTO.getIdVendedor() != null) {
            Vendedor vendedorActualizado = vendedorService.obtenerVendedorPorId(requestDTO.getIdVendedor())
                    .orElseThrow(() -> new NegocioException("Vendedor no encontrado con ID: " + requestDTO.getIdVendedor()));
            contrato.setVendedor(vendedorActualizado);
        } else {
            contrato.setVendedor(null);
        }

        contrato.getClientes().clear();

        Contrato contratoActualizado = contratoRepository.saveAndFlush(contrato);

        if (requestDTO.getIdLotes() != null)
            requestDTO.getIdLotes().forEach(idLote -> registrarLoteEnContrato(contratoActualizado, idLote));

        if (requestDTO.getIdClientes() != null) {
            int index = 0;
            for (Integer idCliente : requestDTO.getIdClientes()) {
                Cliente cliente = clienteService.buscarClientePorId(idCliente);
                ContratoCliente cc = new ContratoCliente();
                cc.setId(new ContratoClienteId(contratoActualizado.getIdContrato(), idCliente));
                cc.setContrato(contratoActualizado); cc.setCliente(cliente);
                cc.setTipoPropietario(TipoPropietario.TITULAR);
                cc.setOrden(index++);
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
    public List<ContratoResponseDTO> listarContratos() {
        List<Contrato> contratos = contratoRepository.findAllConClientesYLotes();
        if (contratos.isEmpty()) return List.of();

        // Batch-fetch vouchers de todos los pagos iniciales en 1 consulta
        List<Integer> pagoInicialIds = contratos.stream()
                .map(Contrato::getPagoInicial)
                .filter(Objects::nonNull)
                .map(PagoInicial::getIdPagoInicial)
                .collect(Collectors.toList());

        Map<Integer, List<String>> vouchersPorPagoInicial;
        if (!pagoInicialIds.isEmpty()) {
            List<Voucher> vouchers = voucherRepository
                    .findByTipoOrigenAndReferenciaIdIn("PAGO_INICIAL", pagoInicialIds);
            vouchersPorPagoInicial = vouchers.stream()
                    .collect(Collectors.groupingBy(
                            Voucher::getReferenciaId,
                            Collectors.mapping(Voucher::getUrl, Collectors.toList())));
        } else {
            vouchersPorPagoInicial = Map.of();
        }

        return contratos.stream()
                .map(c -> mapToContratoResponseDTO(c, vouchersPorPagoInicial))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContratoListItemDTO> listarContratosResumen() {
        List<Contrato> contratos = contratoRepository.findAllResumen();
        if (contratos.isEmpty()) return List.of();

        return contratos.stream()
                .map(this::mapToContratoListItemDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContratoListItemDTO> listarContratosResumenPaginado(Pageable pageable) {
        Page<Contrato> contratoPage = contratoRepository.findAllResumenPaginado(pageable);
        List<ContratoListItemDTO> dtos = contratoPage.getContent().stream()
                .map(this::mapToContratoListItemDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, contratoPage.getTotalElements());
    }

    private ContratoListItemDTO mapToContratoListItemDTO(Contrato contrato) {
        ContratoListItemDTO dto = new ContratoListItemDTO();
        dto.setIdContrato(contrato.getIdContrato());
        dto.setFechaContrato(contrato.getFechaContrato());
        dto.setTipoContrato(contrato.getTipoContrato());
        dto.setEstadoContrato(contrato.getEstadoContrato());
        dto.setMontoTotal(contrato.getMontoTotal());
        dto.setInicial(contrato.getInicial());
        dto.setSaldo(contrato.getSaldo());
        dto.setCantidadLetras(contrato.getCantidadLetras());
        dto.setMoneda(contrato.getMoneda());

        dto.setClientes(contrato.getClientes().stream()
                .map(cc -> {
                    Cliente c = cc.getCliente();
                    return new ContratoListItemDTO.ClienteSimpleDTO(
                            c.getNombre(), c.getApellidos(), c.getNumDoc());
                })
                .collect(Collectors.toList()));

        dto.setLotes(contrato.getLotes().stream()
                .map(cl -> {
                    Lote l = cl.getLote();
                    return new ContratoListItemDTO.LoteSimpleDTO(
                            l.getManzana(), l.getNumeroLote(),
                            l.getPrograma() != null ? l.getPrograma().getNombrePrograma() : null);
                })
                .collect(Collectors.toList()));

        dto.setTieneLetras(contrato.getLetrasCambio() != null && !contrato.getLetrasCambio().isEmpty());

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(key = "#idContrato")
    public ContratoResponseDTO buscarPorId(Integer idContrato) {
        Contrato contrato = contratoRepository.findByIdConTodo(idContrato);
        if (contrato == null) return null;
        return mapToContratoResponseDTO(contrato);
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
        return contratoRepository.findByClienteNombreContaining(termino)
                .stream()
                .map(this::mapToContratoResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContratoListItemDTO> buscarPorNombreClienteResumen(String termino) {
        return contratoRepository.findByClienteNombreContaining(termino)
                .stream()
                .map(this::mapToContratoListItemDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(allEntries = true)
    public void eliminarContrato(Integer idContrato) {
        Contrato contrato = contratoRepository.findByIdConTodo(idContrato);
        if (contrato == null) throw new NegocioException("No se encontro el contrato con ID: " + idContrato);

        // ── 1. Reunir pagos, moras y comprobantes del contrato ──────────────────
        PagoInicial pagoInicial = contrato.getPagoInicial();

        List<LetraCambio> letras = contrato.getLetrasCambio() != null
                ? contrato.getLetrasCambio() : List.of();

        List<PagoLetras> pagosLetras = new ArrayList<>();
        for (LetraCambio letra : letras) {
            if (letra.getPagos() != null) pagosLetras.addAll(letra.getPagos());
        }

        List<MoraLetra> moras = new ArrayList<>();
        for (LetraCambio letra : letras) {
            moras.addAll(moraRepository.findByLetraIdLetra(letra.getIdLetra()));
        }

        List<PagoMora> pagosMora = new ArrayList<>();
        for (MoraLetra mora : moras) {
            if (mora.getPagos() != null) pagosMora.addAll(mora.getPagos());
        }

        List<Comprobante> comprobantes = new ArrayList<>();
        if (pagoInicial != null) agregarComprobante(comprobantes, pagoInicial.getComprobante());
        for (PagoLetras p : pagosLetras) agregarComprobante(comprobantes, p.getComprobante());
        for (PagoMora pm : pagosMora) agregarComprobante(comprobantes, pm.getComprobante());

        // ── 2. Bloquear si hay comprobantes electrónicos emitidos a SUNAT ───────
        List<Comprobante> comprobantesSunat = comprobantes.stream()
                .filter(this::esComprobanteSunat)
                .toList();
        if (!comprobantesSunat.isEmpty()) {
            String numeros = comprobantesSunat.stream()
                    .map(c -> c.getNumeroCompleto() != null ? c.getNumeroCompleto() : "?")
                    .distinct()
                    .collect(Collectors.joining(", "));
            throw new NegocioException(
                "No se puede eliminar el contrato: tiene comprobantes electrónicos emitidos a SUNAT ("
                + numeros + "). Anúlelos primero con nota de crédito.");
        }

        // ── 3. Liberar lotes y separación ───────────────────────────────────────
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

        List<PagoInscripcionComprobante> inscripciones =
                pagoInscripcionComprobanteRepository.findByContratoId(idContrato);

        // ── 4. Eliminar moras (desbloquea la FK mora_letra → pago_letra) ────────
        for (MoraLetra mora : moras) {
            mora.setPagoLetra(null);
            moraRepository.delete(mora);
        }

        // ── 5. Eliminar pago inicial e inscripciones ────────────────────────────
        if (pagoInicial != null) {
            contrato.setPagoInicial(null);
            pagoInicialRepository.delete(pagoInicial);
        }

        if (!inscripciones.isEmpty()) {
            pagoInscripcionComprobanteRepository.deleteAll(inscripciones);
        }

        // ── 6. Eliminar el contrato (cascada: letras → pagos) ───────────────────
        contratoRepository.delete(contrato);

        // ── 7. Eliminar comprobantes internos (recibos, no SUNAT) ───────────────
        for (Comprobante comp : comprobantes) {
            if (esComprobanteSunat(comp)) continue;
            try {
                comprobanteService.eliminarComprobante(comp.getIdComprobante());
            } catch (Exception e) {
                log.warn("No se pudo eliminar el comprobante {} (referenciado por otra entidad): {}",
                        comp.getNumeroCompleto(), e.getMessage());
            }
        }
    }

    private boolean esComprobanteSunat(Comprobante comp) {
        return comp != null && comp.getHashCdr() != null && !comp.getHashCdr().isBlank();
    }

    private void agregarComprobante(List<Comprobante> lista, Comprobante comp) {
        if (comp == null) return;
        for (Comprobante existente : lista) {
            if (existente.getIdComprobante().equals(comp.getIdComprobante())) return;
        }
        lista.add(comp);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarPdf(Integer idContrato) {
        Contrato contrato = contratoRepository.findByIdConTodo(idContrato);
        if (contrato == null) throw new NegocioException("No se encontro el contrato con ID: " + idContrato);
        ContratoResponseDTO dto = this.mapToContratoResponseDTO(contrato);
        LetraCambio primeraLetra = letraCambioRepository
                .findFirstByContratoIdContratoOrderByNumeroLetraAsc(idContrato).orElse(null);

        String nombrePrograma = (dto.getLotes() != null && !dto.getLotes().isEmpty())
                ? dto.getLotes().get(0).getNombrePrograma()
                : "";
        String nombreProgUp = (nombrePrograma != null) ? nombrePrograma.toUpperCase() : "";

        // Plantilla NAPOLE (MERRUIC). Uso normal: programas cuyo nombre contiene "NAPOLE".
        // EXCEPCIÓN TEMPORAL: el programa de pruebas "PROGRAMA PRUEBAS" (solo MERRUIC)
        // también usa la plantilla Napole para validar el contrato. Se debe quitar
        // cuando ya no se necesite la prueba.
        String rucActivo = empresaService.obtenerActiva().getRuc();
        boolean esMerruic = "20552273223".equals(rucActivo);
        boolean esPruebasTemporal = esMerruic && nombreProgUp.contains("PRUEBAS");

        if (nombreProgUp.contains("NAPOLE") || esPruebasTemporal) {
            return ContratoNapolePdfMerruic.generarContratoNapole(dto, primeraLetra);
        }
        // LA FLORIDA DE TORRE BLANCA (cualquiera de sus etapas) y demás programas
        return ContratoFloridaPdf.generarContratoFlorida(dto, primeraLetra);
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
                l.getPrecioM2(),
                l.getColindanteNorte(), l.getColindanteSur(), l.getColindanteEste(),
                l.getColindanteOeste(), l.getPrograma().getNombrePrograma(),
                l.getPrograma().getIdPrograma(), l.getEstado().name());
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

    // ── Upload voucher de la inicial ───────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(allEntries = true)
    public ContratoResponseDTO subirVoucherInicial(Integer idContrato, MultipartFile voucher) {
        Contrato contrato = contratoRepository.findById(idContrato)
                .orElseThrow(() -> new NegocioException("Contrato no encontrado: " + idContrato));

        PagoInicial pago = contrato.getPagoInicial();
        if (pago == null)
            throw new NegocioException("El contrato #" + idContrato + " no tiene un pago de inicial registrado.");

        try {
            String publicId = "inicial-" + idContrato + "-" + System.currentTimeMillis();
            Map<?, ?> params = ObjectUtils.asMap(
                "folder",    "vouchers/contrato-" + idContrato,
                "public_id", publicId
            );
            Map<?, ?> result = cloudinary.uploader().upload(voucher.getBytes(), params);
            String url = result.get("secure_url").toString();
            Voucher v = new Voucher();
            v.setTipoOrigen("PAGO_INICIAL");
            v.setReferenciaId(pago.getIdPagoInicial());
            v.setUrl(url);
            voucherRepository.save(v);
        } catch (Exception e) {
            throw new NegocioException("Error al subir voucher de la inicial: " + e.getMessage());
        }
        return mapToContratoResponseDTO(contratoRepository.save(contrato));
    }

    // ── Mapeo a DTO ────────────────────────────────────────────────────────────

    private ContratoResponseDTO mapToContratoResponseDTO(Contrato contrato) {
        return mapToContratoResponseDTO(contrato, Map.of());
    }

    private ContratoResponseDTO mapToContratoResponseDTO(
            Contrato contrato, Map<Integer, List<String>> vouchersPorPagoInicial) {
        if (contrato == null) return null;

        ContratoResponseDTO dto = modelMapper.map(contrato, ContratoResponseDTO.class);

        if (contrato.getVendedor() != null) {
            Vendedor v = contrato.getVendedor();
            dto.setVendedor(new VendedorResponseDTO(
                v.getIdVendedor(), v.getNombre(), v.getApellidos(), v.getDni(), v.getCelular()));
        }

        if (contrato.getClientes() != null) {
            dto.setClientes(contrato.getClientes().stream().map(cc -> {
                Cliente c = cc.getCliente();
                Distrito d = c.getDistrito();
                DistritoDTO distritoDTO = d != null ? new DistritoDTO(
                    d.getIdDistrito(), d.getNombre(), d.getCodigoUbigeo(), d.getProvincia(), d.getDepartamento()
                ) : null;
                return new ClienteResponseDTO(
                        c.getIdCliente(), c.getNombre(), c.getApellidos(),
                        c.getEstadoCivil(), c.getNumDoc(), c.getDireccion(), c.getCelular(),
                        c.getTelefono(), c.getEmail(),
                        distritoDTO, c.getGenero(),
                        c.getTipoCliente(),
                        c.getNacionalidad(),
                        c.getEstado(),
                        c.getFechaRegistro()
                    );
            }).collect(Collectors.toList()));
        }

        if (contrato.getLotes() != null) {
            dto.setLotes(contrato.getLotes().stream().map(cl -> {
                Lote l = cl.getLote();
                return new LoteResponseDTO(l.getIdLote(), l.getManzana(), l.getNumeroLote(),
                    l.getArea(), l.getLargo1(), l.getLargo2(), l.getAncho1(), l.getAncho2(),
                    l.getPrecioM2(),
                    l.getColindanteNorte(), l.getColindanteSur(), l.getColindanteEste(),
                    l.getColindanteOeste(), l.getPrograma().getNombrePrograma(),
                    l.getPrograma().getIdPrograma(), l.getEstado().name());
            }).collect(Collectors.toList()));
        }

        if (contrato.getLetrasCambio() != null) {
            dto.setLetras(contrato.getLetrasCambio().stream()
                .sorted(Comparator.comparing(l -> Integer.parseInt(l.getNumeroLetra().split("/")[0])))
                .map(letra -> new LetraResponseDTO(letra.getNumeroLetra(), letra.getFechaVencimiento(),
                    letra.getImporte(), letra.getImporteLetras()))
                .collect(Collectors.toList()));
        }

        // ── Comprobante de la inicial (compatibilidad) ─────────────────────────
        if (contrato.getComprobanteInicial() != null) {
            Comprobante ci = contrato.getComprobanteInicial();
            dto.setIdComprobanteInicial(ci.getIdComprobante());
            dto.setTipoComprobanteInicial(ci.getTipoComprobante());
            dto.setNumeroComprobanteInicial(ci.getNumeroCompleto());
        }

        // ── Datos completos del pago de la inicial ─────────────────────────────
        if (contrato.getPagoInicial() != null) {
            PagoInicial pi = contrato.getPagoInicial();
            PagoInicialResponseDTO piDto = new PagoInicialResponseDTO();
            piDto.setIdPagoInicial(pi.getIdPagoInicial());
            piDto.setImportePagado(pi.getImportePagado());
            piDto.setFechaPago(pi.getFechaPago());
            piDto.setMedioPago(pi.getMedioPago());
            piDto.setNumeroOperacion(pi.getNumeroOperacion());
            piDto.setObservaciones(pi.getObservaciones());
            List<String> urlsVoucher = vouchersPorPagoInicial
                    .getOrDefault(pi.getIdPagoInicial(), List.of());
            piDto.setUrlsVoucher(urlsVoucher);
            if (pi.getComprobante() != null) {
                piDto.setIdComprobante(pi.getComprobante().getIdComprobante());
                piDto.setTipoComprobante(pi.getComprobante().getTipoComprobante());
                piDto.setNumeroComprobante(pi.getComprobante().getNumeroCompleto());
            }
            dto.setPagoInicial(piDto);
        }

        return dto;
    }

    private void notificarAdminPagoInicial(PagoInicial pago) {
        if (!pago.getFechaPago().equals(LocalDate.now())) return;
        try {
            var contrato = pago.getContrato();
            Moneda moneda = contrato.getMoneda() != null ? contrato.getMoneda() : Moneda.USD;

            String clienteNombre = "-";
            if (contrato.getClientes() != null && !contrato.getClientes().isEmpty()) {
                var c = contrato.getClientes().iterator().next().getCliente();
                clienteNombre = c.getNombre() + " " + c.getApellidos();
            }

            String detalle = "Pago inicial / cuota del contrato";

            String loteInfo = "";
            if (contrato.getLotes() != null && !contrato.getLotes().isEmpty()) {
                var lote = contrato.getLotes().iterator().next().getLote();
                if (lote != null) {
                    loteInfo = " Mz. " + lote.getManzana() + " Lt. " + lote.getNumeroLote();
                    if (lote.getPrograma() != null) {
                        loteInfo += " del Programa: " + lote.getPrograma().getNombrePrograma();
                    }
                }
            }
            detalle += loteInfo;

            String medioPago = pago.getMedioPago() != null ? pago.getMedioPago().name() : "-";
            notificacionAdminEmailService.notificarPagoInicial(detalle, clienteNombre, pago.getImportePagado(), moneda, medioPago);
        } catch (Exception e) {
            log.warn("No se pudo enviar notificacion admin para pago inicial ID {}: {}", pago.getIdPagoInicial(), e.getMessage());
        }
    }
}