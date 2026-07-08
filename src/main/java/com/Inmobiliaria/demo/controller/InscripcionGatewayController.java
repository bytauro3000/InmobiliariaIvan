package com.Inmobiliaria.demo.controller;
	
import com.Inmobiliaria.demo.client.InscripcionClient;
import com.Inmobiliaria.demo.dto.*;
import com.Inmobiliaria.demo.entity.Comprobante;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.Lote;
import com.Inmobiliaria.demo.entity.PagoInscripcionComprobante;
import com.Inmobiliaria.demo.entity.Voucher;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.enums.TipoOrigenComprobante;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.service.impl.NotificacionAdminEmailService;
import com.Inmobiliaria.demo.repository.ComprobanteRepository;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import com.Inmobiliaria.demo.repository.PagoInscripcionComprobanteRepository;
import com.Inmobiliaria.demo.repository.VoucherRepository;
import com.Inmobiliaria.demo.service.ComprobanteService;
import com.Inmobiliaria.demo.service.impl.InscripcionComprobanteServiceImpl;
import com.Inmobiliaria.demo.util.ComprobanteInscripcionPdf;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import feign.FeignException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
	
	@RestController
	@RequestMapping("/api/gateway/inscripciones")
	@RequiredArgsConstructor
	@Slf4j
	public class InscripcionGatewayController {
	
    private final CacheManager                         cacheManager;
    private final InscripcionClient                    inscripcionClient;
    private final ContratoRepository                   contratoRepository;
    private final PagoInscripcionComprobanteRepository pagoInscripcionComprobanteRepository;
    private final ComprobanteService                   comprobanteService;
    private final ComprobanteRepository                comprobanteRepository;
    private final InscripcionComprobanteServiceImpl    inscripcionComprobanteService;
    private final VoucherRepository                    voucherRepository;
    private final Cloudinary                           cloudinary;
    private final NotificacionAdminEmailService         notificacionAdminEmailService;
	
	    // ── 1. Listar resumen contratos con servicios ────────────────────────────
	
	    @GetMapping("/resumen")
	    @PreAuthorize("hasAnyAuthority('ROLE_SECRETARIA', 'ROLE_ADMINISTRADOR')")
	    @Transactional(readOnly = true)
	    public ResponseEntity<List<InscripcionResumenDTO>> listarResumen() {
	        try {
	            Map<Integer, List<String>> serviciosPorContrato =
	                    inscripcionClient.obtenerResumenServicios();
	
	            Map<Integer, List<Map<String, Object>>> pendientesPorContrato;
	            try {
	                pendientesPorContrato = inscripcionClient.obtenerResumenPendientes();
	            } catch (Exception ex) {
	                pendientesPorContrato = new HashMap<>();
	            }
	
	            List<Contrato> contratosConClientes =
	                    contratoRepository.findResumenInscripcionesConClientes();
	
	            Map<Integer, Contrato> contratosConLotes =
	                    contratoRepository.findResumenInscripcionesConLotes()
	                            .stream()
	                            .collect(Collectors.toMap(Contrato::getIdContrato, c -> c));
	
	            contratosConClientes.sort(Comparator.comparing(Contrato::getIdContrato).reversed());
	
	            final Map<Integer, List<Map<String, Object>>> pendientesRef = pendientesPorContrato;
	
	            List<InscripcionResumenDTO> resultado = contratosConClientes.stream().map(c -> {
	
	                String nombre = "—";
	                if (c.getClientes() != null && !c.getClientes().isEmpty()) {
	                    var cliente = c.getClientes().iterator().next().getCliente();
	                    nombre = cliente.getNombre() + " " + cliente.getApellidos();
	                }
	
                String manzana = "", numeroLote = "", nombrePrograma = "";
                Contrato cLotes = contratosConLotes.get(c.getIdContrato());
                if (cLotes != null && cLotes.getLotes() != null && !cLotes.getLotes().isEmpty()) {
                    Lote l = cLotes.getLotes().iterator().next().getLote();
                    manzana    = l.getManzana();
                    numeroLote = l.getNumeroLote();
                    if (l.getPrograma() != null) {
                        nombrePrograma = l.getPrograma().getNombrePrograma();
                    }
                }
	
	                List<String> servicios = serviciosPorContrato
	                        .getOrDefault(c.getIdContrato(), List.of());
	
	                List<Map<String, Object>> pendientes =
	                        pendientesRef.getOrDefault(c.getIdContrato(), List.of());
	
	                PendienteInscripcionDTO pendienteLuz  = null;
	                PendienteInscripcionDTO pendienteAgua = null;
	
	                for (Map<String, Object> p : pendientes) {
	                    String tipo = String.valueOf(p.getOrDefault("tipoServicio", ""));
	                    Integer idIns = p.get("idInscripcion") instanceof Number
	                            ? ((Number) p.get("idInscripcion")).intValue() : null;
	                    java.math.BigDecimal montoTotal = p.get("montoTotal") instanceof Number
	                            ? java.math.BigDecimal.valueOf(((Number) p.get("montoTotal")).doubleValue()) : java.math.BigDecimal.ZERO;
	                    java.math.BigDecimal montoAcumulado = p.get("montoAcumulado") instanceof Number
	                            ? java.math.BigDecimal.valueOf(((Number) p.get("montoAcumulado")).doubleValue()) : java.math.BigDecimal.ZERO;
	
	                    if ("LUZ".equalsIgnoreCase(tipo)) {
	                        pendienteLuz = new PendienteInscripcionDTO(idIns, montoTotal, montoAcumulado);
	                    } else if ("AGUA".equalsIgnoreCase(tipo)) {
	                        pendienteAgua = new PendienteInscripcionDTO(idIns, montoTotal, montoAcumulado);
	                    }
	                }
	
                InscripcionResumenDTO dto = new InscripcionResumenDTO();
                dto.setIdContrato(c.getIdContrato());
                dto.setNombreCliente(nombre);
                dto.setManzana(manzana);
                dto.setNumeroLote(numeroLote);
                dto.setNombrePrograma(nombrePrograma);
                dto.setTieneLuz(servicios.contains("LUZ"));
                dto.setTieneAgua(servicios.contains("AGUA"));
	                dto.setTienePendienteLuz(pendienteLuz != null);
	                dto.setTienePendienteAgua(pendienteAgua != null);
	                dto.setPendienteLuz(pendienteLuz);
	                dto.setPendienteAgua(pendienteAgua);
	                return dto;
	
	            }).collect(Collectors.toList());
	
	            return ResponseEntity.ok(resultado);
	
	        } catch (FeignException e) {
	            return ResponseEntity.status(e.status()).build();
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	        }
	    }
	
	    // ── 2. Registrar nueva inscripción ───────────────────────────────────────
	
	    @PostMapping("/registrar")
	    @PreAuthorize("hasAuthority('ROLE_SECRETARIA')")
	    public ResponseEntity<?> registrarInscripcion(@RequestBody InscripcionServicioDTO dto) {
	        try {
	            InscripcionServicioDTO payload = new InscripcionServicioDTO();
	            payload.setIdContrato(dto.getIdContrato());
	            payload.setTipoServicio(dto.getTipoServicio());
	
	            InscripcionServicioDTO resultado = inscripcionClient.crearInscripcion(payload);
	            limpiarCache();
	            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
	
	        } catch (FeignException e) {
	            return ResponseEntity.status(e.status()).body(e.contentUTF8());
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body("Error al comunicarse con el servicio de inscripciones: " + e.getMessage());
	        }
	    }
	
	    // ── 3. Registrar abono a una inscripción existente ───────────────────────

	    @PostMapping(value = "/{idInscripcion}/abonar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	    @PreAuthorize("hasAuthority('ROLE_SECRETARIA')")
	    @Transactional
	    public ResponseEntity<?> registrarAbono(
	            @PathVariable Integer idInscripcion,
	            @RequestPart("pago") AbonoInscripcionRequestDTO request,
	            @RequestPart(value = "vouchers", required = false) List<MultipartFile> vouchers,
	            Authentication authentication) {
	
	        if (request.getIdContrato() == null)
	            return ResponseEntity.badRequest().body("El idContrato es obligatorio.");
	        if (request.getTipoServicio() == null || request.getTipoServicio().isBlank())
	            return ResponseEntity.badRequest().body("El tipo de servicio es obligatorio.");
	        if (request.getMontoPagado() == null || request.getMontoPagado().signum() <= 0)
	            return ResponseEntity.badRequest().body("El monto pagado debe ser mayor a cero.");
	        if (request.getMedioPago() == null)
	            return ResponseEntity.badRequest().body("El medio de pago es obligatorio.");
	        if (request.getTipoComprobante() == null)
	            return ResponseEntity.badRequest().body("El tipo de comprobante es obligatorio.");
	
	        // a) Enviar abono al microservicio
	        try {
	            Map<String, Object> abonoPayload = new HashMap<>();
	            abonoPayload.put("montoPagado", request.getMontoPagado());
	            abonoPayload.put("metodoPago",  request.getMedioPago().name());
	
	            inscripcionClient.registrarAbono(idInscripcion, abonoPayload);
	
	        } catch (FeignException e) {
	            return ResponseEntity.status(e.status()).body(e.contentUTF8());
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body("Error al registrar el abono en el servicio de inscripciones: " + e.getMessage());
	        }
	
	        // b) Buscar el contrato en el monolito
	        Contrato contrato = contratoRepository.findById(request.getIdContrato())
	                .orElseThrow(() -> new NegocioException(
	                        "Contrato no encontrado con ID: " + request.getIdContrato()));
	
	        // c) Generar comprobante en el monolito
	        LocalDate fechaPago = LocalDate.now();
	
	        Comprobante comprobante;
	        if (request.getNumeroComprobantePersonalizado() != null
	                && !request.getNumeroComprobantePersonalizado().isBlank()) {
	            comprobante = comprobanteService.generarComprobanteConNumero(
	                    request.getTipoComprobante(),
	                    TipoOrigenComprobante.PAGO_INSCRIPCION,
	                    request.getIdContrato(),
	                    request.getMontoPagado(),
	                    fechaPago,
	                    request.getNumeroComprobantePersonalizado());
	        } else {
	            comprobante = comprobanteService.generarComprobante(
	                    request.getTipoComprobante(),
	                    TipoOrigenComprobante.PAGO_INSCRIPCION,
	                    request.getIdContrato(),
	                    request.getMontoPagado(),
	                    fechaPago);
	        }
	
	        // d) Guardar en la tabla exclusiva de comprobantes de inscripción
	        PagoInscripcionComprobante pago = new PagoInscripcionComprobante();
	        pago.setContrato(contrato);
	        pago.setImportePagado(request.getMontoPagado());
	        pago.setFechaPago(fechaPago);
	        pago.setMedioPago(request.getMedioPago());
	        pago.setNumeroOperacion(request.getNumeroOperacion());
	        pago.setObservaciones(request.getObservaciones() != null
	                ? request.getObservaciones()
	                : "Abono inscripción servicio de " + request.getTipoServicio().toUpperCase());
	        pago.setComprobante(comprobante);
	        pago.setIdInscripcionServicio(idInscripcion);
	        pago.setTipoServicio(request.getTipoServicio().toUpperCase());
	
	        PagoInscripcionComprobante pagoGuardado =
	                pagoInscripcionComprobanteRepository.save(pago);

	        // ── Notificar al admin solo si el pago es de hoy ───────────────────────
	        if (fechaPago.equals(LocalDate.now())) {
	            try {
	                Moneda moneda = contrato.getMoneda() != null ? contrato.getMoneda() : Moneda.USD;
	                String clienteNombre = "-";
	                if (contrato.getClientes() != null && !contrato.getClientes().isEmpty()) {
	                    var c = contrato.getClientes().iterator().next().getCliente();
	                    clienteNombre = c.getNombre() + " " + c.getApellidos();
	                }
	                String detalle = "Pago de servicio " + request.getTipoServicio().toUpperCase();
	                String medioPago = request.getMedioPago() != null ? request.getMedioPago().name() : "-";
	                notificacionAdminEmailService.notificarPagoServicio(
	                    detalle, clienteNombre, request.getMontoPagado(), moneda, medioPago);
	            } catch (Exception e) {
	                log.warn("No se pudo enviar notificacion admin para pago de servicio: {}", e.getMessage());
	            }
	        }

            /* f) Enviar a SUNAT sincronamente — si rechaza, @Transactional revierte todo
            Cliente cliente = contrato.getClientes().iterator().next().getCliente();
            String descripcion = "Abono inscripcion servicio de " + request.getTipoServicio().toUpperCase();
            sunatEnvioService.enviarBoleta(cliente, contrato, comprobante,
                    request.getMontoPagado(), descripcion);*/

	        // e) Guardar vouchers (opcional, en paralelo al resto del flujo)
	        try {
	            guardarVouchers(vouchers, pagoGuardado, request.getIdContrato(), idInscripcion);
	        } catch (Exception e) {
	            // No interrumpimos el pago si falla la subida del voucher
	            System.err.println("Error al guardar vouchers del pago de inscripción: " + e.getMessage());
	        }

	        InscripcionConPagoResponseDTO response = new InscripcionConPagoResponseDTO(
	                pagoGuardado.getIdPagoInscripcionComprobante(),
	                comprobante.getNumeroCompleto(),
	                request.getTipoServicio(),
	                request.getIdContrato()
	        );
	        return ResponseEntity.status(HttpStatus.CREATED).body(response);
	    }
	
	    // ── 4. Consultar saldo pendiente de una inscripción ──────────────────────
	
	    @GetMapping("/{idInscripcion}/saldo")
	    @PreAuthorize("hasAnyAuthority('ROLE_SECRETARIA', 'ROLE_ADMINISTRADOR')")
	    public ResponseEntity<?> obtenerSaldo(@PathVariable Integer idInscripcion) {
	        try {
	            return ResponseEntity.ok(inscripcionClient.obtenerSaldo(idInscripcion));
	        } catch (FeignException e) {
	            return ResponseEntity.status(e.status()).body(e.contentUTF8());
	        } catch (Exception e) {
	            return ResponseEntity.internalServerError()
	                    .body("Error al consultar el saldo de la inscripción.");
	        }
	    }
	
	    // ── 5. Historial de abonos de una inscripción ────────────────────────────
	
	    @GetMapping("/{idInscripcion}/abonos")
	    @PreAuthorize("hasAnyAuthority('ROLE_SECRETARIA', 'ROLE_ADMINISTRADOR')")
	    public ResponseEntity<?> listarAbonos(@PathVariable Integer idInscripcion) {
	        try {
	            return ResponseEntity.ok(inscripcionClient.listarAbonos(idInscripcion));
	        } catch (FeignException e) {
	            return ResponseEntity.status(e.status()).body(e.contentUTF8());
	        } catch (Exception e) {
	            return ResponseEntity.internalServerError()
	                    .body("Error al obtener el historial de abonos.");
	        }
	    }
	
	    // ── 6. Descargar PDF del comprobante de un abono ─────────────────────────
	
	    @GetMapping("/pago/{idPago}/comprobante-pdf")
	    public ResponseEntity<byte[]> descargarComprobanteInscripcion(
	            @PathVariable Integer idPago,
	            Authentication authentication) {
	
	        try {
	            // Dos queries separadas para evitar MultipleBagFetchException
	            PagoInscripcionComprobante pago = inscripcionComprobanteService.obtenerPagoConDetalle(idPago);
	
	            String rolUsuario = "SECRETARIA";
	            if (authentication != null && authentication.getAuthorities() != null) {
	                rolUsuario = authentication.getAuthorities().stream()
	                        .map(GrantedAuthority::getAuthority)
	                        .map(r -> r.replace("ROLE_", ""))
	                        .findFirst()
	                        .orElse("SECRETARIA");
	            }
	
	            String tipoServicio = pago.getTipoServicio() != null
	                    ? pago.getTipoServicio() : "SERVICIO";
	
	            List<Voucher> vouchers = voucherRepository
	                    .findByTipoOrigenAndReferenciaId("PAGO_INSCRIPCION",
	                            pago.getIdPagoInscripcionComprobante());
	
	            byte[] pdf = ComprobanteInscripcionPdf.generar(pago, tipoServicio, rolUsuario, vouchers);
	
	            return ResponseEntity.ok()
	                    .header(HttpHeaders.CONTENT_DISPOSITION,
	                            "inline; filename=\"comprobante-inscripcion-" + idPago + ".pdf\"")
	                    .contentType(MediaType.APPLICATION_PDF)
	                    .body(pdf);
	
	        } catch (NegocioException e) {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
	        } catch (Exception e) {
	            return ResponseEntity.internalServerError().build();
	        }}
	
	    // ── 7. Secretaria + Admin: Listar pagos de inscripción ───────────────────
	
	    @GetMapping("/pagos")
	    @PreAuthorize("hasAnyAuthority('ROLE_SECRETARIA', 'ROLE_ADMINISTRADOR')")
	    @Transactional(readOnly = true)
	    public ResponseEntity<List<PagoInscripcionDTO>> listarPagos() {
	        try {
	            List<PagoInscripcionDTO> resultado = pagoInscripcionComprobanteRepository
	                    .findAllConDetalle()
	                    .stream()
	                    .map(p -> mapPagoToDTO(p, false))
	                    .collect(Collectors.toList());
	            return ResponseEntity.ok(resultado);
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	        }
	    }
	
	    // ── 8. Inscripciones pendientes por contrato ─────────────────────────────
	
	    @GetMapping("/pendientes/{idContrato}")
	    @PreAuthorize("hasAnyAuthority('ROLE_SECRETARIA', 'ROLE_ADMINISTRADOR')")
	    public ResponseEntity<?> listarPendientesPorContrato(@PathVariable Integer idContrato) {
	        try {
	            return ResponseEntity.ok(inscripcionClient.listarPendientesPorContrato(idContrato));
	        } catch (FeignException e) {
	            return ResponseEntity.status(e.status()).body(e.contentUTF8());
	        } catch (Exception e) {
	            return ResponseEntity.internalServerError()
	                    .body("Error al consultar inscripciones pendientes.");
	        }
	    }
	
	    // ── 9. Eliminar inscripción ───────────────────────────────────────────────
	
	    @DeleteMapping("/{idInscripcion}")
	    @PreAuthorize("hasAuthority('ROLE_SECRETARIA')")
	    public ResponseEntity<?> eliminarInscripcion(@PathVariable Integer idInscripcion) {
	        try {
	            inscripcionClient.eliminarInscripcion(idInscripcion);
	            limpiarCache();
	            return ResponseEntity.noContent().build();
	        } catch (FeignException e) {
	            if (e.status() == 404 || e.status() == 400) {
	                return ResponseEntity.status(e.status()).body(e.contentUTF8());
	            }
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body("Error al eliminar la inscripción.");
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body("Error al comunicarse con el servicio de inscripciones.");
	        }
	    }
	
	    // ── 10. Contratos activos por servicio ───────────────────────────────────
	
	    @GetMapping("/contratos-activos")
	    @PreAuthorize("hasAuthority('ROLE_SECRETARIA')")
	    public ResponseEntity<List<Integer>> obtenerContratosPorServicio(@RequestParam String tipo) {
	        try {
	            return ResponseEntity.ok(inscripcionClient.obtenerContratosPorServicio(tipo));
	        } catch (Exception e) {
	            return ResponseEntity.internalServerError().build();
	        }
	    }
	
	    // ── 11. Admin: Listado general con filtros ────────────────────────────────
	
	    @GetMapping("/pagos/todos")
	    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR')")
	    @Transactional(readOnly = true)
	    public ResponseEntity<List<PagoInscripcionDTO>> listarPagosTodos(
	            @RequestParam(required = false) String numeroComprobante,
	            @RequestParam(required = false) String manzana,
	            @RequestParam(required = false) String numeroLote,
	            @RequestParam(required = false) Integer idPrograma,
	            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
	            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
	        try {
	            List<PagoInscripcionDTO> resultado = pagoInscripcionComprobanteRepository.findTodos(
	                            (numeroComprobante != null && !numeroComprobante.isBlank()) ? numeroComprobante : null,
	                            (manzana           != null && !manzana.isBlank())           ? manzana           : null,
	                            (numeroLote        != null && !numeroLote.isBlank())        ? numeroLote        : null,
	                            idPrograma, desde, hasta)
	                    .stream()
	                    .map(p -> mapPagoToDTO(p, true))
	                    .collect(Collectors.toList());
	            return ResponseEntity.ok(resultado);
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	        }
	    }
	
	    // ── 12. Admin: Anular pago de inscripción ────────────────────────────────
	
	    @PatchMapping("/pago/{idPago}/anular")
	    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR')")
	    @Transactional
	    public ResponseEntity<?> anularPagoInscripcion(
	            @PathVariable Integer idPago,
	            @Valid @RequestBody AnulacionRequestDTO request,
	            Authentication authentication) {
	        try {
	            PagoInscripcionComprobante pago = pagoInscripcionComprobanteRepository
	                    .findById(idPago)
	                    .orElseThrow(() -> new NegocioException(
	                            "No se encontró el pago de inscripción con ID: " + idPago));
	
	            if (Boolean.TRUE.equals(pago.getAnulado()))
	                throw new NegocioException("El pago ya fue anulado.");
	
	            pago.setAnulado(true);
	            pago.setMotivoAnulacion(request.getMotivo());
	            pago.setFechaAnulacion(java.time.LocalDateTime.now());
	            pago.setAnuladoPor(authentication.getName());
	            pagoInscripcionComprobanteRepository.save(pago);

	            // Crear NC interna para comprobantes no SUNAT
	            Comprobante orig = pago.getComprobante();
	            if (orig != null && orig.getSerie() != null && !orig.getSerie().startsWith("B")) {
	                Comprobante nc = comprobanteService.generarNotaCredito(
	                        orig, "01", request.getMotivo(), authentication.getName());
	                orig.setIdNotaCreditoAnulacion(nc.getIdComprobante());
	                comprobanteRepository.save(orig);
	            }
	
	            return ResponseEntity.ok(Map.of(
	                    "mensaje", "Pago de inscripción anulado correctamente.",
	                    "idPago",  idPago));
	
	        } catch (NegocioException e) {
	            return ResponseEntity.badRequest().body(e.getMessage());
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body("Error al anular el pago de inscripción.");
	        }
	    }
	
	    // ── Helper: mapper privado reutilizable ───────────────────────────────────
	
	    /**
	     * Convierte PagoInscripcionComprobante → PagoInscripcionDTO.
	     * @param incluirDatosAdmin  true  → popula nombreCliente (solo admin)
	     *                           false → deja nombreCliente en null (secretaria)
	     */
	    private PagoInscripcionDTO mapPagoToDTO(PagoInscripcionComprobante p, boolean incluirDatosAdmin) {
	        PagoInscripcionDTO dto = new PagoInscripcionDTO();
	
	        dto.setIdPagoInscripcionComprobante(p.getIdPagoInscripcionComprobante());
	        dto.setIdContrato(p.getContrato().getIdContrato());
	        dto.setImportePagado(p.getImportePagado());
	        dto.setFechaPago(p.getFechaPago());
	        dto.setMedioPago(p.getMedioPago());
	        dto.setNumeroOperacion(p.getNumeroOperacion());
	        dto.setObservaciones(p.getObservaciones());
	        dto.setTipoServicio(p.getTipoServicio());
	
        Comprobante c = p.getComprobante();
        if (c != null) {
            dto.setTipoComprobante(c.getTipoComprobante());
            dto.setNumeroComprobante(c.getNumeroCompleto());
            dto.setFechaEmision(c.getFechaEmision());
            dto.setIdComprobante(c.getIdComprobante());
        }
	
	        var lotes = p.getContrato().getLotes();
	        if (lotes != null && !lotes.isEmpty()) {
	            Lote l = lotes.iterator().next().getLote();
	            if (l != null) {
	                dto.setManzana(l.getManzana());
	                dto.setNumeroLote(l.getNumeroLote());
	                if (l.getPrograma() != null) {
	                    dto.setIdPrograma(l.getPrograma().getIdPrograma());
	                    dto.setNombrePrograma(l.getPrograma().getNombrePrograma());
	                }
	            }
	        }
	
	        // Anulación — siempre presente
	        dto.setAnulado(Boolean.TRUE.equals(p.getAnulado()));
	        dto.setMotivoAnulacion(p.getMotivoAnulacion());
	        dto.setFechaAnulacion(p.getFechaAnulacion());
	        dto.setAnuladoPor(p.getAnuladoPor());
	
	        // Solo admin
	        if (incluirDatosAdmin) {
	            var clientes = p.getContrato().getClientes();
	            if (clientes != null && !clientes.isEmpty()
	                    && clientes.iterator().next().getCliente() != null) {
	                var cli = clientes.iterator().next().getCliente();
	                dto.setNombreCliente(cli.getNombre() + " " + cli.getApellidos());
	            }
	            if (p.getContrato().getMoneda() != null) {
	                dto.setMoneda(p.getContrato().getMoneda().name());
	            }
	        }
	
	        return dto;
	    }
	
	    // ── Helper ────────────────────────────────────────────────────────────────

	    private void limpiarCache() {
	        if (cacheManager.getCache("contratos") != null) {
	            cacheManager.getCache("contratos").clear();
	        }
	    }

	    // ═══════════════════════════════════════════════════════════════════════════
	    // Vouchers
	    // ═══════════════════════════════════════════════════════════════════════════

	    private String subirImagenVoucher(MultipartFile file, Integer idContrato, Integer idInscripcion) throws IOException {
	        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
	        String publicId = "inscripcion-" + idInscripcion + "-" + timestamp;
	        Map<?, ?> params = ObjectUtils.asMap(
	            "folder", "vouchers/contrato-" + idContrato, "public_id", publicId);
	        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), params);
	        return result.get("url").toString();
	    }

	    private void guardarVouchers(List<MultipartFile> files, PagoInscripcionComprobante pago,
	                                  Integer idContrato, Integer idInscripcion) throws IOException {
	        if (files != null && !files.isEmpty()) {
	            for (MultipartFile file : files) {
	                Voucher v = new Voucher();
	                v.setTipoOrigen("PAGO_INSCRIPCION");
	                v.setReferenciaId(pago.getIdPagoInscripcionComprobante());
	                v.setUrl(subirImagenVoucher(file, idContrato, idInscripcion));
	                voucherRepository.save(v);
	            }
	        }
	    }
	}