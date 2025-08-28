package com.Inmobiliaria.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Inmobiliaria.demo.entity.Vendedor;
import com.Inmobiliaria.demo.service.VendedorService;

@RestController
@RequestMapping("/api/vendedores")
@CrossOrigin(origins = "http://localhost:4200") // permite peticiones desde Angular
public class VendedorController {

    @Autowired
    private VendedorService vendedorService;

    // LISTAR TODOS
    @GetMapping
    public List<Vendedor> listar() {
        return vendedorService.listarVendedores();
    }

    // OBTENER POR ID
    @GetMapping("/{id}")
    public ResponseEntity<Vendedor> obtenerPorId(@PathVariable Integer id) {
        return vendedorService.obtenerVendedorPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // CREAR NUEVO
    @PostMapping
    public ResponseEntity<Vendedor> crear(@RequestBody Vendedor vendedor) {
        Vendedor nuevo = vendedorService.guardarVendedor(vendedor);
        return ResponseEntity.ok(nuevo);
    }

    // ACTUALIZAR EXISTENTE
    @PutMapping("/{id}")
    public ResponseEntity<Vendedor> actualizar(@PathVariable Integer id, @RequestBody Vendedor vendedor) {
        try {
            Vendedor actualizado = vendedorService.actualizarVendedor(id, vendedor);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ELIMINAR
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        vendedorService.eliminarVendedor(id);
        return ResponseEntity.noContent().build();
    }
}
