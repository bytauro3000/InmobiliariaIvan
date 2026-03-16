package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.client.InscripcionClient;
import com.Inmobiliaria.demo.client.ReciboClient;
import com.Inmobiliaria.demo.dto.LecturaUnificadaDTO;
import com.Inmobiliaria.demo.dto.ReciboConClienteDTO;
import com.Inmobiliaria.demo.dto.ReciboDTO;
import com.Inmobiliaria.demo.entity.Cliente;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.Lote;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import com.Inmobiliaria.demo.util.ReciboServiciosPdfGenerator;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gateway/recibos")
@RequiredArgsConstructor
public class ReciboGatewayController {

    
    private final ReciboClient reciboClient;

    
    private final InscripcionClient inscripcionClient;

    
    private final ContratoRepository contratoRepository;
    
   
    private final ModelMapper modelMapper;

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
        //ORDENAMIENTO POR MZ Y LOTE
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
            @RequestParam(required = false) String fechaGiro,
            @RequestParam(required = false) String fechaLectura) {
        try {
            LocalDate fechaParaRegistro = (fechaGiro != null) ? LocalDate.parse(fechaGiro) : LocalDate.now();
            LocalDate fechaLecturaParsed = (fechaLectura != null) ? LocalDate.parse(fechaLectura) : fechaParaRegistro;

            for (LecturaUnificadaDTO u : planilla) {
                if (u.isInscritoLuz() && u.getLecturaActLuz() != null && u.getLecturaActLuz() > u.getLecturaAntLuz()) {
                    ReciboDTO rLuz = new ReciboDTO();
                    rLuz.setIdContrato(u.getIdContrato());
                    rLuz.setTipoServicio("LUZ");
                    rLuz.setLecturaAnterior(u.getLecturaAntLuz());
                    rLuz.setLecturaActual(u.getLecturaActLuz());
                    rLuz.setFechaGiro(fechaParaRegistro);
                    rLuz.setFechaLectura(fechaLecturaParsed);
                    reciboClient.registrarLectura(rLuz);
                }

                if (u.isInscritoAgua() && u.getLecturaActAgua() != null && u.getLecturaActAgua() > u.getLecturaAntAgua()) {
                    ReciboDTO rAgua = new ReciboDTO();
                    rAgua.setIdContrato(u.getIdContrato());
                    rAgua.setTipoServicio("AGUA");
                    rAgua.setLecturaAnterior(u.getLecturaAntAgua());
                    rAgua.setLecturaActual(u.getLecturaActAgua());
                    rAgua.setFechaGiro(fechaParaRegistro);
                    rAgua.setFechaLectura(fechaLecturaParsed);
                    reciboClient.registrarLectura(rAgua);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Planilla guardada correctamente");
            response.put("registrosProcesados", planilla.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            String mensajeError;

            // Si es una excepción de Feign con código 400, extraemos el cuerpo (mensaje real)
            if (e instanceof FeignException && ((FeignException) e).status() == 400) {
                mensajeError = ((FeignException) e).contentUTF8();
            } else {
                mensajeError = e.getMessage();
            }

            errorResponse.put("error", mensajeError);
            // También puedes poner "detalle" si lo deseas, pero "error" es suficiente
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @GetMapping("/listar-con-filtros")
    @PreAuthorize("hasAuthority('ROLE_SECRETARIA')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ReciboConClienteDTO>> listarRecibosConFiltros(
            @RequestParam int mes,
            @RequestParam int anio,
            @RequestParam String tipoServicio) {
        
        List<ReciboDTO> recibos = reciboClient.filtrarPorMesYTipo(mes, anio, tipoServicio);
        List<ReciboConClienteDTO> resultado = new ArrayList<>();
        
        for (ReciboDTO r : recibos) {
            ReciboConClienteDTO dto = modelMapper.map(r, ReciboConClienteDTO.class);
            Contrato contrato = contratoRepository.findById(r.getIdContrato()).orElse(null);
            
            if (contrato != null) {
                // Cliente (ya lo tienes)
                if (contrato.getClientes() != null && !contrato.getClientes().isEmpty()) {
                    Cliente cliente = contrato.getClientes().get(0).getCliente();
                    dto.setNombreCliente(cliente.getNombre() + " " + cliente.getApellidos());
                } else {
                    dto.setNombreCliente("Cliente no encontrado");
                }
                
                // Lotes (tomamos el primer lote del contrato, asumiendo que es el principal)
                if (contrato.getLotes() != null && !contrato.getLotes().isEmpty()) {
                    Lote lote = contrato.getLotes().get(0).getLote();
                    dto.setManzana(lote.getManzana());
                    dto.setLote(lote.getNumeroLote());
                    dto.setNombrePrograma(lote.getPrograma().getNombrePrograma()); // si necesitas el programa
                } else {
                    dto.setManzana("N/A");
                    dto.setLote("N/A");
                    dto.setNombrePrograma("N/A");
                }
            } else {
                dto.setNombreCliente("Cliente no encontrado");
                dto.setManzana("N/A");
                dto.setLote("N/A");
                dto.setNombrePrograma("N/A");
            }
            resultado.add(dto);
        }
        
        return ResponseEntity.ok(resultado);
    }
    
    /*========================================================================*/
    @GetMapping("/generar-pdf/{idRecibo}")
    @PreAuthorize("hasAuthority('ROLE_SECRETARIA')")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> generarPdfRecibo(@PathVariable Long idRecibo) {
        try {
            // 1. Obtener el recibo del microservicio
            ReciboDTO reciboDTO;
            try {
                reciboDTO = reciboClient.obtenerPorId(idRecibo);
            } catch (FeignException e) {
                if (e.status() == 404) {
                    return ResponseEntity.notFound().build();
                }
                throw e;
            }
            
            // 2. Crear DTO enriquecido
            ReciboConClienteDTO dto = modelMapper.map(reciboDTO, ReciboConClienteDTO.class);
            
            // 3. Buscar el contrato para obtener datos adicionales
            Contrato contrato = contratoRepository.findById(reciboDTO.getIdContrato()).orElse(null);
            if (contrato != null) {
                // Cliente
                if (contrato.getClientes() != null && !contrato.getClientes().isEmpty()) {
                    Cliente cliente = contrato.getClientes().get(0).getCliente();
                    dto.setNombreCliente(cliente.getNombre() + " " + cliente.getApellidos());
                } else {
                    dto.setNombreCliente("Cliente no encontrado");
                }
                
                // Lote (primer lote)
                if (contrato.getLotes() != null && !contrato.getLotes().isEmpty()) {
                    Lote lote = contrato.getLotes().get(0).getLote();
                    dto.setManzana(lote.getManzana());
                    dto.setLote(lote.getNumeroLote());
                    dto.setNombrePrograma(lote.getPrograma().getNombrePrograma());
                } else {
                    dto.setManzana("N/A");
                    dto.setLote("N/A");
                    dto.setNombrePrograma("N/A");
                }
            }
            
            // 4. Generar PDF
            byte[] pdf = ReciboServiciosPdfGenerator.generarRecibo(dto);

            // 5. Construir nombre de archivo
            String tipo = dto.getTipoServicio(); // "LUZ" o "AGUA"
            String mz = dto.getManzana() != null ? dto.getManzana() : "NA";
            String lt = dto.getLote() != null ? dto.getLote() : "NA";
            String programa = dto.getNombrePrograma() != null ? dto.getNombrePrograma().replaceAll("\\s+", "_") : "NA";

            String filename = String.format("Recibo-%s-%s-%s-%s.pdf", tipo, mz, lt, programa);

            // 6. Devolver como adjunto
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (Exception e) {
            // Manejo de errores generales
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}