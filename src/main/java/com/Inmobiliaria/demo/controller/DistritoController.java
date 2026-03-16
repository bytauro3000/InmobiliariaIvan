package com.Inmobiliaria.demo.controller;

import com.Inmobiliaria.demo.entity.Distrito;
import com.Inmobiliaria.demo.service.DistritoService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/distritos")
@RequiredArgsConstructor
public class DistritoController {
	
    private final DistritoService distritoService;

    @GetMapping("/listar")
    public List<Distrito> listarDistritos() {
        return distritoService.listarDistritos();
    }

    @GetMapping("/obtener/{id}")
    public Distrito obtenerDistritoPorId(@PathVariable Integer id) {
        return distritoService.obtenerPorId(id);
    }
}
