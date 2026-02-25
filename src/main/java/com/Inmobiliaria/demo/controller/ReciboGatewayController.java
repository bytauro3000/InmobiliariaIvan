package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.client.InscripcionClient;
import com.Inmobiliaria.demo.client.ReciboClient;
import com.Inmobiliaria.demo.dto.LecturaUnificadaDTO;
import com.Inmobiliaria.demo.dto.ReciboDTO;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.Lote;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gateway/recibos")
public class ReciboGatewayController {

    @Autowired
    private ReciboClient reciboClient;

    @Autowired
    private InscripcionClient inscripcionClient;

    @Autowired
    private ContratoRepository contratoRepository;

    @GetMapping("/preparar-planilla-unificada")
    @PreAuthorize("hasAuthority('ROLE_SECRETARIA')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<LecturaUnificadaDTO>> prepararPlanillaUnificada(
            @RequestParam Integer idPrograma,
            @RequestParam(required = false) String manzana,
            @RequestParam(required = false) String loteFiltro) {

        List<Contrato> contratos = contratoRepository.findByLotesLoteProgramaIdPrograma(idPrograma);
        List<ReciboDTO> historialCompleto = reciboClient.listarTodos();
        List<Integer> inscritosLuz = inscripcionClient.obtenerContratosPorServicio("LUZ");
        List<Integer> inscritosAgua = inscripcionClient.obtenerContratosPorServicio("AGUA");

        List<LecturaUnificadaDTO> planilla = contratos.stream().map(contrato -> {
            if (contrato.getLotes().isEmpty()) return null;
            Lote loteEntidad = contrato.getLotes().get(0).getLote();

            // Filtros de búsqueda (Mz/Lt)
            if (manzana != null && !manzana.trim().isEmpty() && !loteEntidad.getManzana().equalsIgnoreCase(manzana)) return null;
            if (loteFiltro != null && !loteFiltro.trim().isEmpty() && !loteEntidad.getNumeroLote().equalsIgnoreCase(loteFiltro)) return null;

            LecturaUnificadaDTO dto = new LecturaUnificadaDTO();
            dto.setIdContrato(contrato.getIdContrato());
            var cliente = contrato.getClientes().get(0).getCliente();
            dto.setClienteNombre(cliente.getNombre() + " " + cliente.getApellidos());
            dto.setManzana(loteEntidad.getManzana());
            dto.setLote(loteEntidad.getNumeroLote());

            // Verificar inscripciones
            dto.setInscritoLuz(inscritosLuz.contains(contrato.getIdContrato()));
            dto.setInscritoAgua(inscritosAgua.contains(contrato.getIdContrato()));

            // 🟢 FILTRO DE EXCLUSIÓN: Si no tiene ninguna inscripción, lo descartamos
            if (!dto.isInscritoLuz() && !dto.isInscritoAgua()) return null; //

            // Cargar lecturas anteriores (solo si están inscritos)
            if (dto.isInscritoLuz()) {
                dto.setLecturaAntLuz(historialCompleto.stream()
                    .filter(r -> r.getIdContrato().equals(contrato.getIdContrato()) && "LUZ".equals(r.getTipoServicio()))
                    .map(ReciboDTO::getLecturaActual).reduce((f, s) -> s).orElse(0.0));
            }
            if (dto.isInscritoAgua()) {
                dto.setLecturaAntAgua(historialCompleto.stream()
                    .filter(r -> r.getIdContrato().equals(contrato.getIdContrato()) && "AGUA".equals(r.getTipoServicio()))
                    .map(ReciboDTO::getLecturaActual).reduce((f, s) -> s).orElse(0.0));
            }

            return dto;
        })
        .filter(Objects::nonNull)
        // 🟢 ORDENAMIENTO POR MZ Y LOTE
        .sorted((a, b) -> {
            int resMz = a.getManzana().compareToIgnoreCase(b.getManzana());
            if (resMz != 0) return resMz;
            return a.getLote().compareToIgnoreCase(b.getLote());
        })
        .collect(Collectors.toList());

        return ResponseEntity.ok(planilla);
    }

    @PostMapping("/guardar-planilla-unificada")
    @PreAuthorize("hasAuthority('ROLE_SECRETARIA')")
    public ResponseEntity<?> guardarPlanillaUnificada(
            @RequestBody List<LecturaUnificadaDTO> planilla, 
            @RequestParam(required = false) String fechaGiro) {
        try {
            LocalDate fechaParaRegistro = (fechaGiro != null) ? LocalDate.parse(fechaGiro) : LocalDate.now();
            
            for (LecturaUnificadaDTO u : planilla) {
                // Registrar Luz
                if (u.isInscritoLuz() && u.getLecturaActLuz() != null && u.getLecturaActLuz() > u.getLecturaAntLuz()) {
                    ReciboDTO rLuz = new ReciboDTO();
                    rLuz.setIdContrato(u.getIdContrato());
                    rLuz.setTipoServicio("LUZ");
                    rLuz.setLecturaAnterior(u.getLecturaAntLuz());
                    rLuz.setLecturaActual(u.getLecturaActLuz());
                    rLuz.setFechaGiro(fechaParaRegistro);
                    reciboClient.registrarLectura(rLuz);
                }

                // Registrar Agua
                if (u.isInscritoAgua() && u.getLecturaActAgua() != null && u.getLecturaActAgua() > u.getLecturaAntAgua()) {
                    ReciboDTO rAgua = new ReciboDTO();
                    rAgua.setIdContrato(u.getIdContrato());
                    rAgua.setTipoServicio("AGUA");
                    rAgua.setLecturaAnterior(u.getLecturaAntAgua());
                    rAgua.setLecturaActual(u.getLecturaActAgua());
                    rAgua.setFechaGiro(fechaParaRegistro);
                    reciboClient.registrarLectura(rAgua);
                }
            }
            return ResponseEntity.ok("Procesado");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}