package com.Inmobiliaria.demo.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.Inmobiliaria.demo.dto.SeparacionDTO;
import com.Inmobiliaria.demo.dto.SeparacionResumenDTO;
import com.Inmobiliaria.demo.entity.Separacion;
import com.Inmobiliaria.demo.service.SeparacionService;

@RestController
@RequestMapping("/api/separaciones")
@CrossOrigin(origins = "*")
public class SeparacionController {

    @Autowired
    private SeparacionService separacionService;

    // Se agrega produces = MediaType.APPLICATION_JSON_VALUE para forzar JSON
    @GetMapping(value = "/buscar", produces = MediaType.APPLICATION_JSON_VALUE) 
    public ResponseEntity<List<SeparacionDTO>> buscarSeparaciones(@RequestParam(value = "filtro", required = false) String filtro) {
        if (filtro == null || filtro.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(separacionService.buscarPorDniOApellido(filtro));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Separacion>> listaTodo() {
        List<Separacion> lista = separacionService.listadoSeparacion();
        return ResponseEntity.ok(lista);
    }
    
    // Método crítico corregido para devolver JSON
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Separacion> obtenerPorId(@PathVariable Integer id) {
        Separacion separa = separacionService.obtenerPorId(id);
        return (separa != null) ? ResponseEntity.ok(separa) : ResponseEntity.notFound().build();
    }
    
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> crearSeparacion(@RequestBody Separacion sepa) {
        try {
            Separacion nueva = separacionService.crearSeparacion(sepa);
            return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
        } catch (Exception e) {
            // Retorna el error en formato texto plano o podrías envolverlo en un mapa para JSON
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Separacion> actualizarSeparacion(@PathVariable Integer id, @RequestBody Separacion sepa) {
        sepa.setIdSeparacion(id);
        Separacion actualizada = separacionService.actualizarSeparacion(sepa);
        return (actualizada != null) ? ResponseEntity.ok(actualizada) : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarSeparacion(@PathVariable Integer id) {
        try {
            separacionService.eliminarSeparacion(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping(value = "/resumen", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SeparacionResumenDTO>> obtenerResumen() {
        return ResponseEntity.ok(separacionService.listarResumen());
    }
}