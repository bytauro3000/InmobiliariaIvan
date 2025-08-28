package com.Inmobiliaria.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Inmobiliaria.demo.entity.Parcelero;
import com.Inmobiliaria.demo.service.ParceleroService;

@RestController
@RequestMapping("/api/parceleros")
@CrossOrigin(origins = "http://localhost:4200") // permite peticiones desde Angular
public class ParceleroController {

    @Autowired
    private ParceleroService parceleroService;

    // LISTAR TODOS
    @GetMapping
    public List<Parcelero> listar() {
        return parceleroService.listarParceleros();
    }

    // OBTENER POR ID
    @GetMapping("/{id}")
    public ResponseEntity<Parcelero> obtenerPorId(@PathVariable Integer id) {
        return parceleroService.obtenerParceleroPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // CREAR NUEVO
    @PostMapping
    public ResponseEntity<Parcelero> crear(@RequestBody Parcelero parcelero) {
        Parcelero nuevo = parceleroService.guardarParcelero(parcelero);
        return ResponseEntity.ok(nuevo);
    }

    // ACTUALIZAR EXISTENTE
    @PutMapping("/{id}")
    public ResponseEntity<Parcelero> actualizar(@PathVariable Integer id, @RequestBody Parcelero parcelero) {
        try {
            Parcelero actualizado = parceleroService.actualizarParcelero(id, parcelero);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ELIMINAR
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        parceleroService.eliminarParcelero(id);
        return ResponseEntity.noContent().build();
    }
}