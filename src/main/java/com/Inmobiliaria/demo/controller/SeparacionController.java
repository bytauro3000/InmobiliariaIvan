package com.Inmobiliaria.demo.controller;

import java.util.List;
import java.util.Map;
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

    // Búsqueda para autocompletado: el Repositorio ahora usa DISTINCT para evitar duplicados
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
    
    /**
     * MÉTODO CRÍTICO: Obtener detalle por ID.
     * Gracias al EntityGraph en el Repositorio, ahora carga los distritos de Clientes y Vendedores,
     * evitando el error 403 / HttpMessageNotWritableException (no session).
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> obtenerPorId(@PathVariable Integer id) {
        try {
            Separacion separa = separacionService.obtenerPorId(id);
            if (separa == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                     .body(Map.of("mensaje", "Separación con ID " + id + " no encontrada"));
            }
            return ResponseEntity.ok(separa);
        } catch (Exception e) {
            // Si el error de serialización persiste, esto lo capturará en lugar de lanzar un 403 genérico
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "Error al procesar los datos: " + e.getMessage()));
        }
    }
    
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> crearSeparacion(@RequestBody Separacion sepa) {
        try {
            Separacion nueva = separacionService.crearSeparacion(sepa);
            return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> actualizarSeparacion(@PathVariable Integer id, @RequestBody Separacion sepa) {
        try {
            sepa.setIdSeparacion(id);
            Separacion actualizada = separacionService.actualizarSeparacion(sepa);
            if (actualizada == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(actualizada);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarSeparacion(@PathVariable Integer id) {
        try {
            separacionService.eliminarSeparacion(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                 .body(Map.of("error", "No se pudo eliminar: " + e.getMessage()));
        }
    }
    
    @GetMapping(value = "/resumen", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SeparacionResumenDTO>> obtenerResumen() {
        return ResponseEntity.ok(separacionService.listarResumen());
    }
}