package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.dto.NotaCreditoRequestDTO;
import com.Inmobiliaria.demo.entity.*;
import com.Inmobiliaria.demo.enums.Moneda;
import com.Inmobiliaria.demo.enums.TipoComprobante;
import com.Inmobiliaria.demo.enums.TipoOrigenComprobante;
import com.Inmobiliaria.demo.exception.NegocioException;
import com.Inmobiliaria.demo.repository.*;
import com.Inmobiliaria.demo.service.ComprobanteService;
import com.Inmobiliaria.demo.service.PagoLetraService;
import com.Inmobiliaria.demo.service.SunatEnvioService;
import com.Inmobiliaria.demo.util.NotaCreditoElectronicaPdf;
import com.Inmobiliaria.demo.util.NotaCreditoReciboPdf;
import com.Inmobiliaria.demo.util.NumeroALetras;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/nota-credito")
@RequiredArgsConstructor
public class NotaCreditoController {

    private static final Logger log = LoggerFactory.getLogger(NotaCreditoController.class);

    private final PagoLetraRepository pagoLetraRepository;
    private final PagoMoraRepository pagoMoraRepository;
    private final PagoInicialRepository pagoInicialRepository;
    private final PagoInscripcionComprobanteRepository pagoInscripcionRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final ComprobanteService comprobanteService;
    private final PagoLetraService pagoLetraService;
    private final SunatEnvioService sunatEnvioService;

    @PostMapping("/enviar")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRADOR')")
    @Transactional
    public ResponseEntity<?> enviarNotaCredito(
            @Valid @RequestBody NotaCreditoRequestDTO request,
            Principal principal) {

        String anuladoPor = principal.getName();
        String codMotivo = request.getCodMotivo();
        String desMotivo = request.getDesMotivo();

        PagoBase pago = findPagoByTipo(request.getTipoPago(), request.getIdPago());
        Comprobante comprobanteOriginal = pago.getComprobante();

        if (comprobanteOriginal == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "El pago no tiene un comprobante asociado."));
        }

        if (comprobanteOriginal.getTipoComprobante() != TipoComprobante.BOLETA) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo se pueden anular boletas con nota de crédito."));
        }

        String serie = comprobanteOriginal.getSerie();
        if (!serie.startsWith("B")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "La serie " + serie + " no fue emitida por este CEE. Solo se puede anular con SUNAT boletas serie B001."));
        }

        if (Boolean.TRUE.equals(pago.getAnulado())) {
            return ResponseEntity.badRequest().body(Map.of("error", "El pago ya fue anulado."));
        }

        Cliente cliente = extractCliente(pago);
        Contrato contrato = extractContrato(pago);
        String descripcionOriginal = comprobanteOriginal.getDescripcion();
        String descripcion = (descripcionOriginal != null && !descripcionOriginal.isBlank())
                ? descripcionOriginal
                : "NOTA DE CREDITO - " + desMotivo + " - " + comprobanteOriginal.getNumeroCompleto();

        Comprobante notaCredito = comprobanteService.generarNotaCredito(
                comprobanteOriginal, codMotivo, desMotivo, anuladoPor);

        try {
            Map<String, Object> respuesta = sunatEnvioService.enviarNotaCredito(
                    cliente, contrato, notaCredito, comprobanteOriginal,
                    comprobanteOriginal.getMonto(), descripcion, codMotivo, desMotivo);

            notaCredito.setEstadoSunat("ACEPTADA");
            notaCredito.setHashCdr((String) respuesta.get("hash"));
            comprobanteRepository.save(notaCredito);

            // Para pago de letra: anula pago + restaura letra + anula moras
            if (request.getTipoPago().equalsIgnoreCase("LETRA")) {
                pagoLetraService.anularPagoConMoras(
                        request.getIdPago(),
                        "NC " + notaCredito.getNumeroCompleto() + " - " + desMotivo,
                        anuladoPor);
            } else {
                // Otros tipos: anulación simple inline
                pago.setAnulado(true);
                pago.setMotivoAnulacion("NC " + notaCredito.getNumeroCompleto() + " - " + desMotivo);
                pago.setFechaAnulacion(LocalDateTime.now());
                pago.setAnuladoPor(anuladoPor);
                savePago(pago);
            }

            comprobanteOriginal.setIdNotaCreditoAnulacion(notaCredito.getIdComprobante());
            comprobanteOriginal.setFechaAnulacionSunat(LocalDateTime.now());
            comprobanteRepository.save(comprobanteOriginal);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("mensaje", "Nota de crédito " + notaCredito.getNumeroCompleto()
                    + " emitida y aceptada por SUNAT. Boleta " + comprobanteOriginal.getNumeroCompleto() + " anulada.");
            result.put("notaCredito", notaCredito.getNumeroCompleto());
            result.put("comprobanteAnulado", comprobanteOriginal.getNumeroCompleto());
            result.put("idNotaCredito", notaCredito.getIdComprobante());
            result.put("tipoComprobanteNC", notaCredito.getTipoComprobante().name());
            result.put("serieNC", notaCredito.getSerie());
            return ResponseEntity.ok(result);

        } catch (NegocioException e) {
            notaCredito.setEstadoSunat("RECHAZADA");
            notaCredito.setMotivoNotaCredito(e.getMessage());
            comprobanteRepository.save(notaCredito);

            log.error("SUNAT rechazo NC {} para boleta {}: {}",
                    notaCredito.getNumeroCompleto(), comprobanteOriginal.getNumeroCompleto(), e.getMessage());

            return ResponseEntity.badRequest().body(Map.of(
                    "error", "SUNAT rechazó la nota de crédito: " + e.getMessage(),
                    "notaCredito", notaCredito.getNumeroCompleto()));
        }
    }

    private PagoBase findPagoByTipo(String tipoPago, Integer idPago) {
        return switch (tipoPago.toUpperCase()) {
            case "LETRA" -> pagoLetraRepository.findById(idPago)
                    .orElseThrow(() -> new NegocioException("Pago de letra no encontrado: " + idPago));
            case "MORA" -> pagoMoraRepository.findById(idPago)
                    .orElseThrow(() -> new NegocioException("Pago de mora no encontrado: " + idPago));
            case "INICIAL" -> pagoInicialRepository.findById(idPago)
                    .orElseThrow(() -> new NegocioException("Pago inicial no encontrado: " + idPago));
            case "INSCRIPCION" -> pagoInscripcionRepository.findById(idPago)
                    .orElseThrow(() -> new NegocioException("Pago de inscripción no encontrado: " + idPago));
            default -> throw new NegocioException("Tipo de pago no válido: " + tipoPago);
        };
    }

    private Cliente extractCliente(PagoBase pago) {
        Contrato contrato = extractContrato(pago);
        if (contrato.getClientes() != null && !contrato.getClientes().isEmpty()) {
            ContratoCliente cc = contrato.getClientes().iterator().next();
            return cc.getCliente();
        }
        throw new NegocioException("El contrato no tiene cliente asociado.");
    }

    private Contrato extractContrato(PagoBase pago) {
        if (pago instanceof PagoLetras pl) {
            return pl.getLetra().getContrato();
        } else if (pago instanceof PagoInicial pi) {
            return pi.getContrato();
        } else if (pago instanceof PagoMora pm) {
            return pm.getMora().getLetra().getContrato();
        } else if (pago instanceof PagoInscripcionComprobante pic) {
            return pic.getContrato();
        }
        throw new NegocioException("No se pudo determinar el contrato del pago.");
    }

    private void savePago(PagoBase pago) {
        if (pago instanceof PagoLetras pl) {
            pagoLetraRepository.save(pl);
        } else if (pago instanceof PagoInicial pi) {
            pagoInicialRepository.save(pi);
        } else if (pago instanceof PagoMora pm) {
            pagoMoraRepository.save(pm);
        } else if (pago instanceof PagoInscripcionComprobante pic) {
            pagoInscripcionRepository.save(pic);
        }
    }

    // ─── PDF: Nota de Crédito Electrónica ─────────────────────────────────
    @GetMapping("/{idNotaCredito}/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> descargarPdfNotaCredito(
            @PathVariable Long idNotaCredito) {
        Comprobante nc = comprobanteRepository.findById(idNotaCredito)
                .orElseThrow(() -> new NegocioException("Nota de crédito no encontrada: " + idNotaCredito));

        if (nc.getTipoComprobante() != TipoComprobante.NOTA_CREDITO) {
            return ResponseEntity.badRequest().build();
        }

        Comprobante orig = nc.getComprobanteReferencia();
        if (orig == null) {
            return ResponseEntity.badRequest().build();
        }

        PagoBase pago = findPagoByTipoOrigen(
                nc.getTipoOrigen(),
                nc.getReferenciaId());
        Cliente cliente = extractCliente(pago);
        String clienteNombre = (cliente.getNombre() + " " + (cliente.getApellidos() != null ? cliente.getApellidos() : "")).trim();
        String clienteDoc = cliente.getNumDoc() != null ? cliente.getNumDoc() : "";
        String direccionCliente = cliente.getDireccion() != null ? cliente.getDireccion().toUpperCase() : "-";
        Contrato contrato = extractContrato(pago);
        Moneda moneda = contrato.getMoneda() != null ? contrato.getMoneda() : Moneda.USD;

        String descripcion = buildDescripcionAnulacion(orig, pago, contrato);

        byte[] pdf = NotaCreditoElectronicaPdf.generar(
                nc.getSerie(), nc.getNumero().toString(), nc.getFechaEmision().toString(),
                moneda.name(), String.format("%.2f", nc.getMonto()),
                clienteNombre.toUpperCase(), clienteDoc, direccionCliente,
                descripcion,
                NumeroALetras.convertir(nc.getMonto(), moneda),
                nc.getMonto(),
                nc.getHashCdr(),
                nc.getCodigoMotivo() != null ? nc.getCodigoMotivo() : "",
                nc.getMotivoNotaCredito() != null ? nc.getMotivoNotaCredito() : "",
                orig.getSerie(), orig.getNumero().toString(),
                orig.getFechaEmision() != null ? orig.getFechaEmision().toString() : ""
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=nc-electronica-" + nc.getNumeroCompleto() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ─── PDF: Nota de Crédito de Recibo ─────────────────────────────────
    @GetMapping("/recibo/{idNotaCredito}/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> descargarPdfNotaCreditoRecibo(
            @PathVariable Long idNotaCredito) {
        Comprobante nc = comprobanteRepository.findById(idNotaCredito)
                .orElseThrow(() -> new NegocioException("Nota de crédito no encontrada: " + idNotaCredito));

        if (nc.getTipoComprobante() != TipoComprobante.NOTA_CREDITO) {
            return ResponseEntity.badRequest().build();
        }

        Comprobante orig = nc.getComprobanteReferencia();
        if (orig == null) {
            orig = nc;
        }

        PagoBase pago = findPagoByTipoOrigen(
                nc.getTipoOrigen(),
                nc.getReferenciaId());
        Cliente cliente = extractCliente(pago);
        String clienteNombre = (cliente.getNombre() + " " + (cliente.getApellidos() != null ? cliente.getApellidos() : "")).trim();
        String clienteDoc = cliente.getNumDoc() != null ? cliente.getNumDoc() : "";
        String direccionCliente = cliente.getDireccion() != null ? cliente.getDireccion().toUpperCase() : "-";
        Contrato contrato = extractContrato(pago);
        Moneda moneda = contrato.getMoneda() != null ? contrato.getMoneda() : Moneda.USD;

        String descripcion = buildDescripcionAnulacion(orig, pago, contrato);

        byte[] pdf = NotaCreditoReciboPdf.generar(
                nc.getSerie(), nc.getNumero().toString(), nc.getFechaEmision().toString(),
                moneda.name(), String.format("%.2f", nc.getMonto()),
                clienteNombre.toUpperCase(), clienteDoc, direccionCliente,
                descripcion,
                NumeroALetras.convertir(nc.getMonto(), moneda),
                nc.getMonto(),
                orig.getSerie(), orig.getNumero().toString(),
                orig.getFechaEmision() != null ? orig.getFechaEmision().toString() : ""
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=nc-recibo-" + nc.getNumeroCompleto() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ─── Buscar NC por ID del comprobante original ─────────────────────
    @GetMapping("/por-original/{idOriginal}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> buscarPorComprobanteOriginal(@PathVariable Long idOriginal) {
        var ncOpt = comprobanteRepository.findByComprobanteReferenciaIdComprobante(idOriginal);
        if (ncOpt.isEmpty()) {
            var empty = new HashMap<String, Object>();
            empty.put("idNotaCredito", null);
            return ResponseEntity.ok(empty);
        }
        Comprobante nc = ncOpt.get();
        return ResponseEntity.ok(Map.of(
                "idNotaCredito", nc.getIdComprobante(),
                "numeroCompleto", nc.getNumeroCompleto(),
                "serie", nc.getSerie()
        ));
    }

    // ─── Helper: descripción enriquecida con datos de letra y lote ────────
    private String buildDescripcionAnulacion(Comprobante orig, PagoBase pago, Contrato contrato) {
        StringBuilder sb = new StringBuilder();
        sb.append("ANULACION - ").append(orig.getNumeroCompleto());

        String letraNumero = "";
        if (pago instanceof PagoLetras pl) {
            String nl = pl.getLetra().getNumeroLetra();
            if (nl != null) {
                if (nl.contains("/")) nl = nl.substring(0, nl.indexOf("/"));
                letraNumero = nl;
            }
        }

        String loteInfo = "";
        if (contrato.getLotes() != null && !contrato.getLotes().isEmpty()) {
            var lot = contrato.getLotes().iterator().next().getLote();
            if (lot != null) {
                loteInfo = "MZ." + lot.getManzana() + " LT." + lot.getNumeroLote();
                if (lot.getPrograma() != null) {
                    loteInfo += " DEL PROGRAMA " + lot.getPrograma().getNombrePrograma().toUpperCase();
                }
            }
        }

        if (!letraNumero.isEmpty() || !loteInfo.isEmpty()) {
            sb.append("\n");
            if (!letraNumero.isEmpty()) sb.append("LETRA N° ").append(letraNumero).append(" ");
            sb.append(loteInfo);
        }

        return sb.toString();
    }

    // ─── Helper para encontrar pago por tipo de origen ───────────────────
    private PagoBase findPagoByTipoOrigen(TipoOrigenComprobante tipoOrigen, Integer referenciaId) {
        if (referenciaId == null) throw new NegocioException("El comprobante no tiene referencia de pago.");
        return switch (tipoOrigen) {
            case PAGO_LETRA -> pagoLetraRepository.findById(referenciaId)
                    .orElseThrow(() -> new NegocioException("Pago de letra no encontrado: " + referenciaId));
            case PAGO_MORA -> pagoMoraRepository.findById(referenciaId)
                    .orElseThrow(() -> new NegocioException("Pago de mora no encontrado: " + referenciaId));
            case PAGO_INICIAL -> pagoInicialRepository.findById(referenciaId)
                    .orElseThrow(() -> new NegocioException("Pago inicial no encontrado: " + referenciaId));
            case PAGO_INSCRIPCION -> pagoInscripcionRepository.findById(referenciaId)
                    .orElseThrow(() -> new NegocioException("Pago de inscripción no encontrado: " + referenciaId));
        };
    }

    @GetMapping("/motivos")
    public ResponseEntity<?> listarMotivos() {
        Map<String, String> motivos = new HashMap<>();
        motivos.put("01", "Anulacion de la operacion");
        motivos.put("02", "Anulacion parcial de la operacion");
        motivos.put("03", "Descuento total");
        motivos.put("06", "Devolucion total");
        motivos.put("07", "Devolucion por item");
        motivos.put("09", "Disminucion en el valor");
        return ResponseEntity.ok(motivos);
    }
}
