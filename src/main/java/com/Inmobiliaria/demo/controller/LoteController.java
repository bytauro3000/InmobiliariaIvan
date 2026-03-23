package com.Inmobiliaria.demo.controller;

import java.util.List;
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

import com.Inmobiliaria.demo.dto.LoteProgramaResponseDTO;
import com.Inmobiliaria.demo.entity.Lote;
import com.Inmobiliaria.demo.service.LoteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/lotes")
@RequiredArgsConstructor
public class LoteController {
	
    private final LoteService loteService;

    //Listar todos los lotes (entidad completa)
    @GetMapping
    public ResponseEntity<List<Lote>> listarLotes() {
        return ResponseEntity.ok(loteService.listarLotes());
    }

    // Reporte de lotes agrupados por programa — para impresion/PDF
    @GetMapping("/reporte")
    public ResponseEntity<List<Lote>> listarLotesParaReporte() {
        return ResponseEntity.ok(loteService.listarLotesParaReporte());
    }
    
    //USO esta lista para VISTA CONTRATO
    @GetMapping("/listarPorPrograma/{idPrograma}")
    public ResponseEntity<List<LoteProgramaResponseDTO>> listarLotesPorPrograma(@PathVariable Integer idPrograma) {
        List<LoteProgramaResponseDTO> lotesDTO = loteService.listarLotesPorPrograma(idPrograma);
        if (lotesDTO.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(lotesDTO);
    }

    //Obtener un lote por su ID
    @GetMapping("/{id}")
    public ResponseEntity<Lote> obtenerLotePorId(@PathVariable Integer id) {
        Lote lote = loteService.obtenerLotePorId(id);
        return (lote != null) ? ResponseEntity.ok(lote) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Lote> actualizarLote(@PathVariable Integer id, @RequestBody Lote lote) {
        lote.setIdLote(id);
        Lote actualizado = loteService.actualizarLote(lote);
        return (actualizado != null) ? ResponseEntity.ok(actualizado) : ResponseEntity.badRequest().build();
    }

    @PostMapping
    public ResponseEntity<?> crearLote(@RequestBody Lote lote) {
        try {
            Lote nuevo = loteService.crearLote(lote);
            return ResponseEntity.ok(nuevo);
        } catch (RuntimeException e) {
            // 🟢 Retornamos un 400 (Bad Request) con el mensaje de error personalizado
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    public void EliminarLote(@PathVariable Integer id) {
    	loteService.eliminarLote(id);
    }
    
 // URL: GET /api/lotes/gestion/programa/{idPrograma}
    @GetMapping("/gestion/programa/{idPrograma}")
    public ResponseEntity<List<Lote>> obtenerLotesParaGestion(@PathVariable Integer idPrograma) {
        List<Lote> lotes = loteService.listarLotesPorProgramaGestion(idPrograma);
        return ResponseEntity.ok(lotes);
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Lote>> buscarLotes(
            @RequestParam Integer idPrograma,
            @RequestParam(required = false, defaultValue = "") String manzana,
            @RequestParam(required = false, defaultValue = "") String numeroLote) {
            
        List<Lote> resultados = loteService.buscarLotesPorGestion(idPrograma, manzana, numeroLote);
        return ResponseEntity.ok(resultados);
    }
    
    //VALIDAMOS LA EXISTENCIA DE LOTES DUPLICADOS PARA EVITAR SU REGISTRO 
    @GetMapping("/validar-duplicado")
    public ResponseEntity<Boolean> verificarDuplicado(
            @RequestParam Integer idPrograma,
            @RequestParam String manzana,
            @RequestParam String numeroLote) {
        boolean existe = loteService.existeLote(idPrograma, manzana, numeroLote);
        return ResponseEntity.ok(existe);
    }
}