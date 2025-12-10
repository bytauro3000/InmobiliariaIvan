package com.Inmobiliaria.demo.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.Inmobiliaria.demo.dto.SeparacionDTO;
import com.Inmobiliaria.demo.dto.SeparacionResumenDTO;
import com.Inmobiliaria.demo.entity.Lote;
import com.Inmobiliaria.demo.entity.Separacion;
import com.Inmobiliaria.demo.service.SeparacionService;

@RestController
@RequestMapping("/api/separaciones") 
@CrossOrigin(origins = "*")
public class SeparacionController {

    @Autowired
    private SeparacionService separacionService;

    @GetMapping("/buscar") 
    public List<SeparacionDTO> buscarSeparaciones(@RequestParam(value = "filtro", required = false) String filtro) {
        // Si el filtro es nulo o vacío, devuelve una lista vacía para evitar búsquedas innecesarias
        if (filtro == null || filtro.trim().isEmpty()) {
            return List.of();
        }
        // Delega la lógica de búsqueda al servicio
        return separacionService.buscarPorDniOApellido(filtro);
    }

    //listar todas las separaciones:
    @GetMapping
    public ResponseEntity<List<Separacion>>listaTodo(){
    	return ResponseEntity.ok(separacionService.listadoSeparacion());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Separacion> obtenerPorId (@PathVariable Integer id) {
        Separacion separa = separacionService.obtenerPorId(id);
        return (separa != null) ? ResponseEntity.ok(separa) : ResponseEntity.notFound().build();
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Separacion> actualizarSeparacion(@PathVariable Integer id, @RequestBody Separacion sepa) {
        // Aseguramos que el ID de la URL coincida con el objeto
        sepa.setIdSeparacion(id);
        
        Separacion actualizada = separacionService.actualizarSeparacion(sepa);
        
        if (actualizada != null) {
            return ResponseEntity.ok(actualizada);
        } else {
            return ResponseEntity.notFound().build(); // Devuelve 404 si el ID no existe
        }
    }
    
    @PostMapping
    public ResponseEntity<Separacion> crearSeparacion(@RequestBody Separacion sepa){
    	Separacion nueva = separacionService.crearSeparacion(sepa);
    	return ResponseEntity.ok(nueva);
    }

    
    @DeleteMapping("/{id}")
    public void eliminarSeparacion(@PathVariable Integer id) {
    	separacionService.eliminarSeparacion(id);
    }
    
    @GetMapping("/resumen")
    public List<SeparacionResumenDTO> obtenerResumen() {
        return separacionService.listarResumen();
    }

    
}
