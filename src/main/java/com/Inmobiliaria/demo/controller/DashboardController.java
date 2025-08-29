package com.Inmobiliaria.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Inmobiliaria.demo.service.LoteService;
import com.Inmobiliaria.demo.service.ParceleroService;
import com.Inmobiliaria.demo.service.ProgramaService;
import com.Inmobiliaria.demo.service.VendedorService;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:4200")
public class DashboardController {

    @Autowired
    private VendedorService vendedorService;

    @Autowired
    private ParceleroService parceleroService;

    @Autowired
    private ProgramaService programaService;

    @Autowired
    private LoteService loteService;

    @GetMapping("/totales")
    public Map<String, Long> obtenerTotales() {
        Map<String, Long> totales = new HashMap<>();
        totales.put("vendedores", (long) vendedorService.listarVendedores().size());
        totales.put("parceleros", (long) parceleroService.listarParceleros().size());
        totales.put("programas", (long) programaService.listProgramas().size());
        totales.put("lotes", (long) loteService.listarLotes().size());
        return totales;
    }
}
