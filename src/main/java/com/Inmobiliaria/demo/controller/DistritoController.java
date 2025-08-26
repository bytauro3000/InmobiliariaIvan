package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.entity.Distrito;
import com.Inmobiliaria.demo.service.DistritoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// ✅ La ruta base ahora es "/api/distritos"
@RequestMapping("/api/distritos")
public class DistritoController {

    private final DistritoService distritoService;

    public DistritoController(DistritoService distritoService) {
        this.distritoService = distritoService;
    }

    // 🔹 Listar todos los distritos
    @GetMapping("/listar")
    public List<Distrito> listarDistritos() {
        return distritoService.listarDistritos();
    }

    // 🔹 Obtener distrito por ID
    @GetMapping("/obtener/{id}")
    public Distrito obtenerDistritoPorId(@PathVariable Integer id) {
        return distritoService.obtenerPorId(id);
    }
}