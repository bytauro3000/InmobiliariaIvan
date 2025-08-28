package com.Inmobiliaria.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Inmobiliaria.demo.entity.Lote;
import com.Inmobiliaria.demo.service.LoteService;




@RestController
@RequestMapping("/api/lotes")
public class LoteController {

	@Autowired	
    LoteService loteService;


    //Listar todos los lotes (entidad completa)
    @GetMapping
    public ResponseEntity<List<Lote>> listarLotes() {
        return ResponseEntity.ok(loteService.listarLotes());
    }
    
    //Se usa para contrato 
    @GetMapping("/listarPorPrograma/{idPrograma}")
    public ResponseEntity<List<Lote>> listarLotesPorPrograma(@PathVariable Integer idPrograma) {
        List<Lote> lotes = loteService.listarLotesPorPrograma(idPrograma);
        if (lotes.isEmpty()) {
            return ResponseEntity.noContent().build(); 
        }
        return ResponseEntity.ok(lotes);
    }

    //Obtener un lote por su ID
    @GetMapping("/{id}")
    public ResponseEntity<Lote> obtenerLotePorId(@PathVariable Integer id) {
        Lote lote = loteService.obtenerLotePorId(id);
        return (lote != null) ? ResponseEntity.ok(lote) : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}")
    public Lote actualizarLote(@PathVariable Integer id, @RequestBody Lote lote) {
        lote.setIdLote(id);
        return loteService.actualizarLote(lote);
    }

    @PostMapping
    public ResponseEntity<Lote> crearLote(@RequestBody Lote lote) {
        Lote nuevo = loteService.crearLote(lote);
        return ResponseEntity.ok(nuevo);
    }

    @DeleteMapping("/{id}")
    public void EliminarLote(@PathVariable Integer id) {
    	loteService.eliminarLote(id);
    }

}
