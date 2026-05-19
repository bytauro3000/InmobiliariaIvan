package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.client.InscripcionClient;
import com.Inmobiliaria.demo.dto.InscripcionConPagoRequestDTO;
import com.Inmobiliaria.demo.dto.InscripcionConPagoResponseDTO;
import com.Inmobiliaria.demo.dto.InscripcionResumenDTO;
import com.Inmobiliaria.demo.dto.InscripcionServicioDTO;
import com.Inmobiliaria.demo.entity.Comprobante;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.Lote;
import com.Inmobiliaria.demo.entity.PagoInicial;
import com.Inmobiliaria.demo.enums.TipoOrigenComprobante;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import com.Inmobiliaria.demo.repository.PagoInicialRepository;
import com.Inmobiliaria.demo.service.ComprobanteService;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gateway/inscripciones")
@RequiredArgsConstructor
public class InscripcionGatewayController {

    private final CacheManager           cacheManager;
    private final InscripcionClient      inscripcionClient;
    private final ContratoRepository     contratoRepository;
    private final PagoInicialRepository  pagoInicialRepository;
    private final ComprobanteService     comprobanteService;

    /**
     * Devuelve el resumen de todos los contratos con su estado de servicios (LUZ/AGUA).
     *
     * CORRECCIÓN: Se reemplazó findResumenParaInscripciones() (una sola query con
     * DISTINCT + múltiples JOIN FETCH + ORDER BY que Hibernate rechaza) por dos
     * queries separadas que se combinan en memoria, igual que el patrón ya usado
     * en findAllConClientes() + findAllConLotes().
     */
    @GetMapping("/resumen")
    @PreAuthorize("hasAnyAuthority('ROLE_SECRETARIA', 'ROLE_ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<InscripcionResumenDTO>> listarResumen() {
        try {
            // 1. Una sola llamada al microservicio → { idContrato → ["LUZ","AGUA"] }
            Map<Integer, List<String>> serviciosPorContrato =
                    inscripcionClient.obtenerResumenServicios();

            // 2a. Query A: contratos con clientes cargados
            List<Contrato> contratosConClientes =
                    contratoRepository.findResumenInscripcionesConClientes();

            // 2b. Query B: contratos con lotes cargados → mapa para lookup rápido
            Map<Integer, Contrato> contratosConLotes =
                    contratoRepository.findResumenInscripcionesConLotes()
                            .stream()
                            .collect(Collectors.toMap(Contrato::getIdContrato, c -> c));

            // 2c. Ordenar por idContrato descendente (reemplaza el ORDER BY eliminado)
            contratosConClientes.sort(Comparator.comparing(Contrato::getIdContrato).reversed());

            // 3. Armar el DTO de respuesta
            List<InscripcionResumenDTO> resultado = contratosConClientes.stream().map(c -> {

                // Nombre del primer cliente (o "—" si no tiene)
                String nombre = "—";
                if (c.getClientes() != null && !c.getClientes().isEmpty()) {
                    var cliente = c.getClientes().get(0).getCliente();
                    nombre = cliente.getNombre() + " " + cliente.getApellidos();
                }

                // Manzana y lote: se toman del mapa de lotes por idContrato
                String manzana = "", numeroLote = "";
                Contrato cLotes = contratosConLotes.get(c.getIdContrato());
                if (cLotes != null && cLotes.getLotes() != null && !cLotes.getLotes().isEmpty()) {
                    Lote l = cLotes.getLotes().get(0).getLote();
                    manzana    = l.getManzana();
                    numeroLote = l.getNumeroLote();
                }

                // Servicios inscritos para este contrato
                List<String> servicios = serviciosPorContrato
                        .getOrDefault(c.getIdContrato(), List.of());

                // CORRECCIÓN: el campo en el DTO es "nombreCliente" (no "clienteNombre")
                return new InscripcionResumenDTO(
                        c.getIdContrato(),
                        nombre,
                        manzana,
                        numeroLote,
                        servicios.contains("LUZ"),
                        servicios.contains("AGUA")
                );

            }).collect(Collectors.toList());

            return ResponseEntity.ok(resultado);

        } catch (FeignException e) {
            return ResponseEntity.status(e.status()).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Endpoint original (sin comprobante) ──────────────────────────────────

    @PostMapping("/registrar")
    @PreAuthorize("hasAuthority('ROLE_SECRETARIA')")
    public ResponseEntity<?> registrarInscripcion(@RequestBody InscripcionServicioDTO dto) {
        try {
            InscripcionServicioDTO resultado = inscripcionClient.crearInscripcion(dto);
            limpiarCache();
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (FeignException e) {
            return ResponseEntity.status(e.status()).body(e.contentUTF8());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error crítico de comunicación con Servicios Básicos.");
        }
    }

    // ── Registrar inscripción + generar comprobante ──────────────────────────

    /**
     * Registra la inscripción en el microservicio Y guarda un PagoInicial con
     * comprobante en el monolito. Devuelve el idPagoInicial para descargar el PDF.
     */
    @PostMapping("/registrar-con-pago")
    @PreAuthorize("hasAuthority('ROLE_SECRETARIA')")
    @Transactional
    public ResponseEntity<?> registrarInscripcionConPago(
            @RequestBody InscripcionConPagoRequestDTO request,
            Authentication authentication) {

        // 1. Validaciones básicas
        if (request.getIdContrato() == null) {
            return ResponseEntity.badRequest().body("El idContrato es obligatorio.");
        }
        if (request.getTipoServicio() == null || request.getTipoServicio().isBlank()) {
            return ResponseEntity.badRequest().body("El tipo de servicio es obligatorio.");
        }
        if (request.getMontoPagado() == null || request.getMontoPagado().signum() <= 0) {
            return ResponseEntity.badRequest().body("El monto pagado debe ser mayor a cero.");
        }
        if (request.getMedioPago() == null) {
            return ResponseEntity.badRequest().body("El medio de pago es obligatorio.");
        }
        if (request.getTipoComprobante() == null) {
            return ResponseEntity.badRequest().body("El tipo de comprobante es obligatorio.");
        }

        // 2. Registrar inscripción en el microservicio
        try {
            InscripcionServicioDTO inscripcionDTO = new InscripcionServicioDTO();
            inscripcionDTO.setIdContrato(request.getIdContrato());
            inscripcionDTO.setTipoServicio(request.getTipoServicio());
            inscripcionDTO.setMontoPagado(request.getMontoPagado());
            inscripcionDTO.setFechaInscripcion(
                    request.getFechaPago() != null ? request.getFechaPago() : LocalDate.now());
            inscripcionClient.crearInscripcion(inscripcionDTO);
            limpiarCache();
        } catch (FeignException e) {
            return ResponseEntity.status(e.status()).body(e.contentUTF8());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al comunicarse con el servicio de inscripciones: " + e.getMessage());
        }

        // 3. Cargar el Contrato del monolito
        Contrato contrato = contratoRepository.findById(request.getIdContrato())
                .orElseThrow(() -> new NegocioException(
                        "Contrato no encontrado con ID: " + request.getIdContrato()));

        // 4. Generar el comprobante
        Comprobante comprobante;
        if (request.getNumeroComprobantePersonalizado() != null
                && !request.getNumeroComprobantePersonalizado().isBlank()) {
            comprobante = comprobanteService.generarComprobanteConNumero(
                    request.getTipoComprobante(),
                    TipoOrigenComprobante.PAGO_INSCRIPCION,
                    request.getIdContrato(),
                    request.getMontoPagado(),
                    request.getFechaPago() != null ? request.getFechaPago() : LocalDate.now(),
                    request.getNumeroComprobantePersonalizado());
        } else {
            comprobante = comprobanteService.generarComprobante(
                    request.getTipoComprobante(),
                    TipoOrigenComprobante.PAGO_INSCRIPCION,
                    request.getIdContrato(),
                    request.getMontoPagado(),
                    request.getFechaPago() != null ? request.getFechaPago() : LocalDate.now());
        }

        // 5. Construir y guardar el PagoInicial
        PagoInicial pago = new PagoInicial();
        pago.setContrato(contrato);
        pago.setImportePagado(request.getMontoPagado());
        pago.setFechaPago(request.getFechaPago() != null ? request.getFechaPago() : LocalDate.now());
        pago.setMedioPago(request.getMedioPago());
        pago.setNumeroOperacion(request.getNumeroOperacion());
        pago.setObservaciones(request.getObservaciones() != null
                ? request.getObservaciones()
                : "Inscripción de servicio de " + request.getTipoServicio().toUpperCase());
        pago.setComprobante(comprobante);
        PagoInicial pagoGuardado = pagoInicialRepository.save(pago);

        // 6. Respuesta
        InscripcionConPagoResponseDTO response = new InscripcionConPagoResponseDTO(
                pagoGuardado.getIdPagoInicial(),
                comprobante.getNumeroCompleto(),
                request.getTipoServicio(),
                request.getIdContrato()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Descargar PDF del comprobante de inscripción ─────────────────────────

    @GetMapping("/pago/{idPagoInicial}/comprobante-pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> descargarComprobanteInscripcion(
            @PathVariable Integer idPagoInicial,
            Authentication authentication) {

        try {
            PagoInicial pago = pagoInicialRepository.findById(idPagoInicial)
                    .orElseThrow(() -> new NegocioException(
                            "Pago de inscripción no encontrado con ID: " + idPagoInicial));

            String rolUsuario = "SECRETARIA";
            if (authentication != null && authentication.getAuthorities() != null) {
                rolUsuario = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(r -> r.replace("ROLE_", ""))
                        .findFirst()
                        .orElse("SECRETARIA");
            }

            // Extraer tipoServicio desde las observaciones
            String tipoServicio = "SERVICIO";
            String obs = pago.getObservaciones();
            if (obs != null) {
                if (obs.toUpperCase().contains("LUZ"))  tipoServicio = "LUZ";
                if (obs.toUpperCase().contains("AGUA")) tipoServicio = "AGUA";
            }

            byte[] pdf = ComprobanteInscripcionPdf.generar(pago, tipoServicio, rolUsuario);

            String nombreArchivo = "comprobante-inscripcion-" + idPagoInicial + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + nombreArchivo + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (NegocioException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── Endpoint existente ───────────────────────────────────────────────────

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