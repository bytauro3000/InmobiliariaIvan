package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.client.InscripcionClient;
import com.Inmobiliaria.demo.dto.*;
import com.Inmobiliaria.demo.entity.Comprobante;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.Lote;
import com.Inmobiliaria.demo.entity.PagoInscripcionComprobante;
import com.Inmobiliaria.demo.enums.TipoOrigenComprobante;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import com.Inmobiliaria.demo.repository.PagoInscripcionComprobanteRepository;
import com.Inmobiliaria.demo.service.ComprobanteService;
import com.Inmobiliaria.demo.service.impl.InscripcionComprobanteServiceImpl;
import com.Inmobiliaria.demo.util.ComprobanteInscripcionPdf;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gateway/inscripciones")
@RequiredArgsConstructor
public class InscripcionGatewayController {

    private final CacheManager                         cacheManager;
    private final InscripcionClient                    inscripcionClient;
    private final ContratoRepository                   contratoRepository;
    private final PagoInscripcionComprobanteRepository pagoInscripcionComprobanteRepository;
    private final ComprobanteService                   comprobanteService;
    private final InscripcionComprobanteServiceImpl    inscripcionComprobanteService;

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
                    var cliente = c.getClientes().get(0).getCliente();
                    nombre = cliente.getNombre() + " " + cliente.getApellidos();
                }

                String manzana = "", numeroLote = "";
                Contrato cLotes = contratosConLotes.get(c.getIdContrato());
                if (cLotes != null && cLotes.getLotes() != null && !cLotes.getLotes().isEmpty()) {
                    Lote l = cLotes.getLotes().get(0).getLote();
                    manzana    = l.getManzana();
                    numeroLote = l.getNumeroLote();
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

    @PostMapping("/{idInscripcion}/abonar")
    @PreAuthorize("hasAuthority('ROLE_SECRETARIA')")
    @Transactional
    public ResponseEntity<?> registrarAbono(
            @PathVariable Integer idInscripcion,
            @RequestBody AbonoInscripcionRequestDTO request,
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
        LocalDate fechaPago = request.getFechaPago() != null ? request.getFechaPago() : LocalDate.now();

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

            byte[] pdf = ComprobanteInscripcionPdf.generar(pago, tipoServicio, rolUsuario);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"comprobante-inscripcion-" + idPago + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (NegocioException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }


    // ── 7. Listar todos los pagos de inscripción ─────────────────────────────

    @GetMapping("/pagos")
    @PreAuthorize("hasAnyAuthority('ROLE_SECRETARIA', 'ROLE_ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PagoInscripcionDTO>> listarPagos() {
        try {
            List<PagoInscripcionComprobante> pagos =
                    pagoInscripcionComprobanteRepository.findAllConDetalle();

            List<PagoInscripcionDTO> resultado = pagos.stream().map(p -> {

                Comprobante c = p.getComprobante();

                String manzana        = null;
                String numeroLote     = null;
                Integer idPrograma    = null;
                String nombrePrograma = null;

                var lotes = p.getContrato().getLotes();
                if (lotes != null && !lotes.isEmpty()) {
                    Lote l = lotes.get(0).getLote();
                    if (l != null) {
                        manzana    = l.getManzana();
                        numeroLote = l.getNumeroLote();
                        if (l.getPrograma() != null) {
                            idPrograma     = l.getPrograma().getIdPrograma();
                            nombrePrograma = l.getPrograma().getNombrePrograma();
                        }
                    }
                }

                return new PagoInscripcionDTO(
                        p.getIdPagoInscripcionComprobante(),
                        p.getContrato().getIdContrato(),
                        p.getImportePagado(),
                        p.getFechaPago(),
                        p.getMedioPago(),
                        p.getNumeroOperacion(),
                        p.getObservaciones(),
                        c != null ? c.getTipoComprobante() : null,
                        c != null ? c.getNumeroCompleto()  : null,
                        c != null ? c.getFechaEmision()    : null,
                        p.getTipoServicio(),    // ya explícito, no requiere parseo
                        manzana,
                        numeroLote,
                        idPrograma,
                        nombrePrograma
                );
            }).collect(Collectors.toList());

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

    // ── Helper ───────────────────────────────────────────────────────────────

    private void limpiarCache() {
        if (cacheManager.getCache("contratos") != null) {
            cacheManager.getCache("contratos").clear();
        }
    }
}