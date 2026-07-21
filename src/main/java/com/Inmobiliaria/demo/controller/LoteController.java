package com.Inmobiliaria.demo.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Inmobiliaria.demo.dto.DetalleVentaDTO;
import com.Inmobiliaria.demo.dto.LoteProgramaResponseDTO;
import com.Inmobiliaria.demo.dto.LoteRequestDTO;
import com.Inmobiliaria.demo.dto.LoteResponseDTO;
import com.Inmobiliaria.demo.entity.Contrato;
import com.Inmobiliaria.demo.entity.Lote;
import com.Inmobiliaria.demo.service.LoteService;
import com.Inmobiliaria.demo.repository.ContratoRepository;
import com.Inmobiliaria.demo.repository.ProgramaRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;

@RestController
@RequestMapping("/api/lotes")
@RequiredArgsConstructor
public class LoteController {

    private final LoteService loteService;
    private final ProgramaRepository programaRepository;
    private final ContratoRepository contratoRepository;
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<List<LoteResponseDTO>> listarLotes() {
        List<LoteResponseDTO> lotes = loteService.listarLotes().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lotes);
    }

    @GetMapping("/reporte")
    public ResponseEntity<List<LoteResponseDTO>> listarLotesParaReporte() {
        List<LoteResponseDTO> lotes = loteService.listarLotesParaReporte().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lotes);
    }

    @GetMapping("/listarPorPrograma/{idPrograma}")
    public ResponseEntity<List<LoteProgramaResponseDTO>> listarLotesPorPrograma(@PathVariable Integer idPrograma) {
        List<LoteProgramaResponseDTO> lotesDTO = loteService.listarLotesPorPrograma(idPrograma);
        if (lotesDTO.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(lotesDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoteResponseDTO> obtenerLotePorId(@PathVariable Integer id) {
        Lote lote = loteService.obtenerLotePorId(id);
        return (lote != null) ? ResponseEntity.ok(toResponseDTO(lote)) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoteResponseDTO> actualizarLote(@PathVariable Integer id, @Valid @RequestBody LoteRequestDTO dto) {
        Lote lote = toEntity(dto);
        lote.setIdLote(id);
        Lote actualizado = loteService.actualizarLote(lote);
        return (actualizado != null) ? ResponseEntity.ok(toResponseDTO(actualizado)) : ResponseEntity.badRequest().build();
    }

    @PostMapping
    public ResponseEntity<?> crearLote(@Valid @RequestBody LoteRequestDTO dto) {
        try {
            Lote lote = toEntity(dto);
            Lote nuevo = loteService.crearLote(lote);
            return ResponseEntity.ok(toResponseDTO(nuevo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public void eliminarLote(@PathVariable Integer id) {
        loteService.eliminarLote(id);
    }

    @GetMapping("/gestion/programa/{idPrograma}")
    public ResponseEntity<List<LoteResponseDTO>> obtenerLotesParaGestion(@PathVariable Integer idPrograma) {
        List<LoteResponseDTO> lotes = loteService.listarLotesPorProgramaGestion(idPrograma).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lotes);
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<LoteResponseDTO>> buscarLotes(
            @RequestParam Integer idPrograma,
            @RequestParam(required = false, defaultValue = "") String manzana,
            @RequestParam(required = false, defaultValue = "") String numeroLote) {
        List<LoteResponseDTO> resultados = loteService.buscarLotesPorGestion(idPrograma, manzana, numeroLote).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(resultados);
    }

    @GetMapping("/precio-venta/{idLote}")
    public ResponseEntity<java.math.BigDecimal> obtenerPrecioVenta(@PathVariable Integer idLote) {
        java.util.Optional<java.math.BigDecimal> precio = contratoRepository.findPrecioVentaByLoteId(idLote);
        return precio.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/detalle-venta/{idLote}")
    public ResponseEntity<DetalleVentaDTO> obtenerDetalleVenta(@PathVariable Integer idLote) {
        java.util.Optional<Contrato> optContrato = contratoRepository.findContratoByLoteId(idLote);
        if (optContrato.isEmpty()) return ResponseEntity.notFound().build();

        Contrato c = optContrato.get();
        java.util.List<String> clientes = c.getClientes().stream()
            .map(cc -> cc.getCliente().getNombre() + " " + cc.getCliente().getApellidos())
            .collect(java.util.stream.Collectors.toList());

        java.util.List<String> lotes = c.getLotes().stream()
            .map(cl -> "Mz. " + cl.getLote().getManzana() + " - Lt. " + cl.getLote().getNumeroLote()
                + " (" + cl.getLote().getPrograma().getNombrePrograma() + ")")
            .collect(java.util.stream.Collectors.toList());

        DetalleVentaDTO dto = new DetalleVentaDTO(
            c.getMontoTotal(),
            c.getCantidadLetras(),
            clientes,
            lotes
        );
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/validar-duplicado")
    public ResponseEntity<Boolean> verificarDuplicado(
            @RequestParam Integer idPrograma,
            @RequestParam String manzana,
            @RequestParam String numeroLote) {
        boolean existe = loteService.existeLote(idPrograma, manzana, numeroLote);
        return ResponseEntity.ok(existe);
    }

    private LoteResponseDTO toResponseDTO(Lote lote) {
        LoteResponseDTO dto = modelMapper.map(lote, LoteResponseDTO.class);
        if (lote.getPrograma() != null) {
            dto.setNombrePrograma(lote.getPrograma().getNombrePrograma());
            dto.setIdPrograma(lote.getPrograma().getIdPrograma());
        }
        if (lote.getEstado() != null) {
            dto.setEstado(lote.getEstado().name());
        }
        return dto;
    }

    private Lote toEntity(LoteRequestDTO dto) {
        Lote lote = new Lote();
        lote.setManzana(dto.getManzana());
        lote.setNumeroLote(dto.getNumeroLote());
        lote.setArea(dto.getArea());
        lote.setLargo1(dto.getLargo1());
        lote.setLargo2(dto.getLargo2());
        lote.setAncho1(dto.getAncho1());
        lote.setAncho2(dto.getAncho2());
        lote.setPrecioM2(dto.getPrecioM2());
        lote.setColindanteNorte(dto.getColindanteNorte());
        lote.setColindanteSur(dto.getColindanteSur());
        lote.setColindanteEste(dto.getColindanteEste());
        lote.setColindanteOeste(dto.getColindanteOeste());
        if (dto.getEstado() != null) {
            lote.setEstado(dto.getEstado());
        }
        if (dto.getIdPrograma() != null) {
            lote.setPrograma(programaRepository.findById(dto.getIdPrograma()).orElse(null));
        }
        return lote;
    }
}