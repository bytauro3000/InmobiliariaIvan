package com.Inmobiliaria.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Inmobiliaria.demo.entity.Programa;
import com.Inmobiliaria.demo.service.ProgramaService;

@RestController
@RequestMapping("/api/programas")
@CrossOrigin(origins = "*") // Si quieres habilitar desde front
public class ProgramaController {

    @Autowired
    ProgramaService programaService;

    // Listar todos
    @GetMapping
    public ResponseEntity<List<Programa>> listadoProgramas() {
        return ResponseEntity.ok(programaService.listProgramas());
    }

    // Obtener por ID
    @GetMapping("/{id}")
    public ResponseEntity<Programa> obtenerPrograma(@PathVariable Integer id) {
        return programaService.getProgramaById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Crear
    @PostMapping
    public ResponseEntity<Programa> crearPrograma(@RequestBody Programa programa) {
        return ResponseEntity.ok(programaService.savePrograma(programa));
    }

    // Actualizar
    @PutMapping("/{id}")
    public ResponseEntity<Programa> actualizarPrograma(@PathVariable Integer id, @RequestBody Programa programa) {
        return ResponseEntity.ok(programaService.updatePrograma(id, programa));
    }

    // Eliminar
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPrograma(@PathVariable Integer id) {
        programaService.deletePrograma(id);
        return ResponseEntity.noContent().build();
    }
}
