package com.Inmobiliaria.demo.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.Inmobiliaria.demo.dto.SeparacionDTO;
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
}
